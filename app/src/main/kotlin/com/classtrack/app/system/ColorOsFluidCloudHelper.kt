package com.classtrack.app.system

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.UserManager
import android.provider.Settings
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.classtrack.app.MainActivity
import com.classtrack.app.R
import com.classtrack.app.data.DataRepository
import com.classtrack.app.data.model.ArtistTheme
import com.classtrack.app.data.model.CourseItem
import com.classtrack.app.data.model.NotificationCardStyle
import com.classtrack.app.data.model.TodayTrackUiModel
import com.classtrack.app.data.model.TrackPlayState
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * 四大国产系统实况胶囊 / 流体云协议枚举
 */
enum class LiveCapsuleOsFamily(
    val displayName: String,
    val brandDesc: String,
    val featureName: String
) {
    AUTO("自动检测", "自动匹配当前手机系统", "系统实况胶囊"),
    COLOROS("ColorOS", "OPPO / 一加 / 真我", "流体云 (Fluid Cloud)"),
    ORIGINOS("OriginOS", "vivo / iQOO", "原子岛 (Atomic Island)"),
    HYPEROS("HyperOS", "小米 / Redmi", "澎湃超级岛 · 焦点通知"),
    MAGICOS("MagicOS", "荣耀 (Honor)", "灵动胶囊 · 实况窗")
}

/**
 * 通知岛收起后的滚动阶段枚举
 */
enum class IslandScrollStage(val label: String) {
    POPUP_EXPANDED("弹窗展开态"),
    COURSE_NAME("时间与课名"),
    ROOM_LOCATION("教室地点"),
    CLASS_TIME("状态倒计时")
}

/**
 * 通知岛实时预览与状态数据（供设置页同步展示流体卡与胶囊效果）
 */
data class IslandLivePreviewState(
    val isActive: Boolean = false,
    val stage: IslandScrollStage = IslandScrollStage.POPUP_EXPANDED,
    val islandCapsuleText: String = "",
    val fullMarqueeText: String = "",
    val scrollOffset: Int = 0,
    val capsuleLeftLabel: String = "",
    val capsuleRightValue: String = "",
    val fullCourseName: String = "",
    val roomName: String = "",
    val timeRange: String = "",
    val progressPercent: Int = 0
)

/**
 * 《课轨》流体云、极简点阵流体卡与穿戴设备统一调度引擎
 *
 * 1. 支持双样式自由切换：
 *    - `DOT_CARD`（默认）：复刻高精度 28 点横向渐变时间轴、顶行时间+教室粗体聚焦、右侧黑胶角标、底部珊瑚红状态与紧凑「已到教室」胶囊按钮；
 *    - `LIVE_CAPSULE`：Android 16 原生 ProgressStyle + ColorOS/OriginOS/HyperOS/MagicOS 状态栏实况胶囊（移除笨重左大方块与纸飞机光标）。
 * 2. 零冻结实时真值渲染：所有唤醒、心跳、前台服务每帧均通过 `DataRepository.resolveActiveOrUpcomingTrack(LocalDateTime.now())` 实时重算，绝不重复推送冻结的旧分钟数。
 * 3. 与分钟时钟严格对齐：活跃窗口内由硬件级 RTC `:00.150` 秒心跳逐分推进状态与点阵进度，到点自动从「X分钟后上课」切换为「Y分钟后下课」。
 */
object ColorOsFluidCloudHelper {

    const val CHANNEL_ID_FLUID_CLOUD = "classtrack_fluid_cloud_live_v3"
    const val CHANNEL_ID_WATCH_BRIDGE = "classtrack_watch_bridge_v1"
    private const val LEGACY_CHANNEL_MEDIA = "classtrack_fluid_media_capsule_v2"
    const val NOTIFICATION_ID_FLUID_TRACK = 202601
    const val NOTIFICATION_ID_WATCH_BRIDGE = 202603
    private const val LEGACY_NOTIFICATION_ID_MEDIA = 202602

    const val ACTION_DISMISS_FLUID_CLOUD = "com.classtrack.app.ACTION_DISMISS_FLUID_CLOUD"
    const val ACTION_TRIGGER_COURSE_REMINDER = "com.classtrack.app.ACTION_TRIGGER_COURSE_REMINDER"
    const val ACTION_LIVE_TICK_FLUID_CLOUD = "com.classtrack.app.ACTION_LIVE_TICK_FLUID_CLOUD"
    const val ACTION_CHECK_ACTIVE_REMINDER = "com.classtrack.app.ACTION_CHECK_ACTIVE_REMINDER"
    const val ACTION_TEST_WATCH_BRIDGE = "com.classtrack.app.ACTION_TEST_WATCH_BRIDGE"

    private const val DOT_COUNT = 28
    private const val WAKE_LOCK_TAG = "ClassTrack:CourseRtcWakeLock"

    private val islandCarouselHandler = Handler(Looper.getMainLooper())
    private var islandCarouselRunnable: Runnable? = null
    private var sessionFixedWhenMs: Long = 0L
    private var cachedVinylKey: String = ""
    private var cachedVinylBitmap: Bitmap? = null
    private var cachedTrackerDotKey: String = ""
    private var cachedTrackerDotBitmap: Bitmap? = null

    private val _islandPreviewState = MutableStateFlow(IslandLivePreviewState())
    val islandPreviewState: StateFlow<IslandLivePreviewState> = _islandPreviewState.asStateFlow()

    data class IslandCarouselFrame(
        val stage: IslandScrollStage,
        val shortCriticalText: String,
        val capsuleLeftTitle: String,
        val capsuleRightContent: String,
        val subTextBanner: String,
        val marqueeFullText: String
    )

    data class FluidCloudDiagnostics(
        val detectedOsFamily: LiveCapsuleOsFamily,
        val canPostPromoted: Boolean,
        val hasPromotableCharacteristics: Boolean,
        val cardStyle: NotificationCardStyle,
        val statusSummary: String
    )

    private data class CardPalette(
        val accentArgb: Int,
        val headlineArgb: Int,
        val mutedTextArgb: Int,
        val statusAccentArgb: Int,
        val unlitDotArgb: Int,
        val pillBgResId: Int,
        val pillTextArgb: Int,
        val isDarkSurface: Boolean
    )

    fun detectDeviceOsFamily(): LiveCapsuleOsFamily {
        val brand = "${Build.MANUFACTURER} ${Build.BRAND} ${Build.DISPLAY}".lowercase(Locale.ROOT)
        return when {
            brand.contains("oppo") || brand.contains("oneplus") || brand.contains("realme") ||
                getSystemProperty("ro.build.version.opporom").isNotBlank() ||
                getSystemProperty("ro.oplus.version.my_heytap").isNotBlank() -> LiveCapsuleOsFamily.COLOROS

            brand.contains("vivo") || brand.contains("iqoo") ||
                getSystemProperty("ro.vivo.os.version").isNotBlank() -> LiveCapsuleOsFamily.ORIGINOS

            brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") ||
                getSystemProperty("ro.mi.os.version.name").isNotBlank() ||
                getSystemProperty("ro.miui.ui.version.name").isNotBlank() -> LiveCapsuleOsFamily.HYPEROS

            brand.contains("honor") || brand.contains("huawei") ||
                getSystemProperty("ro.build.version.magic").isNotBlank() ||
                getSystemProperty("msc.config.magic.version").isNotBlank() -> LiveCapsuleOsFamily.MAGICOS

            else -> LiveCapsuleOsFamily.COLOROS
        }
    }

    private fun getSystemProperty(key: String): String = runCatching {
        val cls = Class.forName("android.os.SystemProperties")
        val getMethod = cls.getMethod("get", String::class.java, String::class.java)
        (getMethod.invoke(null, key, "") as? String).orEmpty()
    }.getOrDefault("")

    fun ensureNotificationChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        runCatching {
            manager.deleteNotificationChannel(LEGACY_CHANNEL_MEDIA)
            manager.cancel(LEGACY_NOTIFICATION_ID_MEDIA)
        }

