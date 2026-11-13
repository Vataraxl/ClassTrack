package com.classtrack.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.classtrack.app.system.ColorOsFluidCloudHelper
import com.classtrack.app.system.CourseAlarmScheduler
import com.classtrack.app.system.CourseAlarmWatchdogJobService

class MainActivity : ComponentActivity() {

    private var lastBackgroundDispatchMs: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyNativeHighRefreshRate()
        ColorOsFluidCloudHelper.ensureNotificationChannel(this)
        requestNotificationPermissionIfNeeded()
        CourseAlarmWatchdogJobService.ensureScheduled(this)
        CourseAlarmScheduler.rescheduleAll(this)
        enableEdgeToEdge()
        setContent {
            MainNavigation()
        }
    }

    override fun onResume() {
        super.onResume()
        applyNativeHighRefreshRate()
        CourseAlarmWatchdogJobService.ensureScheduled(this)
        CourseAlarmScheduler.rescheduleAll(this)
        // 若当前恰好处于上课或课前提醒窗口且未主动收起，打开/返回应用时同步刷新实时分钟与点阵
        ColorOsFluidCloudHelper.postOrUpdateActiveLiveReminder(
            context = this,
            isSilentUpdate = true
        )
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        dispatchBackgroundLiveSync()
    }

    override fun onStop() {
        super.onStop()
        dispatchBackgroundLiveSync()
    }

    private fun dispatchBackgroundLiveSync() {
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastBackgroundDispatchMs < 1500L) return
        lastBackgroundDispatchMs = nowMs
        ColorOsFluidCloudHelper.triggerAutoBackgroundFluidCloud(this)
        CourseAlarmScheduler.rescheduleAll(this)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    /**
     * 原生硬件高刷新率适配 (90Hz / 120Hz / 144Hz+)：
     * 1. 遍历屏幕支持的所有 Display.Mode，选取最高物理刷新率并锁定为 preferredDisplayModeId 与 preferredRefreshRate；
     * 2. 避免系统在加载复杂网格、嵌套视图或内嵌 WebView 时将帧率误降为 60Hz。
     */
    private fun applyNativeHighRefreshRate() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val currentDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    display
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay
                }

                currentDisplay?.supportedModes?.let { modes ->
                    val highestRefreshMode = modes.maxByOrNull { it.refreshRate }
                    if (highestRefreshMode != null && highestRefreshMode.refreshRate >= 80f) {
                        val lp = window.attributes
                        var changed = false
                        if (lp.preferredDisplayModeId != highestRefreshMode.modeId) {
                            lp.preferredDisplayModeId = highestRefreshMode.modeId
                            changed = true
                        }
                        if (lp.preferredRefreshRate != highestRefreshMode.refreshRate) {
                            lp.preferredRefreshRate = highestRefreshMode.refreshRate
                            changed = true
                        }
                        if (changed) {
                            window.attributes = lp
                        }
                    }
                }
            }
        }
    }
}
