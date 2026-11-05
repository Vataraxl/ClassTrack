package com.classtrack.app.system

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.classtrack.app.data.DataRepository
import com.classtrack.app.data.model.TodayTrackUiModel
import com.classtrack.app.data.model.TrackPlayState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 流体云实况跑马灯后台保活前台服务 (FluidCloudForegroundService)
 *
 * 核心作用：
 * 1. 彻底解决 ColorOS 16/15、OriginOS、HyperOS 等系统在用户退出应用回到桌面后，因“快速进程冻结（Quick Freeze）”
 *    导致主线程 Handler 被系统暂停、状态栏胶囊停止滚动轮播的顽疾；
 * 2. 以官方规范的 Foreground Service 持有实况流体云常驻通知，向系统声明前台任务状态，杜绝被杀或休眠；
 * 3. 内部使用轻量级协程驱动，每 2.0 秒平滑递增轮播步进，在手机状态栏实现稳定从容的【时间 ➔ 课程 ➔ 地点】轮流展示；
 * 4. 任务结束后（如用户点击已到教室打卡、下课或收起）自动 stopSelf() 释放全部系统资源，绿色轻量。
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
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
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
            ACTION_START_CAROUSEL -> {
                startForegroundCarouselLoop()
            }
            else -> {
                startForegroundCarouselLoop()
            }
        }
        return START_STICKY
    }

    private fun startForegroundCarouselLoop() {
        carouselJob?.cancel()
        val repository = DataRepository(applicationContext)

        carouselJob = serviceScope.launch {
            var step = 0

            // 首次先执行一次首帧推送并锁定 startForeground
            val initialTrack = resolveCurrentTrack(repository)
            val initialNotif = ColorOsFluidCloudHelper.createLiveTrackNotification(
                context = applicationContext,
                track = initialTrack,
                artistTheme = repository.artistTheme.value,
                watchBridgeEnabled = repository.watchBridgeEnabled.value,
                carouselFrameIndex = 0,
                isCarouselTick = false
            )

            runCatching {
                startForeground(ColorOsFluidCloudHelper.NOTIFICATION_ID_FLUID_TRACK, initialNotif)
            }

            // 1.5 秒后（顶端流体云大弹窗收回状态栏通知岛后）立即启动从容轮播
            delay(1500L)
            step = 1

            // 轮播循环：每 2.0 秒平稳推进 1 帧（时间 ➔ 课程 ➔ 地点 ➔ 时间）
            while (isActive && step < 600) {
                val currentTrack = resolveCurrentTrack(repository)
                ColorOsFluidCloudHelper.postFluidCloudTrackNotification(
                    context = applicationContext,
                    track = currentTrack,
                    artistTheme = repository.artistTheme.value,
                    watchBridgeEnabled = repository.watchBridgeEnabled.value,
                    carouselFrameIndex = step,
                    isCarouselTick = true
                )
                step++
                delay(3000L) // 3.0 秒平稳停顿，大字完整展示时间+课名或地点，杜绝系统限流与视觉眩晕
            }

            // 达到最大轮播时长后安全退出
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun resolveCurrentTrack(repository: DataRepository): TodayTrackUiModel {
        val tracks = repository.buildTracksForDay(
            dayOfWeek = repository.selectedDayOfWeek.value,
            week = repository.currentWeek.value
        )
        return tracks.firstOrNull { it.playState == TrackPlayState.NOW_PLAYING }
            ?: tracks.firstOrNull { it.playState == TrackPlayState.UPCOMING }
            ?: tracks.firstOrNull()
            ?: TodayTrackUiModel(
                trackNumber = 1,
                course = com.classtrack.app.data.model.CourseItem(
                    name = "高等数学",
                    room = "二教 209",
                    teacher = "任课教师",
                    dayOfWeek = repository.selectedDayOfWeek.value,
                    startSection = 1,
                    endSection = 2
                ),
                startTime = "08:00",
                endTime = "09:35",
                playState = TrackPlayState.UPCOMING,
                progressFraction = 0.35f,
                minutesUntilStartOrEnd = 15
            )
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
