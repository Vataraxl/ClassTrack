package com.classtrack.app

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.classtrack.app.system.ColorOsFluidCloudHelper
import com.classtrack.app.system.CourseAlarmScheduler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyNativeHighRefreshRate()
        ColorOsFluidCloudHelper.ensureNotificationChannel(this)
        requestNotificationPermissionIfNeeded()
        // 全自动课前提醒闹钟排程：无论用户是否打开应用，提前精准唤醒
        CourseAlarmScheduler.rescheduleAll(this)
        enableEdgeToEdge()
        setContent {
            MainNavigation()
        }
    }

    override fun onResume() {
        super.onResume()
        applyNativeHighRefreshRate()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // 用户按 Home 键或上滑退出至桌面时，立即自动唤起流体云与状态栏实况胶囊
        ColorOsFluidCloudHelper.triggerAutoBackgroundFluidCloud(this)
        CourseAlarmScheduler.rescheduleAll(this)
    }

    override fun onStop() {
        super.onStop()
        // 用户退出应用或退至桌面时，若开启了流体云则自动触发当前课程的固定宽度横向跑马灯实况流体云
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
                    // 挑选刷新率最高的 Display.Mode（如 90Hz, 120Hz, 144Hz 等）
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
