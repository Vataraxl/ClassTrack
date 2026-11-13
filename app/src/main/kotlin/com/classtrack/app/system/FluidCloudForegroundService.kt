package com.classtrack.app.system

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 实况胶囊后台保活前台服务 (FluidCloudForegroundService)
 *
 * 仅在用户选择 `LIVE_CAPSULE` 状态栏实况胶囊样式且处于活跃窗口时按需启动；
 * 每一帧均通过 `DataRepository.resolveActiveOrUpcomingTrack(LocalDateTime.now())` 实时计算，
 * 当课程下课或被「已到教室」收起时立刻 `stopForeground` 释放资源，绝不发送冻结的旧状态。
 */
class FluidCloudForegroundService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var carouselJob: Job? = null

    companion object {
        const val ACTION_START_CAROUSEL = "com.classtrack.app.ACTION_START_CAROUSEL"
        const val ACTION_STOP_CAROUSEL = "com.classtrack.app.ACTION_STOP_CAROUSEL"

        fun startCarousel(context: Context) {
            val intent = Intent(context, FluidCloudForegroundService::class.java).apply {
                action = ACTION_START_CAROUSEL
            }
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stopCarousel(context: Context) {
            val intent = Intent(context, FluidCloudForegroundService::class.java).apply {
                action = ACTION_STOP_CAROUSEL
            }
            runCatching {
                context.startService(intent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_CAROUSEL -> {
                carouselJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForegroundCarouselLoop()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundCarouselLoop() {
        carouselJob?.cancel()
        val repository = DataRepository(applicationContext)

        carouselJob = serviceScope.launch {
            val now = LocalDateTime.now()
            val initialTrack = resolveLiveOrFallbackTrack(repository, now)
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
                cardStyle = NotificationCardStyle.LIVE_CAPSULE,
                sessionKey = initialSessionKey
            )

            val started = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceCompat.startForeground(
                        this@FluidCloudForegroundService,
                        ColorOsFluidCloudHelper.NOTIFICATION_ID_FLUID_TRACK,
                        initialNotif,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(ColorOsFluidCloudHelper.NOTIFICATION_ID_FLUID_TRACK, initialNotif)
                }
                true
            }.getOrDefault(false)

            if (!started) {
                stopSelf()
                return@launch
            }

            // 如果首帧只是兜底预览且当前无真实活跃课程，直接 detach 并退出，不滞留空服务
            val realActive = repository.resolveActiveOrUpcomingTrack(now = now, respectDismissed = true)
            if (realActive == null || !repository.fluidCloudEnabled.value) {
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
                return@launch
            }

            delay(3000L)
            var step = 1

            while (isActive && step < 480) {
                if (!repository.fluidCloudEnabled.value ||
                    repository.notificationCardStyle.value != NotificationCardStyle.LIVE_CAPSULE
                ) {
                    break
                }
                val tickNow = LocalDateTime.now()
                val currentTrack = repository.resolveActiveOrUpcomingTrack(
                    now = tickNow,
                    leadMinutes = repository.leadMinutes.value,
                    respectDismissed = true
                ) ?: break

                if (currentTrack.playState == TrackPlayState.COMPLETED) {
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
                    cardStyle = NotificationCardStyle.LIVE_CAPSULE,
                    sessionKey = sessionKey,
                    isSilentUpdate = true,
                    startLiveUpdates = false
                )
                step++
                delay(4500L)
            }

            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun resolveLiveOrFallbackTrack(
        repository: DataRepository,
        now: LocalDateTime
    ): TodayTrackUiModel {
        return repository.resolveActiveOrUpcomingTrack(
            now = now,
            leadMinutes = repository.leadMinutes.value,
            allowAnyUpcomingToday = false,
            respectDismissed = true
        ) ?: TodayTrackUiModel(
            trackNumber = 1,
            course = CourseItem(
                name = "无机及分析化学",
                room = "二教208",
                teacher = "任课教师",
                dayOfWeek = LocalDate.now().dayOfWeek.value,
                startSection = 10,
                endSection = 11
            ),
            startTime = "18:30",
            endTime = "20:05",
            playState = TrackPlayState.UPCOMING,
            progressFraction = 0f,
            minutesUntilStartOrEnd = 6
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
