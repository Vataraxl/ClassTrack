package com.classtrack.app.system

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.classtrack.app.data.DataRepository
import com.classtrack.app.data.model.CourseItem
import com.classtrack.app.data.model.NotificationCardStyle
import com.classtrack.app.data.model.TodayTrackUiModel
import com.classtrack.app.data.model.TrackPlayState
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 实况课程提醒后台保活前台服务 (FluidCloudForegroundService)
 *
 * 1. 同时承载「极简点阵流体卡 (DOT_CARD)」与「状态栏实况胶囊 (LIVE_CAPSULE)」在课前/课中活跃窗口的进程保活；
 * 2. 监听系统整分广播 (ACTION_TIME_TICK) 与亮屏解锁 (ACTION_SCREEN_ON / ACTION_USER_PRESENT)，
 *    亮屏或整分瞬间立即用当前真实时间重算通知，避免息屏后剩余分钟卡顿；
 * 3. 无人为步数自毁上限，直至下课或用户点击「已到教室」收起时立刻退居后台释放资源。
 */
class FluidCloudForegroundService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + serviceJob)
    private var sessionJob: Job? = null
    private val refreshSignal = Channel<Unit>(Channel.CONFLATED)
    private var receiverRegistered = false

    @Volatile
    private var isScreenInteractive = true

    private val systemTickAndScreenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenInteractive = false
                }
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_USER_PRESENT -> {
                    isScreenInteractive = true
                    refreshSignal.trySend(Unit)
                }
                Intent.ACTION_TIME_TICK -> {
                    refreshSignal.trySend(Unit)
                }
            }
        }
    }

    companion object {
        private const val TAG = "ClassTrackAlarm"
        const val ACTION_START_CAROUSEL = "com.classtrack.app.ACTION_START_CAROUSEL"
        const val ACTION_STOP_CAROUSEL = "com.classtrack.app.ACTION_STOP_CAROUSEL"
        private const val MAX_SESSION_DURATION_MS = 8L * 3600_000L

        @Volatile
        var isRunning: Boolean = false
            private set

        fun startCarousel(context: Context): Boolean {
            val appContext = context.applicationContext
            val intent = Intent(appContext, FluidCloudForegroundService::class.java).apply {
                action = ACTION_START_CAROUSEL
            }
            return runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appContext.startForegroundService(intent)
                } else {
                    appContext.startService(intent)
                }
                true
            }.getOrElse { error ->
                Log.e(TAG, "启动前台提醒服务失败: ${error.javaClass.simpleName}: ${error.message}", error)
                false
            }
        }

        fun stopCarousel(context: Context) {
            val appContext = context.applicationContext
            runCatching {
                appContext.stopService(Intent(appContext, FluidCloudForegroundService::class.java))
            }.onFailure { error ->
                Log.w(TAG, "停止前台提醒服务异常: ${error.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        isScreenInteractive = powerManager?.isInteractive ?: true

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        runCatching {
            ContextCompat.registerReceiver(
                this,
                systemTickAndScreenReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            receiverRegistered = true
        }.onFailure { error ->
            Log.w(TAG, "注册系统时钟/亮屏广播失败: ${error.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_CAROUSEL) {
            stopAndExit(removeNotification = true)
            return START_NOT_STICKY
        }

        val repository = DataRepository.getInstance(applicationContext)
        val now = LocalDateTime.now()
        val realActive = repository.resolveActiveOrUpcomingTrack(
            now = now,
            leadMinutes = repository.leadMinutes.value,
            respectDismissed = true
        )
        val initialTrack = realActive ?: buildFallbackTrack(now)
        val initialStyle = repository.notificationCardStyle.value
        val initialSessionKey = repository.buildSessionKey(
            date = now.toLocalDate(),
            courseId = initialTrack.course.id,
            startSection = initialTrack.course.startSection
        )

        val initialNotif = ColorOsFluidCloudHelper.createLiveTrackNotification(
            context = applicationContext,
            track = initialTrack,
            artistTheme = repository.artistTheme.value,
            watchBridgeEnabled = repository.watchBridgeEnabled.value,
            carouselFrameIndex = 0,
            isCarouselTick = true,
            cardStyle = initialStyle,
            sessionKey = initialSessionKey
        )

        val startedForeground = runCatching {
            val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            ServiceCompat.startForeground(
                this,
                ColorOsFluidCloudHelper.NOTIFICATION_ID_FLUID_TRACK,
                initialNotif,
                fgType
            )
            true
        }.getOrElse { error ->
            Log.e(TAG, "startForeground 挂载失败: ${error.javaClass.simpleName}: ${error.message}", error)
            false
        }

        if (!startedForeground) {
            isRunning = false
            stopSelf()
            return START_NOT_STICKY
        }

        if (!repository.fluidCloudEnabled.value || realActive == null || realActive.playState == TrackPlayState.COMPLETED) {
            stopAndExit(removeNotification = true)
            return START_NOT_STICKY
        }

        startOrRefreshSessionLoop()
        return START_STICKY
    }

    private fun startOrRefreshSessionLoop() {
        if (sessionJob?.isActive == true) {
            refreshSignal.trySend(Unit)
            return
        }

        val repository = DataRepository.getInstance(applicationContext)
        val sessionStartMs = System.currentTimeMillis()

        sessionJob = serviceScope.launch {
            var step = 1
            Log.i(TAG, "实况课程保活循环启动")

            while (isActive && System.currentTimeMillis() - sessionStartMs < MAX_SESSION_DURATION_MS) {
                val style = repository.notificationCardStyle.value
                val waitMs = if (style == NotificationCardStyle.LIVE_CAPSULE && isScreenInteractive) {
                    4_500L
                } else {
                    (CourseAlarmScheduler.computeNextMinuteTickMillis() - System.currentTimeMillis())
                        .coerceIn(500L, 61_000L)
                }

                withTimeoutOrNull(waitMs) {
                    refreshSignal.receive()
                }

                if (!repository.fluidCloudEnabled.value) {
                    ColorOsFluidCloudHelper.cancelFluidCloudNotification(applicationContext)
                    break
                }

                val tickNow = LocalDateTime.now()
                val currentTrack = repository.resolveActiveOrUpcomingTrack(
                    now = tickNow,
                    leadMinutes = repository.leadMinutes.value,
                    respectDismissed = true
                )
                if (currentTrack == null || currentTrack.playState == TrackPlayState.COMPLETED) {
                    ColorOsFluidCloudHelper.cancelFluidCloudNotification(applicationContext)
                    break
                }

                val sessionKey = repository.buildSessionKey(
                    date = tickNow.toLocalDate(),
                    courseId = currentTrack.course.id,
                    startSection = currentTrack.course.startSection
                )
                ColorOsFluidCloudHelper.postFluidCloudTrackNotification(
                    context = applicationContext,
                    track = currentTrack,
                    artistTheme = repository.artistTheme.value,
                    watchBridgeEnabled = repository.watchBridgeEnabled.value,
                    carouselFrameIndex = step,
                    isCarouselTick = true,
                    cardStyle = repository.notificationCardStyle.value,
                    sessionKey = sessionKey,
                    isSilentUpdate = true,
                    startLiveUpdates = false
                )
                step++
            }

            stopAndExit(removeNotification = false)
        }
    }

    private fun stopAndExit(removeNotification: Boolean) {
        sessionJob?.cancel()
        sessionJob = null
        isRunning = false
        val flags = if (removeNotification) {
            ServiceCompat.STOP_FOREGROUND_REMOVE
        } else {
            ServiceCompat.STOP_FOREGROUND_DETACH
        }
        runCatching { ServiceCompat.stopForeground(this, flags) }
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val appContext = applicationContext
        val repository = DataRepository.getInstance(appContext)
        CourseAlarmScheduler.rescheduleAll(appContext)
        if (repository.fluidCloudEnabled.value) {
            val activeTrack = repository.resolveActiveOrUpcomingTrack(
                now = LocalDateTime.now(),
                leadMinutes = repository.leadMinutes.value,
                respectDismissed = true
            )
            if (activeTrack != null && activeTrack.playState != TrackPlayState.COMPLETED) {
                Log.i(TAG, "检测到划卡清理后台，2 秒后自动恢复活跃课程通知")
                CourseAlarmScheduler.scheduleImmediateRecoveryAlarm(appContext, delayMs = 2_000L)
            }
        }
    }

    private fun buildFallbackTrack(now: LocalDateTime): TodayTrackUiModel {
        return TodayTrackUiModel(
            trackNumber = 1,
            course = CourseItem(
                name = "当前课程",
                room = "教室",
                teacher = "",
                dayOfWeek = now.dayOfWeek.value,
                startSection = 1,
                endSection = 2
            ),
            startTime = "08:00",
            endTime = "09:35",
            playState = TrackPlayState.UPCOMING,
            progressFraction = 0f,
            minutesUntilStartOrEnd = 1
        )
    }

    override fun onDestroy() {
        isRunning = false
        if (receiverRegistered) {
            runCatching { unregisterReceiver(systemTickAndScreenReceiver) }
            receiverRegistered = false
        }
        serviceJob.cancel()
        super.onDestroy()
    }
}