        val liveChannel = NotificationChannel(
            CHANNEL_ID_FLUID_CLOUD,
            "课轨 · 流体云与实况课程进度",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "用于实时课程流体卡、ColorOS 流体云、OriginOS 原子岛、HyperOS 超级岛与 MagicOS 实况窗"
            enableVibration(true)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(liveChannel)

        val watchChannel = NotificationChannel(
            CHANNEL_ID_WATCH_BRIDGE,
            "课轨 · 智能手表上课提醒 (OPPO Watch / 穿戴设备)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "专用于向 OPPO Watch X2、ColorOS Watch、Wear OS 及智能手环同步即时上课提醒"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 150, 250)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(watchChannel)
    }

    fun checkPromotedPermission(context: Context): Boolean {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!manager.areNotificationsEnabled()) return false

        val detected = detectDeviceOsFamily()
        if (detected == LiveCapsuleOsFamily.HYPEROS) {
            val hyperFocusOk = runCatching {
                val uri = Uri.parse("content://miui.statusbar.notification.public")
                val extras = Bundle().apply { putString("package", context.packageName) }
                val res = context.contentResolver.call(uri, "canShowFocus", null, extras)
                res?.getBoolean("canShowFocus", true) ?: true
            }.getOrDefault(true)
            if (!hyperFocusOk) return false
        }

        return if (Build.VERSION.SDK_INT >= 36) {
            runCatching { manager.canPostPromotedNotifications() }.getOrDefault(true)
        } else {
            true
        }
    }

    fun openSystemFluidCloudSettings(context: Context) {
        val launchedPromotionPage = runCatching {
            val promoIntent = Intent("android.settings.APP_NOTIFICATION_PROMOTION_SETTINGS").apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(promoIntent)
            true
        }.getOrDefault(false)

        if (!launchedPromotionPage) {
            runCatching {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }

    /**
     * 依据当前实时系统时间解析「正在上课 / 即将上课」并发送或静默更新流体卡 / 实况胶囊，
     * 同步挂接下一分钟 `:00.150` 秒硬件级 RTC 实时心跳。
     */
    fun postOrUpdateActiveLiveReminder(
        context: Context,
        isSilentUpdate: Boolean = true,
        targetCourseId: Long = -1L,
        allowAnyUpcomingToday: Boolean = false,
        respectDismissed: Boolean = true,
        referenceNow: LocalDateTime = LocalDateTime.now()
    ): FluidCloudDiagnostics? {
        val appContext = context.applicationContext
        val repository = DataRepository(appContext)
        if (!repository.fluidCloudEnabled.value) {
            cancelFluidCloudNotification(appContext)
            CourseAlarmScheduler.cancelLiveTick(appContext)
            return null
        }

        val activeTrack = repository.resolveActiveOrUpcomingTrack(
            now = referenceNow,
            leadMinutes = repository.leadMinutes.value,
            targetCourseId = targetCourseId,
            allowAnyUpcomingToday = allowAnyUpcomingToday,
            respectDismissed = respectDismissed
        )

        if (activeTrack == null || activeTrack.playState == TrackPlayState.COMPLETED) {
            cancelFluidCloudNotification(appContext)
            CourseAlarmScheduler.cancelLiveTick(appContext)
            return null
        }

        val sessionKey = repository.buildSessionKey(
            date = referenceNow.toLocalDate(),
            courseId = activeTrack.course.id,
            startSection = activeTrack.course.startSection
        )

        val diagnostics = postFluidCloudTrackNotification(
            context = appContext,
            track = activeTrack,
            artistTheme = repository.artistTheme.value,
            watchBridgeEnabled = repository.watchBridgeEnabled.value,
            cardStyle = repository.notificationCardStyle.value,
            sessionKey = sessionKey,
            isSilentUpdate = isSilentUpdate,
            startLiveUpdates = true
        )

        // 仅在首次非静默唤醒（课前触发或跨状态主动提醒）时向 OPPO Watch X2 发送腕上脉冲提醒
        if (!isSilentUpdate && repository.watchBridgeEnabled.value) {
            postWatchBridgeNotification(appContext, activeTrack, sessionKey)
        }

        // 确保下一整分钟 :00.150 秒准时推送新一帧
        CourseAlarmScheduler.scheduleNextMinuteLiveTick(appContext)
        return diagnostics
    }

    /**
     * 当用户退回桌面或切换应用时：仅当当前确实处于上课进行中或课前提醒窗口且未收起时，自动挂载/刷新实况卡片
     */
    fun triggerAutoBackgroundFluidCloud(context: Context) {
        val appContext = context.applicationContext
        val repository = DataRepository(appContext)
        if (!repository.fluidCloudEnabled.value) return

        val now = LocalDateTime.now()
        val activeTrack = repository.resolveActiveOrUpcomingTrack(
            now = now,
            leadMinutes = repository.leadMinutes.value,
            allowAnyUpcomingToday = false,
            respectDismissed = true
        )
        if (activeTrack == null || activeTrack.playState == TrackPlayState.COMPLETED) {
            return
        }

        val sessionKey = repository.buildSessionKey(
            date = now.toLocalDate(),
            courseId = activeTrack.course.id,
            startSection = activeTrack.course.startSection
        )

        postFluidCloudTrackNotification(
            context = appContext,
            track = activeTrack,
            artistTheme = repository.artistTheme.value,
            watchBridgeEnabled = repository.watchBridgeEnabled.value,
            cardStyle = repository.notificationCardStyle.value,
            sessionKey = sessionKey,
            isSilentUpdate = true,
            startLiveUpdates = true
        )
        CourseAlarmScheduler.scheduleNextMinuteLiveTick(appContext)
    }

    /**
     * 构建定宽紧凑的状态栏胶囊单元帧（用于实况胶囊模式的单字/短语展示，不折行）
     */
    fun buildIslandMarqueeFrame(
        courseNameRaw: String,
        roomRaw: String,
        startTimeRaw: String,
        endTimeRaw: String,
        minutes: Int,
        playState: TrackPlayState,
        step: Int
    ): IslandCarouselFrame {
        val cleanCourse = sanitizeCourseTitle(courseNameRaw)
        val shortCourseName = when {
            cleanCourse.contains("高等数学") -> "高等数学"
            cleanCourse.contains("大学英语") -> "大学英语"
            cleanCourse.contains("大学物理") -> "大学物理"
            cleanCourse.contains("马克思主义") -> "马原"
            cleanCourse.contains("毛泽东思想") -> "毛概"
            cleanCourse.contains("近代史") -> "近代史"
            cleanCourse.contains("思修") -> "思修"
            cleanCourse.contains("计算机网络") -> "计网"
            cleanCourse.length <= 5 -> cleanCourse
            else -> cleanCourse.take(5)
        }

        val cleanRoom = sanitizeRoom(roomRaw)
        val hasValidRoom = isValidRoom(cleanRoom)
        val timeOnly = startTimeRaw.ifBlank { "08:00" }

        val countdownText = when (playState) {
            TrackPlayState.NOW_PLAYING -> "${minutes}m下课"
            TrackPlayState.UPCOMING -> "${minutes}m上课"
            TrackPlayState.COMPLETED -> "已下课"
        }

        val unitIndex = step.mod(3)
        val (stage, unitText, unitTag) = when (unitIndex) {
            0 -> Triple(IslandScrollStage.COURSE_NAME, shortCourseName, "课程")
            1 -> if (hasValidRoom) {
                Triple(IslandScrollStage.ROOM_LOCATION, cleanRoom.take(6), "教室")
            } else {
                Triple(IslandScrollStage.CLASS_TIME, "$timeOnly 上课", "时间")
            }
            else -> Triple(IslandScrollStage.CLASS_TIME, countdownText, "状态")
        }

        val fullSummary = if (hasValidRoom) {
            "$timeOnly-$endTimeRaw $cleanRoom · $cleanCourse · $countdownText"
        } else {
            "$timeOnly-$endTimeRaw · $cleanCourse · $countdownText"
        }

        return IslandCarouselFrame(
            stage = stage,
            shortCriticalText = unitText,
            capsuleLeftTitle = unitTag,
            capsuleRightContent = unitText,
            subTextBanner = if (hasValidRoom) "$cleanRoom · $countdownText" else countdownText,
            marqueeFullText = fullSummary
        )
    }

    /**
     * 发送或更新课程流体通知（根据用户选择的 `DOT_CARD` 或 `LIVE_CAPSULE` 渲染）
     */
    fun postFluidCloudTrackNotification(
        context: Context,
        track: TodayTrackUiModel,
        artistTheme: ArtistTheme,
        watchBridgeEnabled: Boolean,
        preferredOsFamily: LiveCapsuleOsFamily = LiveCapsuleOsFamily.AUTO,
        carouselFrameIndex: Int = 0,
        isCarouselTick: Boolean = false,
        cardStyle: NotificationCardStyle? = null,
        sessionKey: String = "",
        isSilentUpdate: Boolean = isCarouselTick,
        startLiveUpdates: Boolean = !isCarouselTick
    ): FluidCloudDiagnostics {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val repository = DataRepository(appContext)
        val resolvedStyle = cardStyle ?: repository.notificationCardStyle.value

        val effectiveOs = if (preferredOsFamily == LiveCapsuleOsFamily.AUTO) {
            detectDeviceOsFamily()
        } else {
            preferredOsFamily
        }

        val effectiveSessionKey = sessionKey.ifBlank {
            repository.buildSessionKey(
                date = LocalDate.now(),
                courseId = track.course.id,
                startSection = track.course.startSection
            )
        }

        val notification = createLiveTrackNotification(
            context = appContext,
            track = track,
            artistTheme = artistTheme,
            watchBridgeEnabled = watchBridgeEnabled,
            preferredOsFamily = preferredOsFamily,
            carouselFrameIndex = carouselFrameIndex,
            isCarouselTick = isSilentUpdate,
            cardStyle = resolvedStyle,
            sessionKey = effectiveSessionKey
        )

        val minutes = track.minutesUntilStartOrEnd.coerceAtLeast(1)
        val activeFrame = buildIslandMarqueeFrame(
            courseNameRaw = track.course.name,
            roomRaw = track.course.room,
            startTimeRaw = track.startTime,
            endTimeRaw = track.endTime,
            minutes = minutes,
            playState = track.playState,
            step = carouselFrameIndex
        )

        val progressCurrent = when (track.playState) {
            TrackPlayState.NOW_PLAYING -> (track.progressFraction * 100f).roundToInt().coerceIn(2, 99)
            TrackPlayState.UPCOMING -> 0
            TrackPlayState.COMPLETED -> 100
        }

        val canPostPromoted = checkPromotedPermission(appContext)
        val hasPromotable = if (Build.VERSION.SDK_INT >= 36) {
            runCatching { notification.hasPromotableCharacteristics() }.getOrDefault(false)
        } else {
            resolvedStyle == NotificationCardStyle.LIVE_CAPSULE
        }

        try {
            manager.notify(NOTIFICATION_ID_FLUID_TRACK, notification)
        } catch (e: Exception) {
            android.util.Log.e("ColorOsFluidCloud", "通知发送异常，启动标准兼容兜底", e)
            runCatching {
                val cleanRoom = sanitizeRoom(track.course.room)
                val headline = if (isValidRoom(cleanRoom)) {
                    "${track.startTime}-${track.endTime} $cleanRoom"
                } else {
                    "${track.startTime}-${track.endTime}"
                }
                val fallbackCompat = NotificationCompat.Builder(appContext, CHANNEL_ID_FLUID_CLOUD)
                    .setSmallIcon(R.drawable.ic_stat_schedule)
                    .setContentTitle(headline)
                    .setContentText("${sanitizeCourseTitle(track.course.name)} · ${formatCountdownLabel(track.playState, minutes)}")
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setSilent(isSilentUpdate)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()
                manager.notify(NOTIFICATION_ID_FLUID_TRACK, fallbackCompat)
            }
        }

        _islandPreviewState.value = IslandLivePreviewState(
            isActive = true,
            stage = if (!isSilentUpdate) IslandScrollStage.POPUP_EXPANDED else activeFrame.stage,
            islandCapsuleText = activeFrame.shortCriticalText,
            fullMarqueeText = activeFrame.marqueeFullText,
            scrollOffset = carouselFrameIndex,
            capsuleLeftLabel = activeFrame.capsuleLeftTitle,
            capsuleRightValue = activeFrame.capsuleRightContent,
            fullCourseName = sanitizeCourseTitle(track.course.name),
            roomName = sanitizeRoom(track.course.room),
            timeRange = "${track.startTime}-${track.endTime}",
            progressPercent = progressCurrent
        )

        // 仅在「状态栏实况胶囊」模式按需开启实时动态轮播；「极简点阵流体卡」依靠硬件 RTC 整分钟心跳即可，零多余唤醒
        if (startLiveUpdates && resolvedStyle == NotificationCardStyle.LIVE_CAPSULE) {
            startLiveCapsuleLoop(
                context = appContext,
                preferredOsFamily = preferredOsFamily
            )
        } else if (resolvedStyle == NotificationCardStyle.DOT_CARD) {
            stopIslandCarousel(resetPreview = false)
            FluidCloudForegroundService.stopCarousel(appContext)
        }

        val summary = when (resolvedStyle) {
            NotificationCardStyle.DOT_CARD -> "已推送极简点阵流体卡 (${track.course.name})"
            NotificationCardStyle.LIVE_CAPSULE -> "已推送 ${effectiveOs.displayName} ${effectiveOs.featureName}"
        }

        return FluidCloudDiagnostics(
            detectedOsFamily = effectiveOs,
            canPostPromoted = canPostPromoted,
            hasPromotableCharacteristics = hasPromotable,
            cardStyle = resolvedStyle,
            statusSummary = summary
        )
    }

    /**
     * 构建通知实例：依据 `cardStyle` 自动分流渲染高颜值 `DOT_CARD` 或 `LIVE_CAPSULE`
     */
    fun createLiveTrackNotification(
        context: Context,
        track: TodayTrackUiModel,
        artistTheme: ArtistTheme,
        watchBridgeEnabled: Boolean,
        preferredOsFamily: LiveCapsuleOsFamily = LiveCapsuleOsFamily.AUTO,
        carouselFrameIndex: Int = 0,
        isCarouselTick: Boolean = false,
        cardStyle: NotificationCardStyle = DataRepository(context.applicationContext).notificationCardStyle.value,
        sessionKey: String = ""
    ): Notification {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)

        if (!isCarouselTick || sessionFixedWhenMs == 0L) {
            sessionFixedWhenMs = System.currentTimeMillis()
        }

        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(appContext, CourseReminderReceiver::class.java).apply {
            action = ACTION_DISMISS_FLUID_CLOUD
            if (sessionKey.isNotBlank()) {
                putExtra(CourseAlarmScheduler.EXTRA_SESSION_KEY, sessionKey)
            }
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            appContext,
            1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return if (cardStyle == NotificationCardStyle.DOT_CARD) {
            buildDotCardNotification(
                appContext = appContext,
                track = track,
                artistTheme = artistTheme,
                watchBridgeEnabled = watchBridgeEnabled,
                isSilentUpdate = isCarouselTick,
                contentPendingIntent = contentPendingIntent,
                dismissPendingIntent = dismissPendingIntent
            )
        } else {
            buildLiveCapsuleNotification(
                appContext = appContext,
                track = track,
                artistTheme = artistTheme,
                watchBridgeEnabled = watchBridgeEnabled,
                carouselFrameIndex = carouselFrameIndex,
                isSilentUpdate = isCarouselTick,
                contentPendingIntent = contentPendingIntent,
                dismissPendingIntent = dismissPendingIntent
            )
        }
    }

    /**
     * 样式一：极简点阵流体卡 (DOT_CARD) —— 默认首选
     * 复刻顶尖流体卡片美学：
     * - 顶行左对齐粗体 `18:30-20:05 · 二教208`，右上角 28dp 《课轨》黑胶小标，无笨重左方框
     * - 次行低饱和纯净课名 + 节次
     * - 中部 28 枚高精圆点水平时间轴：课前全灰静默等待，开课后从左至右逐分点亮
     * - 底行左侧高辨识度珊瑚红/主题色实时状态，右侧紧凑胶囊按钮「已到教室」
     */
    private fun buildDotCardNotification(
        appContext: Context,
        track: TodayTrackUiModel,
        artistTheme: ArtistTheme,
        watchBridgeEnabled: Boolean,
        isSilentUpdate: Boolean,
        contentPendingIntent: PendingIntent,
        dismissPendingIntent: PendingIntent
    ): Notification {
        val palette = resolveCardPalette(appContext, artistTheme)
        val cleanCourse = sanitizeCourseTitle(track.course.name)
        val cleanRoom = sanitizeRoom(track.course.room)
        val hasValidRoom = isValidRoom(cleanRoom)
        val minutes = track.minutesUntilStartOrEnd.coerceAtLeast(1)

        val headlineText = if (hasValidRoom) {
            "${track.startTime}-${track.endTime}  $cleanRoom"
        } else {
            "${track.startTime}-${track.endTime}"
        }
        val courseSubtitle = "$cleanCourse · ${track.course.sectionRangeText()}"
        val countdownLabel = formatCountdownLabel(track.playState, minutes)

        val vinylBitmap = getOrCreateVinylBadgeBitmap(
            accentColor = palette.accentArgb,
            isDarkSurface = palette.isDarkSurface
        )
        val dotsBitmap = renderDotTimelineBitmap(
            playState = track.playState,
            progressFraction = track.progressFraction,
            litColor = palette.statusAccentArgb,
            unlitColor = palette.unlitDotArgb
        )

        val expandedViews = RemoteViews(appContext.packageName, R.layout.notification_fluid_course_expanded).apply {
            setTextViewText(R.id.notif_time_room, headlineText)
            setTextColor(R.id.notif_time_room, palette.headlineArgb)
            setImageViewBitmap(R.id.notif_brand_badge, vinylBitmap)

            setTextViewText(R.id.notif_course_name, courseSubtitle)
            setTextColor(R.id.notif_course_name, palette.mutedTextArgb)

            setImageViewBitmap(R.id.notif_dot_timeline, dotsBitmap)

            setTextViewText(R.id.notif_status_countdown, countdownLabel)
            setTextColor(R.id.notif_status_countdown, palette.statusAccentArgb)

            setTextViewText(R.id.notif_action_dismiss, "已到教室")
            setTextColor(R.id.notif_action_dismiss, palette.pillTextArgb)
            setInt(R.id.notif_action_dismiss, "setBackgroundResource", palette.pillBgResId)
            setOnClickPendingIntent(R.id.notif_action_dismiss, dismissPendingIntent)
        }

        val compactViews = RemoteViews(appContext.packageName, R.layout.notification_fluid_course_compact).apply {
            setTextViewText(R.id.notif_compact_time_room, headlineText)
            setTextColor(R.id.notif_compact_time_room, palette.headlineArgb)

            setTextViewText(R.id.notif_compact_course, cleanCourse)
            setTextColor(R.id.notif_compact_course, palette.mutedTextArgb)

            setTextViewText(R.id.notif_compact_status, countdownLabel)
            setTextColor(R.id.notif_compact_status, palette.statusAccentArgb)

            setTextViewText(R.id.notif_compact_dismiss, "已到教室")
            setTextColor(R.id.notif_compact_dismiss, palette.pillTextArgb)
            setInt(R.id.notif_compact_dismiss, "setBackgroundResource", palette.pillBgResId)
            setOnClickPendingIntent(R.id.notif_compact_dismiss, dismissPendingIntent)

            setImageViewBitmap(R.id.notif_compact_badge, vinylBitmap)
        }

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID_FLUID_CLOUD)
            .setSmallIcon(R.drawable.ic_stat_schedule)
            .setColor(palette.statusAccentArgb)
            .setContentTitle(headlineText)
            .setContentText("$cleanCourse · $countdownLabel")
            .setCustomContentView(compactViews)
            .setCustomBigContentView(expandedViews)
            .setCustomHeadsUpContentView(expandedViews)
            .setOngoing(true)
            .setOnlyAlertOnce(isSilentUpdate)
            .setSilent(isSilentUpdate)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setWhen(sessionFixedWhenMs)
            .setShowWhen(false)
            .setContentIntent(contentPendingIntent)

        if (!isSilentUpdate && watchBridgeEnabled) {
            builder.setVibrate(longArrayOf(0, 160, 90, 160))
        }

        return builder.build().apply {
            flags = flags or Notification.FLAG_ONGOING_EVENT
            if (isSilentUpdate) {
                flags = flags or Notification.FLAG_ONLY_ALERT_ONCE
            }
        }
    }

    /**
     * 样式二：Android 16 状态栏实况胶囊 (LIVE_CAPSULE)
     * 彻底修复旧版 3 大丑陋痛点：
     * 1. 移除 `setLargeIcon(badgeBitmap)` 左大方块，主标题与副标题单行流畅舒展不换行；
     * 2. 使用极简发光圆点 `setProgressTrackerIcon` 覆盖系统默认导航纸飞机箭头；
     * 3. 课前阶段进度设为 0，不再误显示 87% 已满。
     */
    @Suppress("DEPRECATION")
    private fun buildLiveCapsuleNotification(
        appContext: Context,
        track: TodayTrackUiModel,
        artistTheme: ArtistTheme,
        watchBridgeEnabled: Boolean,
        carouselFrameIndex: Int,
        isSilentUpdate: Boolean,
        contentPendingIntent: PendingIntent,
        dismissPendingIntent: PendingIntent
    ): Notification {
        val palette = resolveCardPalette(appContext, artistTheme)
        val cleanCourse = sanitizeCourseTitle(track.course.name)
        val cleanRoom = sanitizeRoom(track.course.room)
        val hasValidRoom = isValidRoom(cleanRoom)
        val minutes = track.minutesUntilStartOrEnd.coerceAtLeast(1)

        val activeFrame = buildIslandMarqueeFrame(
            courseNameRaw = cleanCourse,
            roomRaw = cleanRoom,
            startTimeRaw = track.startTime,
            endTimeRaw = track.endTime,
            minutes = minutes,
            playState = track.playState,
            step = carouselFrameIndex
        )
        val capsuleShortText = activeFrame.shortCriticalText
        val countdownLabel = formatCountdownLabel(track.playState, minutes)

        // 主标题放核心时间地点，副标题单行呈现课名+倒计时，彻底杜绝折行
        val titleText = if (hasValidRoom) {
            "${track.startTime}-${track.endTime} · $cleanRoom"
        } else {
            "${track.startTime}-${track.endTime} · ${track.course.sectionRangeText()}"
        }
        val subtitleText = "$cleanCourse · $countdownLabel"

        val accentHex = String.format("#%06X", 0xFFFFFF and palette.statusAccentArgb)
        val (progressValue, elapsedSegmentLen, remainingSegmentLen) = when (track.playState) {
            TrackPlayState.NOW_PLAYING -> {
                val p = (track.progressFraction * 100f).roundToInt().coerceIn(2, 98)
                Triple(p, p, 100 - p)
            }
            TrackPlayState.UPCOMING -> Triple(0, 1, 99)
            TrackPlayState.COMPLETED -> Triple(100, 100, 1)
        }
        val reachedSegmentColor = if (track.playState == TrackPlayState.UPCOMING) {
            palette.unlitDotArgb
        } else {
            palette.statusAccentArgb
        }

        val vinylBitmap = getOrCreateVinylBadgeBitmap(
            accentColor = palette.accentArgb,
            isDarkSurface = palette.isDarkSurface
        )
        val trackerDotBitmap = getOrCreateProgressTrackerDotBitmap(palette.statusAccentArgb)

        val oppoIntelligentIntentJson = buildOppoIntelligentIntentJson(
            track = track,
            activeFrame = activeFrame,
            capsuleShortText = capsuleShortText,
            titleText = titleText,
            subtitleText = subtitleText,
            progress = progressValue,
            accentHex = accentHex
        )
        val originOsIslandJson = buildOriginOsAtomicIslandJson(
            shortRoom = if (hasValidRoom) cleanRoom.take(5) else track.startTime,
            activeFrame = activeFrame,
            titleText = titleText,
            subtitleText = subtitleText,
            progress = progressValue,
            accentHex = accentHex
        )
        val hyperOsFocusJson = buildHyperOsFocusParamJson(
            track = track,
            minutes = minutes,
            activeFrame = activeFrame,
            capsuleShortText = capsuleShortText,
            titleText = titleText,
            subtitleText = subtitleText,
            progress = progressValue,
            accentHex = accentHex,
            isCarouselTick = isSilentUpdate
        )
        val magicOsCapsuleJson = buildMagicOsCapsuleJson(activeFrame, accentHex)
        val magicOsLiveViewJson = buildMagicOsLiveViewJson(
            activeFrame = activeFrame,
            capsuleShortText = capsuleShortText,
            titleText = titleText,
            subtitleText = subtitleText,
            progress = progressValue,
            accentHex = accentHex
        )

        val fluidExtras = Bundle().apply {
            putBoolean("android.requestPromotedOngoing", true)
            putString("android.shortCriticalText", capsuleShortText)
            putString("shortCriticalText", capsuleShortText)

            putString("AndroidOppoIntelligentIntent", oppoIntelligentIntentJson)
            putString("oplus.notification.intelligent_intent", oppoIntelligentIntentJson)
            putBoolean("oplus.notification.fluid_cloud.enable", true)
            putInt("oplus.notification.fluid_cloud.version", 2)
            putString("oplus.notification.capsule.title", activeFrame.capsuleLeftTitle)
            putString("oplus.notification.capsule.content", activeFrame.capsuleRightContent)
            putString("oplus.notification.capsule.text", capsuleShortText)
            putString("capsuleText", capsuleShortText)
            putString("oplus.notification.card.title", titleText)
            putString("oplus.notification.card.subtitle", subtitleText)

            putInt("notification.superx.operation", if (isSilentUpdate) 1 else 0)
            putInt("operation", if (isSilentUpdate) 1 else 0)
            putBoolean("vivo.originos.island.enable", true)
            putInt("vivo.originos.island.operation", if (isSilentUpdate) 1 else 0)
            putString("vivo.originos.capsule.title", activeFrame.capsuleLeftTitle)
            putString("vivo.originos.capsule.content", activeFrame.capsuleRightContent)
            putInt("vivo.originos.island.progress", progressValue)
            putString("com.vivo.abe.island.param", originOsIslandJson)

            putString("miui.focus.param", hyperOsFocusJson)
            putString("miui.focus.ticker", activeFrame.marqueeFullText)
            val focusPics = Bundle().apply {
                val icon = Icon.createWithBitmap(vinylBitmap)
                putParcelable("miui.focus.pic_ticker", icon)
                putParcelable("miui.focus.pic_small", icon)
                putParcelable("miui.focus.pic_imageText", icon)
                putParcelable("miui.focus.pic_aod", icon)
            }
            putBundle("miui.focus.pics", focusPics)

            putInt("notification.live.operation", if (isSilentUpdate) 1 else 0)
            putString("notification.live.event", "SCHEDULE")
            putInt("notification.live.type", 3)
            putCharSequence("notification.live.titleOverlay", titleText)
            putCharSequence("notification.live.contentOverlay", subtitleText)
            putInt("notification.live.progress", progressValue)
            putString("notification.live.capsule", magicOsCapsuleJson)
            putString("com.hihonor.notification.live_view", magicOsLiveViewJson)
        }

        return if (Build.VERSION.SDK_INT >= 36) {
            val progressStyle = Notification.ProgressStyle()
                .setStyledByProgress(false)
                .setProgress(progressValue)
                .setProgressTrackerIcon(Icon.createWithBitmap(trackerDotBitmap))
                .addProgressSegment(
                    Notification.ProgressStyle.Segment(elapsedSegmentLen.coerceAtLeast(1))
                        .setColor(reachedSegmentColor)
                        .setId(1)
                )
                .addProgressSegment(
                    Notification.ProgressStyle.Segment(remainingSegmentLen.coerceAtLeast(1))
                        .setColor(palette.unlitDotArgb)
                        .setId(2)
                )

            val nativeAction = Notification.Action.Builder(
                Icon.createWithResource(appContext, R.drawable.ic_stat_schedule),
                "已到教室",
                dismissPendingIntent
            ).build()

            val nativeBuilder = Notification.Builder(appContext, CHANNEL_ID_FLUID_CLOUD)
                .setSmallIcon(R.drawable.ic_stat_schedule)
                .setContentTitle(titleText)
                .setContentText(subtitleText)
                .setShortCriticalText(capsuleShortText)
                .setStyle(progressStyle)
                .setOngoing(true)
                .setOnlyAlertOnce(isSilentUpdate)
                .setGroupSummary(false)
                .setColorized(false)
                .setColor(palette.statusAccentArgb)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setWhen(sessionFixedWhenMs)
                .setShowWhen(false)
                .setUsesChronometer(false)
                .setContentIntent(contentPendingIntent)
                .addExtras(fluidExtras)
                .addAction(nativeAction)

            if (!isSilentUpdate && watchBridgeEnabled) {
                nativeBuilder.setVibrate(longArrayOf(0, 160, 90, 160))
            }

            nativeBuilder.build().apply {
                flags = flags or
                    Notification.FLAG_ONGOING_EVENT or
                    Notification.FLAG_PROMOTED_ONGOING
                if (isSilentUpdate) {
                    flags = flags or Notification.FLAG_ONLY_ALERT_ONCE
                }
            }
        } else {
            val compatBuilder = NotificationCompat.Builder(appContext, CHANNEL_ID_FLUID_CLOUD)
                .setSmallIcon(R.drawable.ic_stat_schedule)
                .setContentTitle(titleText)
                .setContentText(subtitleText)
                .setTicker(activeFrame.marqueeFullText)
                .setOngoing(true)
                .setOnlyAlertOnce(isSilentUpdate)
                .setSilent(isSilentUpdate)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setColor(palette.statusAccentArgb)
                .setColorized(false)
                .setWhen(sessionFixedWhenMs)
                .setShowWhen(false)
                .setUsesChronometer(false)
                .setProgress(100, progressValue, false)
                .setContentIntent(contentPendingIntent)
                .addExtras(fluidExtras)
                .addAction(
                    R.drawable.ic_stat_schedule,
                    "已到教室",
                    dismissPendingIntent
                )

            if (!isSilentUpdate && watchBridgeEnabled) {
                compatBuilder.setVibrate(longArrayOf(0, 160, 90, 160))
                val wearAction = NotificationCompat.Action.Builder(
                    R.drawable.ic_stat_schedule,
                    "已到教室",
                    dismissPendingIntent
                ).build()
                compatBuilder.extend(NotificationCompat.WearableExtender().addAction(wearAction))
            }

            compatBuilder.build().apply {
                flags = flags or Notification.FLAG_ONGOING_EVENT
                if (isSilentUpdate) {
                    flags = flags or Notification.FLAG_ONLY_ALERT_ONCE
                }
            }
        }
    }

    /**
     * 实况胶囊状态栏轮播循环（每轮均重新从 DataRepository 计算实时系统时间状态，彻底杜绝冻帧）
     */
    private fun startLiveCapsuleLoop(
        context: Context,
        preferredOsFamily: LiveCapsuleOsFamily
    ) {
        stopIslandCarousel(resetPreview = false)
        FluidCloudForegroundService.startCarousel(context)

        var step = 1
        val runnable = object : Runnable {
            override fun run() {
                if (step > 360) return
                val repository = DataRepository(context)
                if (!repository.fluidCloudEnabled.value ||
                    repository.notificationCardStyle.value != NotificationCardStyle.LIVE_CAPSULE
                ) {
                    return
                }
                val liveTrack = repository.resolveActiveOrUpcomingTrack(
                    now = LocalDateTime.now(),
                    leadMinutes = repository.leadMinutes.value,
                    respectDismissed = true
                )
                if (liveTrack != null && liveTrack.playState != TrackPlayState.COMPLETED) {
                    postFluidCloudTrackNotification(
                        context = context,
                        track = liveTrack,
                        artistTheme = repository.artistTheme.value,
                        watchBridgeEnabled = repository.watchBridgeEnabled.value,
                        preferredOsFamily = preferredOsFamily,
                        carouselFrameIndex = step,
                        isCarouselTick = true,
                        cardStyle = NotificationCardStyle.LIVE_CAPSULE,
                        isSilentUpdate = true,
                        startLiveUpdates = false
                    )
                    step++
                    islandCarouselHandler.postDelayed(this, 4500L)
                }
            }
        }
        islandCarouselRunnable = runnable
        islandCarouselHandler.postDelayed(runnable, 3000L)
    }

    private fun stopIslandCarousel(resetPreview: Boolean = true) {
        islandCarouselRunnable?.let { islandCarouselHandler.removeCallbacks(it) }
        islandCarouselRunnable = null
        if (resetPreview) {
            _islandPreviewState.value = IslandLivePreviewState(isActive = false)
        }
    }

    /**
     * 绘制 28 点高精度横向渐进时间轴 Bitmap（课前 0 点点亮，开课后从左至右逐分点亮，末端带柔和微光）
     */
    private fun renderDotTimelineBitmap(
        playState: TrackPlayState,
        progressFraction: Float,
        litColor: Int,
        unlitColor: Int
    ): Bitmap {
        val width = 900
        val height = 30
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        val litCount = when (playState) {
            TrackPlayState.UPCOMING -> 0
            TrackPlayState.NOW_PLAYING -> (progressFraction.coerceIn(0f, 1f) * DOT_COUNT)
                .roundToInt()
                .coerceIn(1, DOT_COUNT)
            TrackPlayState.COMPLETED -> DOT_COUNT
        }

        val marginX = 14f
        val cy = height / 2f
        val dotRadius = 8.4f
        val stepX = (width - marginX * 2f) / (DOT_COUNT - 1).coerceAtLeast(1)

        val haloColor = (litColor and 0x00FFFFFF) or (0x4D shl 24)
        for (i in 0 until DOT_COUNT) {
            val cx = marginX + i * stepX
            if (i < litCount) {
                if (i == litCount - 1 && playState == TrackPlayState.NOW_PLAYING) {
                    paint.color = haloColor
                    canvas.drawCircle(cx, cy, dotRadius * 1.48f, paint)
                }
                paint.color = litColor
                canvas.drawCircle(cx, cy, dotRadius, paint)
            } else {
                paint.color = unlitColor
                canvas.drawCircle(cx, cy, dotRadius * 0.92f, paint)
            }
        }
        return bmp
    }

    /**
     * 绘制右上角 28dp 《课轨》黑胶小标 Bitmap（缓存复用）
     */
    private fun getOrCreateVinylBadgeBitmap(
        accentColor: Int,
        isDarkSurface: Boolean
    ): Bitmap {
        val cacheKey = "${accentColor}_$isDarkSurface"
        cachedVinylBitmap?.let { existing ->
            if (cachedVinylKey == cacheKey && !existing.isRecycled) {
                return existing
            }
        }

        val size = 96
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val half = size / 2f

        // 柔和圆角方徽底板
        paint.style = Paint.Style.FILL
        paint.color = if (isDarkSurface) 0xFF232936.toInt() else 0xFFF1F5F9.toInt()
        canvas.drawRoundRect(RectF(2f, 2f, size - 2f, size - 2f), 26f, 26f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = (accentColor and 0x00FFFFFF) or (0x55 shl 24)
        canvas.drawRoundRect(RectF(3f, 3f, size - 3f, size - 3f), 25f, 25f, paint)

        // 居中黑胶唱片盘面
        paint.style = Paint.Style.FILL
        paint.color = 0xFF0F172A.toInt()
        canvas.drawCircle(half, half, 31f, paint)

        // 唱片纹理微环
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = (accentColor and 0x00FFFFFF) or (0x99 shl 24)
        canvas.drawCircle(half, half, 22f, paint)

        // 主题色唱标芯
        paint.style = Paint.Style.FILL
        paint.color = accentColor
        canvas.drawCircle(half, half, 12f, paint)

        // 中心轴孔
        paint.color = if (isDarkSurface) 0xFF232936.toInt() else 0xFFFFFFFF.toInt()
        canvas.drawCircle(half, half, 4.2f, paint)

        cachedVinylKey = cacheKey
        cachedVinylBitmap = bmp
        return bmp
    }

    /**
     * 为 Android 16 ProgressStyle 提供极简发光圆点进度光标，替代系统默认纸飞机导航箭头
     */
    private fun getOrCreateProgressTrackerDotBitmap(accentColor: Int): Bitmap {
        val key = accentColor.toString()
        cachedTrackerDotBitmap?.let { existing ->
            if (cachedTrackerDotKey == key && !existing.isRecycled) {
                return existing
            }
        }
        val size = 36
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val half = size / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        paint.color = (accentColor and 0x00FFFFFF) or (0x55 shl 24)
        canvas.drawCircle(half, half, 16f, paint)

        paint.color = 0xFFFFFFFF.toInt()
        canvas.drawCircle(half, half, 10.5f, paint)

        paint.color = accentColor
        canvas.drawCircle(half, half, 7.5f, paint)

        cachedTrackerDotKey = key
        cachedTrackerDotBitmap = bmp
        return bmp
    }

    private fun resolveCardPalette(context: Context, artistTheme: ArtistTheme): CardPalette {
        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

        val accentColor = when (artistTheme) {
            ArtistTheme.FUJII_KAZE -> if (isDark) 0xFFFF6E5E.toInt() else 0xFFDC4C3E.toInt()
            ArtistTheme.ZTMY -> if (isDark) 0xFF34E5B8.toInt() else 0xFF0D9488.toInt()
            ArtistTheme.YORUSHIKA -> if (isDark) 0xFF38BDF8.toInt() else 0xFF0284C7.toInt()
            ArtistTheme.JAY_CHOU -> if (isDark) 0xFFFBBF24.toInt() else 0xFFB45309.toInt()
            ArtistTheme.DAVID_TAO -> if (isDark) 0xFFFF7A33.toInt() else 0xFFEA580C.toInt()
            ArtistTheme.TAYLOR_SWIFT -> if (isDark) 0xFFF472B6.toInt() else 0xFFDB2777.toInt()
            ArtistTheme.AESPA -> if (isDark) 0xFFC084FC.toInt() else 0xFF7C3AED.toInt()
            ArtistTheme.CUSTOM_JSON -> if (isDark) 0xFFFF6E5E.toInt() else 0xFFDC4C3E.toInt()
        }

        val statusCoralOrAccent = when (artistTheme) {
            ArtistTheme.FUJII_KAZE, ArtistTheme.CUSTOM_JSON -> if (isDark) 0xFFFF6E5E.toInt() else 0xFFD9483B.toInt()
            else -> accentColor
        }

        return if (isDark) {
            CardPalette(
                accentArgb = accentColor,
                headlineArgb = 0xFFF8FAFC.toInt(),
                mutedTextArgb = 0xFFA3AEBF.toInt(),
                statusAccentArgb = statusCoralOrAccent,
                unlitDotArgb = 0x38FFFFFF,
                pillBgResId = R.drawable.notif_pill_btn_dark,
                pillTextArgb = 0xFFE2E8F0.toInt(),
                isDarkSurface = true
            )
        } else {
            CardPalette(
                accentArgb = accentColor,
                headlineArgb = 0xFF0F172A.toInt(),
                mutedTextArgb = 0xFF64748B.toInt(),
                statusAccentArgb = statusCoralOrAccent,
                unlitDotArgb = 0x240F172A,
                pillBgResId = R.drawable.notif_pill_btn_light,
                pillTextArgb = 0xFF1E293B.toInt(),
                isDarkSurface = false
            )
        }
    }

    private fun sanitizeCourseTitle(raw: String): String {
        return raw
            .replace(Regex("[（(][^）)]*[）)]"), "")
            .replace(Regex("[【\\[][^】\\]]*[】\\]]"), "")
            .trim()
            .ifBlank { raw.trim().ifBlank { "当前课程" } }
    }

    private fun sanitizeRoom(raw: String): String = raw.trim().removePrefix("@").trim()

    private fun isValidRoom(cleanRoom: String): Boolean =
        cleanRoom.isNotBlank() && cleanRoom != "待定教室"

    private fun formatCountdownLabel(playState: TrackPlayState, minutes: Int): String {
        val m = minutes.coerceAtLeast(1)
        return when (playState) {
            TrackPlayState.NOW_PLAYING -> "${m}分钟后下课"
            TrackPlayState.UPCOMING -> "${m}分钟后上课"
            TrackPlayState.COMPLETED -> "本节已下课"
        }
    }

    private fun buildOppoIntelligentIntentJson(
        track: TodayTrackUiModel,
        activeFrame: IslandCarouselFrame,
        capsuleShortText: String,
        titleText: String,
        subtitleText: String,
        progress: Int,
        accentHex: String
    ): String {
        val titleArr = JSONArray().put(
            JSONObject()
                .put("text", titleText)
                .put("color", accentHex)
                .put("darkColor", accentHex)
        )
        val nodeLabels = JSONArray()
            .put("课前准备")
            .put(track.course.room.ifBlank { "前往教室" })
            .put("准时上课")

        val capsuleObj = JSONObject()
            .put("status", 1)
            .put("text", capsuleShortText)
            .put("title", activeFrame.capsuleLeftTitle)
            .put("content", activeFrame.capsuleRightContent)
            .put("rightText", capsuleShortText)
            .put("legacyText", capsuleShortText)
            .put("bgColor", accentHex)

        val entity = JSONObject()
            .put("entityName", "TASK")
            .put("entityId", "classtrack_${track.course.id}_${track.trackNumber}")
            .put("milestone", JSONObject().put("code", 10).put("text", "course_upcoming"))
            .put("capsule", capsuleObj)
            .put("primary", JSONObject().put("title", titleArr).put("content", subtitleText))
            .put(
                "secondaryData",
                JSONObject()
                    .put("type", "PROGRESS")
                    .put("progress", progress)
                    .put("style", "inside")
                    .put("nodeLabels", nodeLabels)
            )

        return JSONObject()
            .put("intentName", "ClassTrack.Progress")
            .put("identifier", "classtrack_fluid_${track.course.id}")
            .put("timestamp", sessionFixedWhenMs)
            .put("intentAction", JSONObject().put("actionStatus", 0))
            .put("intentEntity", entity)
            .toString()
    }

    private fun buildOriginOsAtomicIslandJson(
        shortRoom: String,
        activeFrame: IslandCarouselFrame,
        titleText: String,
        subtitleText: String,
        progress: Int,
        accentHex: String
    ): String {
        val nodes = JSONArray()
            .put(JSONObject().put("label", "课前提醒").put("done", true))
            .put(JSONObject().put("label", shortRoom).put("done", progress >= 50))
            .put(JSONObject().put("label", "开始上课").put("done", progress >= 100))

        return JSONObject()
            .put("operation", 1)
            .put("scene", "COURSE_SCHEDULE")
            .put("templateType", "PROGRESS")
            .put(
                "capsule",
                JSONObject()
                    .put("title", activeFrame.capsuleLeftTitle)
                    .put("subTitle", activeFrame.capsuleRightContent)
                    .put("summaryText", activeFrame.marqueeFullText)
                    .put("highlightColor", accentHex)
                    .put("progress", progress)
            )
            .put(
                "card",
                JSONObject()
                    .put("title", titleText)
                    .put("content", subtitleText)
                    .put("progress", progress)
                    .put("accentColor", accentHex)
                    .put("nodes", nodes)
            )
            .toString()
    }

    private fun buildHyperOsFocusParamJson(
        track: TodayTrackUiModel,
        minutes: Int,
        activeFrame: IslandCarouselFrame,
        capsuleShortText: String,
        titleText: String,
        subtitleText: String,
        progress: Int,
        accentHex: String,
        isCarouselTick: Boolean
    ): String {
        val imageTextInfoLeft = JSONObject()
            .put("type", 1)
            .put("picInfo", JSONObject().put("type", 1).put("pic", "miui.focus.pic_imageText"))
            .put(
                "textInfo",
                JSONObject()
                    .put("frontTitle", activeFrame.capsuleLeftTitle)
                    .put("title", activeFrame.capsuleRightContent)
                    .put("content", "${minutes}分钟 · ${track.startTime}-${track.endTime}")
                    .put("useHighLight", true)
            )

        val bigIslandArea = JSONObject()
            .put("imageTextInfoLeft", imageTextInfoLeft)
            .put(
                "progressInfo",
                JSONObject()
                    .put("progress", progress)
                    .put("colorReach", accentHex)
                    .put("colorUnreach", "#475569")
            )

        val smallIslandArea = JSONObject()
            .put("picInfo", JSONObject().put("type", 1).put("pic", "miui.focus.pic_small"))
            .put(
                "textInfo",
                JSONObject()
                    .put("title", activeFrame.capsuleLeftTitle)
                    .put("content", activeFrame.capsuleRightContent)
            )

        val paramIsland = JSONObject()
            .put("islandProperty", 1)
            .put("highlightColor", accentHex)
            .put("bigIslandArea", bigIslandArea)
            .put("smallIslandArea", smallIslandArea)
            .put(
                "shareData",
                JSONObject()
                    .put("title", titleText)
                    .put("content", subtitleText)
            )

        val baseInfo = JSONObject()
            .put("type", 2)
            .put("title", titleText)
            .put("content", subtitleText)
            .put("subContent", activeFrame.subTextBanner)
            .put("colorTitle", accentHex)

        val paramV2 = JSONObject()
            .put("protocol", 1)
            .put("business", "schedule")
            .put("enableFloat", !isCarouselTick)
            .put("updatable", true)
            .put("ticker", activeFrame.marqueeFullText)
            .put("tickerPic", "miui.focus.pic_ticker")
            .put("aodTitle", capsuleShortText)
            .put("aodPic", "miui.focus.pic_aod")
            .put("param_island", paramIsland)
            .put("baseInfo", baseInfo)
            .put("hintInfo", JSONObject().put("type", 1).put("title", activeFrame.subTextBanner))

        return JSONObject().put("param_v2", paramV2).toString()
    }

    private fun buildMagicOsCapsuleJson(
        activeFrame: IslandCarouselFrame,
        accentHex: String
    ): String {
        return JSONObject()
            .put("type", 1)
            .put("status", 1)
            .put("bgColor", accentHex)
            .put("title", activeFrame.capsuleLeftTitle)
            .put("content", activeFrame.capsuleRightContent)
            .put("remind", activeFrame.marqueeFullText)
            .toString()
    }

    private fun buildMagicOsLiveViewJson(
        activeFrame: IslandCarouselFrame,
        capsuleShortText: String,
        titleText: String,
        subtitleText: String,
        progress: Int,
        accentHex: String
    ): String {
        return JSONObject()
            .put("operation", 1)
            .put("event", "SCHEDULE")
            .put("type", 3)
            .put("titleOverlay", titleText)
            .put("contentOverlay", subtitleText)
            .put("progress", progress)
            .put("accentColor", accentHex)
            .put(
                "capsule",
                JSONObject()
                    .put("title", activeFrame.capsuleLeftTitle)
                    .put("content", capsuleShortText)
                    .put("bgColor", accentHex)
            )
            .toString()
    }

    fun postWatchBridgeNotification(
        context: Context,
        track: TodayTrackUiModel,
        sessionKey: String = ""
    ) {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val dismissIntent = Intent(appContext, CourseReminderReceiver::class.java).apply {
            action = ACTION_DISMISS_FLUID_CLOUD
            if (sessionKey.isNotBlank()) {
                putExtra(CourseAlarmScheduler.EXTRA_SESSION_KEY, sessionKey)
            }
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            appContext,
            2,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            appContext,
            3,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val minutes = track.minutesUntilStartOrEnd.coerceAtLeast(1)
        val statusText = when (track.playState) {
            TrackPlayState.NOW_PLAYING -> "正在上课 (${minutes}分钟后下课)"
            TrackPlayState.UPCOMING -> "${minutes}分钟后上课"
            TrackPlayState.COMPLETED -> "本节已下课"
        }

        val wearAction = NotificationCompat.Action.Builder(
            R.drawable.ic_stat_schedule,
            "已到教室",
            dismissPendingIntent
        ).build()

        val wearableExtender = NotificationCompat.WearableExtender().addAction(wearAction)
        val cleanRoom = sanitizeRoom(track.course.room)
        val bodyLine = if (isValidRoom(cleanRoom)) {
            "$cleanRoom · ${track.startTime}-${track.endTime} · $statusText"
        } else {
            "${track.startTime}-${track.endTime} · $statusText"
        }

        val watchNotification = NotificationCompat.Builder(appContext, CHANNEL_ID_WATCH_BRIDGE)
            .setSmallIcon(R.drawable.ic_stat_schedule)
            .setColor(0xFF2563EB.toInt())
            .setContentTitle(sanitizeCourseTitle(track.course.name))
            .setContentText(bodyLine)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.ic_stat_schedule, "已到教室", dismissPendingIntent)
            .extend(wearableExtender)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

        runCatching {
            manager.notify(NOTIFICATION_ID_WATCH_BRIDGE, watchNotification)
        }
    }

    fun sendWatchTestNotification(context: Context) {
        val testTrack = TodayTrackUiModel(
            trackNumber = 1,
            course = CourseItem(
                name = "无机及分析化学",
                room = "二教208",
                dayOfWeek = 1,
                startSection = 10,
                endSection = 11
            ),
            startTime = "18:30",
            endTime = "20:05",
            playState = TrackPlayState.UPCOMING,
            progressFraction = 0f,
            minutesUntilStartOrEnd = 6
        )
        postWatchBridgeNotification(context, testTrack)
    }

    fun cancelFluidCloudNotification(context: Context) {
        stopIslandCarousel(resetPreview = true)
        FluidCloudForegroundService.stopCarousel(context)
        sessionFixedWhenMs = 0L
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID_FLUID_TRACK)
        manager.cancel(NOTIFICATION_ID_WATCH_BRIDGE)
        manager.cancel(LEGACY_NOTIFICATION_ID_MEDIA)
    }
}

class CourseReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext
        val action = intent?.action ?: return

        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ClassTrack:RtcReceiverLock")
        runCatching { wakeLock?.acquire(8_000L) }

        try {
            when (action) {
                ColorOsFluidCloudHelper.ACTION_DISMISS_FLUID_CLOUD -> {
                    val sessionKey = intent.getStringExtra(CourseAlarmScheduler.EXTRA_SESSION_KEY).orEmpty()
                    if (sessionKey.isNotBlank()) {
                        DataRepository(appContext).markSessionDismissed(sessionKey)
                    }
                    ColorOsFluidCloudHelper.cancelFluidCloudNotification(appContext)
                    CourseAlarmScheduler.cancelLiveTick(appContext)
                }

                ColorOsFluidCloudHelper.ACTION_TRIGGER_COURSE_REMINDER -> {
                    val eventType = intent.getStringExtra(CourseAlarmScheduler.EXTRA_EVENT_TYPE)
                        ?: CourseAlarmScheduler.EVENT_PRE_CLASS
                    val targetCourseId = intent.getLongExtra(CourseAlarmScheduler.EXTRA_COURSE_ID, -1L)

                    when (eventType) {
                        CourseAlarmScheduler.EVENT_CLASS_END -> {
                            // 本节下课节点：补偿毫秒级系统早到偏差，确认下课后收起或顺延下一节
                            ColorOsFluidCloudHelper.postOrUpdateActiveLiveReminder(
                                context = appContext,
                                isSilentUpdate = true,
                                referenceNow = LocalDateTime.now().plusSeconds(3)
                            )
                        }

                        CourseAlarmScheduler.EVENT_CLASS_START -> {
                            // 准点开课节点：立即将通知切换为「X分钟后下课」+ 点亮实时点阵进度（静默无打扰切入上课态）
                            ColorOsFluidCloudHelper.postOrUpdateActiveLiveReminder(
                                context = appContext,
                                isSilentUpdate = true,
                                targetCourseId = targetCourseId,
                                referenceNow = LocalDateTime.now().plusSeconds(3)
                            )
                        }

                        else -> {
                            // 课前提前提醒节点：首发脉冲提醒 + 穿戴手表同步 + 开启每分钟精准刷新链
                            ColorOsFluidCloudHelper.postOrUpdateActiveLiveReminder(
                                context = appContext,
                                isSilentUpdate = false,
                                targetCourseId = targetCourseId,
                                allowAnyUpcomingToday = true,
                                referenceNow = LocalDateTime.now().plusSeconds(3)
                            )
                        }
                    }

                    // 将后续最近的节点晋升为强力 AlarmClock 闹钟，保证下一节点永不失联
                    CourseAlarmScheduler.rescheduleAll(appContext)
                }

                ColorOsFluidCloudHelper.ACTION_LIVE_TICK_FLUID_CLOUD -> {
                    // 严格对齐状态栏 :00.150 秒心跳：永远基于当前真实现实时间重算
                    ColorOsFluidCloudHelper.postOrUpdateActiveLiveReminder(
                        context = appContext,
                        isSilentUpdate = true
                    )
                }

                ColorOsFluidCloudHelper.ACTION_CHECK_ACTIVE_REMINDER -> {
                    CourseAlarmScheduler.rescheduleAll(appContext)
                    ColorOsFluidCloudHelper.postOrUpdateActiveLiveReminder(
                        context = appContext,
                        isSilentUpdate = true
                    )
                }

                ColorOsFluidCloudHelper.ACTION_TEST_WATCH_BRIDGE -> {
                    ColorOsFluidCloudHelper.sendWatchTestNotification(appContext)
                }

                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_LOCKED_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                    val userUnlocked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        appContext.getSystemService(UserManager::class.java)?.isUserUnlocked ?: true
                    } else {
                        true
                    }
                    if (userUnlocked) {
                        runCatching {
                            CourseAlarmScheduler.rescheduleAll(appContext)
                            ColorOsFluidCloudHelper.postOrUpdateActiveLiveReminder(
                                context = appContext,
                                isSilentUpdate = true
                            )
                        }
                    }
                }
            }
        } finally {
            runCatching {
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
            }
        }
    }
}
