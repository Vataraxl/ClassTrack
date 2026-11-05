package com.classtrack.app.system

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.classtrack.app.MainActivity
import com.classtrack.app.R
import com.classtrack.app.data.DataRepository
import com.classtrack.app.data.model.ArtistTheme
import com.classtrack.app.data.model.CourseItem
import com.classtrack.app.data.model.TodayTrackUiModel
import com.classtrack.app.data.model.TrackPlayState
import java.util.Locale
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
 * 通知岛实时预览与状态数据（供设置页同步展示通知岛收回后的滚动效果）
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
 * 全新架构重构：跨平台流体云、实况胶囊与穿戴设备统一调度引擎
 *
 * 借鉴业界标杆（WakeUp 课程表、CustomizeLiveUpdate、SleepDown 课程表）的优秀工程实践：
 * 1. 【高信息密度胶囊设计】：首帧直接聚焦【时间 + 核心课名】（如 "08:00 高数"），一眼尽览核心要素，彻底告别单字切片与无头信息；
 * 2. 【防系统限流机制】：将轮播间隔设定为 3.5 秒黄金舒适节奏，彻底杜绝高频 notify 撞上 Android 系统底层 Rate-Limiting 导致的通知被丢弃；
 * 3. 【官方流体云规范全面对齐】：严格对齐 ColorOS 14/15/16 官方协议字段（如 capsule.text、shortCriticalText、oplus.notification.capsule.*）；
 * 4. 【OPPO Watch X2 穿戴直连】：针对欢太健康过滤 Ongoing 的特性，设立独立的非 ongoing 高优先级手表通知通道与手腕脉冲双击振动；
 * 5. 【前台保活服务与全场景兜底】：防止退到后台后被 ColorOS 墓碑冷冻，并在任何时间退出均能保证流体云 100% 稳定挂载。
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
    const val ACTION_TEST_WATCH_BRIDGE = "com.classtrack.app.ACTION_TEST_WATCH_BRIDGE"

    private val islandCarouselHandler = Handler(Looper.getMainLooper())
    private var islandCarouselRunnable: Runnable? = null
    private var sessionFixedWhenMs: Long = 0L
    private var cachedBadgeKey: String = ""
    private var cachedBadgeBitmap: Bitmap? = null

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
        val statusSummary: String
    )

    /**
     * 自动识别当前手机所属的国产系统家族（ColorOS / OriginOS / HyperOS / MagicOS）
     */
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

    /**
     * 确保通知渠道合规就绪
     */
    fun ensureNotificationChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 清理旧版媒体通道
        runCatching {
            manager.deleteNotificationChannel(LEGACY_CHANNEL_MEDIA)
            manager.cancel(LEGACY_NOTIFICATION_ID_MEDIA)
        }

        // 1. 手机端实时流体云与状态栏实况胶囊通道 (Ongoing 持续常驻进度通知)
        val liveChannel = NotificationChannel(
            CHANNEL_ID_FLUID_CLOUD,
            "课轨 · 流体云与实况课程进度",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "用于 ColorOS 流体云、OriginOS 原子岛、HyperOS 超级岛、MagicOS 实况窗课程进度与课前提醒"
            enableVibration(true)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(liveChannel)

        // 2. 专属智能手表 / 手环穿戴提醒通道 (特别针对 OPPO Watch X2 / 欢太健康 HeyTap Health 同步)
        val watchChannel = NotificationChannel(
            CHANNEL_ID_WATCH_BRIDGE,
            "课轨 · 智能手表上课提醒 (OPPO Watch / 穿戴设备)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "专用于向 OPPO Watch X2、ColorOS Watch、Wear OS 及智能手环同步即时上课提醒，支持手腕振动"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 150, 250)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(watchChannel)
    }

    /**
     * 检查当前设备对实况通知 / 流体云 / 焦点通知的权限状态
     */
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

    /**
     * 跳转到对应系统的「流体云 / 原子岛 / 焦点通知 / 实况窗」权限设置页
     */
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
     * 构建定宽等长、无废话的状态栏实况胶囊帧：
     * - 彻底剔除无意义的 "@待定教室" 噪音；
     * - 保持每帧 4~6 字紧凑宽度，消除系统状态栏胶囊因字数忽长忽短引起的伸缩闪烁。
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
        val cleanCourse = courseNameRaw
            .replace(Regex("[（\\(][^）\\)]*[）\\)]"), "")
            .replace(Regex("[【\\[][^】\\]]*[】\\]]"), "")
            .trim()
            .ifBlank { courseNameRaw.trim().ifBlank { "当前课程" } }

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
            else -> cleanCourse.take(4)
        }

        val rawRoomTrimmed = roomRaw.trim().removePrefix("@").trim()
        val hasValidRoom = rawRoomTrimmed.isNotBlank() && rawRoomTrimmed != "待定教室"
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
                Triple(IslandScrollStage.ROOM_LOCATION, rawRoomTrimmed.take(6), "教室")
            } else {
                Triple(IslandScrollStage.CLASS_TIME, "$timeOnly 上课", "时间")
            }
            else -> Triple(IslandScrollStage.CLASS_TIME, countdownText, "状态")
        }

        val fullSummary = if (hasValidRoom) {
            "$timeOnly · $cleanCourse · $rawRoomTrimmed · $countdownText"
        } else {
            "$timeOnly · $cleanCourse · $countdownText"
        }

        return IslandCarouselFrame(
            stage = stage,
            shortCriticalText = unitText,
            capsuleLeftTitle = unitTag,
            capsuleRightContent = unitText,
            subTextBanner = if (hasValidRoom) "$rawRoomTrimmed · $countdownText" else countdownText,
            marqueeFullText = fullSummary
        )
    }

    /**
     * 发送或更新跨系统实况通知（原生进度通知，单卡纯净呈现）
     */
    fun postFluidCloudTrackNotification(
        context: Context,
        track: TodayTrackUiModel,
        artistTheme: ArtistTheme,
        watchBridgeEnabled: Boolean,
        preferredOsFamily: LiveCapsuleOsFamily = LiveCapsuleOsFamily.AUTO,
        carouselFrameIndex: Int = 0,
        isCarouselTick: Boolean = false
    ): FluidCloudDiagnostics {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val effectiveOs = if (preferredOsFamily == LiveCapsuleOsFamily.AUTO) {
            detectDeviceOsFamily()
        } else {
            preferredOsFamily
        }

        val notification = createLiveTrackNotification(
            context = appContext,
            track = track,
            artistTheme = artistTheme,
            watchBridgeEnabled = watchBridgeEnabled,
            preferredOsFamily = preferredOsFamily,
            carouselFrameIndex = carouselFrameIndex,
            isCarouselTick = isCarouselTick
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
            TrackPlayState.NOW_PLAYING -> (track.progressFraction * 100).toInt().coerceIn(8, 96)
            TrackPlayState.UPCOMING -> (100 - minutes.coerceIn(0, 45) * 100 / 45).coerceIn(10, 92)
            TrackPlayState.COMPLETED -> 100
        }

        val canPostPromoted = checkPromotedPermission(appContext)
        val hasPromotable = if (Build.VERSION.SDK_INT >= 36) {
            runCatching { notification.hasPromotableCharacteristics() }.getOrDefault(false)
        } else {
            true
        }

        try {
            manager.notify(NOTIFICATION_ID_FLUID_TRACK, notification)
        } catch (e: Exception) {
            android.util.Log.e("ColorOsFluidCloud", "主通知发送失败，启动安全降级通道", e)
            runCatching {
                val fallbackCompat = NotificationCompat.Builder(appContext, CHANNEL_ID_FLUID_CLOUD)
                    .setSmallIcon(R.drawable.ic_stat_schedule)
                    .setContentTitle(track.course.name)
                    .setContentText("${track.startTime}-${track.endTime} · ${activeFrame.subTextBanner}")
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()
                manager.notify(NOTIFICATION_ID_FLUID_TRACK, fallbackCompat)
            }
        }

        // 同步更新应用内预览状态
        _islandPreviewState.value = IslandLivePreviewState(
            isActive = true,
            stage = if (!isCarouselTick) IslandScrollStage.POPUP_EXPANDED else activeFrame.stage,
            islandCapsuleText = activeFrame.shortCriticalText,
            fullMarqueeText = activeFrame.marqueeFullText,
            scrollOffset = carouselFrameIndex,
            capsuleLeftLabel = activeFrame.capsuleLeftTitle,
            capsuleRightValue = activeFrame.capsuleRightContent,
            fullCourseName = track.course.name,
            roomName = track.course.room,
            timeRange = "${track.startTime}-${track.endTime}",
            progressPercent = progressCurrent
        )

        // 首次触发时启动状态栏胶囊 4.0s 定宽轮播
        if (!isCarouselTick) {
            startIslandCarousel(
                context = appContext,
                track = track,
                artistTheme = artistTheme,
                watchBridgeEnabled = watchBridgeEnabled,
                preferredOsFamily = preferredOsFamily
            )
        }

        return FluidCloudDiagnostics(
            detectedOsFamily = effectiveOs,
            canPostPromoted = canPostPromoted,
            hasPromotableCharacteristics = hasPromotable,
            statusSummary = "已推送 ${effectiveOs.displayName} ${effectiveOs.featureName}"
        )
    }

    /**
     * 构建跨系统实况通知对象（单行标题 + 单行精炼副标题 + 纯净进度条 + 静态高清日程徽章）
     */
    @Suppress("DEPRECATION")
    fun createLiveTrackNotification(
        context: Context,
        track: TodayTrackUiModel,
        artistTheme: ArtistTheme,
        watchBridgeEnabled: Boolean,
        preferredOsFamily: LiveCapsuleOsFamily = LiveCapsuleOsFamily.AUTO,
        carouselFrameIndex: Int = 0,
        isCarouselTick: Boolean = false
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
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            appContext,
            1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val minutes = track.minutesUntilStartOrEnd.coerceAtLeast(1)
        val statusPrefix = when (track.playState) {
            TrackPlayState.NOW_PLAYING -> "剩余${minutes}分钟下课"
            TrackPlayState.UPCOMING -> "${minutes}分钟后上课"
            TrackPlayState.COMPLETED -> "本节已结束"
        }

        val activeFrame = buildIslandMarqueeFrame(
            courseNameRaw = track.course.name,
            roomRaw = track.course.room,
            startTimeRaw = track.startTime,
            endTimeRaw = track.endTime,
            minutes = minutes,
            playState = track.playState,
            step = carouselFrameIndex
        )

        val capsuleShortText = activeFrame.shortCriticalText
        val cleanRoom = track.course.room.trim().removePrefix("@").trim()
        val hasValidRoom = cleanRoom.isNotBlank() && cleanRoom != "待定教室"

        // 卡片主标题：仅展示纯净课程名称，绝不拼接“· 待定教室”导致折行
        val titleText = track.course.name.trim().ifBlank { "当前课程" }
        // 卡片副标题：单行呈现核心时间、有效教室与状态，彻底告别三倍重复堆砌
        val subtitleText = if (hasValidRoom) {
            "$cleanRoom · ${track.startTime}-${track.endTime} · $statusPrefix"
        } else {
            "${track.startTime}-${track.endTime} (${track.course.sectionRangeText()}) · $statusPrefix"
        }

        val themeAccentArgb = when (artistTheme) {
            ArtistTheme.ZTMY -> 0xFF38BDF8.toInt()
            ArtistTheme.YORUSHIKA -> 0xFF38BDF8.toInt()
            ArtistTheme.FUJII_KAZE -> 0xFFF59E0B.toInt()
            ArtistTheme.JAY_CHOU -> 0xFFD97706.toInt()
            ArtistTheme.DAVID_TAO -> 0xFFFF6B1A.toInt()
            ArtistTheme.TAYLOR_SWIFT -> 0xFFF472B6.toInt()
            ArtistTheme.AESPA -> 0xFFC084FC.toInt()
            ArtistTheme.CUSTOM_JSON -> 0xFF38BDF8.toInt()
        }
        val themeSecondaryArgb = when (artistTheme) {
            ArtistTheme.ZTMY -> 0xFF1E3A8A.toInt()
            ArtistTheme.YORUSHIKA -> 0xFF1E3A8A.toInt()
            ArtistTheme.FUJII_KAZE -> 0xFF78350F.toInt()
            ArtistTheme.JAY_CHOU -> 0xFF78350F.toInt()
            ArtistTheme.DAVID_TAO -> 0xFF7C2D12.toInt()
            ArtistTheme.TAYLOR_SWIFT -> 0xFF4C1D95.toInt()
            ArtistTheme.AESPA -> 0xFF3B0764.toInt()
            ArtistTheme.CUSTOM_JSON -> 0xFF1E3A8A.toInt()
        }
        val accentHex = String.format("#%06X", 0xFFFFFF and themeAccentArgb)

        val progressCurrent = when (track.playState) {
            TrackPlayState.NOW_PLAYING -> (track.progressFraction * 100).toInt().coerceIn(8, 96)
            TrackPlayState.UPCOMING -> (100 - minutes.coerceIn(0, 45) * 100 / 45).coerceIn(10, 92)
            TrackPlayState.COMPLETED -> 100
        }

        // 左侧徽章固定显示节次与开课时间，并缓存复用 Bitmap，彻底消除轮播重绘导致的文字截断与状态栏闪烁
        val sectionBadgeLabel = "%02d-%02d节".format(track.course.startSection, track.course.endSection)
        val badgeTimeLabel = track.startTime.ifBlank { "08:00" }
        val badgeBitmap = getOrCreateScheduleBadgeBitmap(
            topText = sectionBadgeLabel,
            bottomText = badgeTimeLabel,
            primaryColor = themeAccentArgb,
            secondaryColor = themeSecondaryArgb
        )

        // 1. OPPO / OnePlus ColorOS 流体云 JSON
        val oppoIntelligentIntentJson = buildOppoIntelligentIntentJson(
            track = track,
            activeFrame = activeFrame,
            capsuleShortText = capsuleShortText,
            titleText = titleText,
            subtitleText = subtitleText,
            progress = progressCurrent,
            accentHex = accentHex
        )

        // 2. vivo / iQOO OriginOS 原子岛 JSON
        val originOsIslandJson = buildOriginOsAtomicIslandJson(
            shortRoom = if (hasValidRoom) cleanRoom.take(5) else badgeTimeLabel,
            activeFrame = activeFrame,
            titleText = titleText,
            subtitleText = subtitleText,
            progress = progressCurrent,
            accentHex = accentHex
        )

        // 3. 小米 / Redmi HyperOS 澎湃超级岛 & 焦点通知 JSON
        val hyperOsFocusJson = buildHyperOsFocusParamJson(
            track = track,
            minutes = minutes,
            activeFrame = activeFrame,
            capsuleShortText = capsuleShortText,
            titleText = titleText,
            subtitleText = subtitleText,
            progress = progressCurrent,
            accentHex = accentHex,
            isCarouselTick = isCarouselTick
        )

        // 4. 荣耀 MagicOS 灵动胶囊 & 实况窗 JSON
        val magicOsCapsuleJson = buildMagicOsCapsuleJson(
            activeFrame = activeFrame,
            accentHex = accentHex
        )
        val magicOsLiveViewJson = buildMagicOsLiveViewJson(
            activeFrame = activeFrame,
            capsuleShortText = capsuleShortText,
            titleText = titleText,
            subtitleText = subtitleText,
            progress = progressCurrent,
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

            putInt("notification.superx.operation", if (isCarouselTick) 1 else 0)
            putInt("operation", if (isCarouselTick) 1 else 0)
            putBoolean("vivo.originos.island.enable", true)
            putInt("vivo.originos.island.operation", if (isCarouselTick) 1 else 0)
            putString("vivo.originos.capsule.title", activeFrame.capsuleLeftTitle)
            putString("vivo.originos.capsule.content", activeFrame.capsuleRightContent)
            putInt("vivo.originos.island.progress", progressCurrent)
            putString("com.vivo.abe.island.param", originOsIslandJson)

            putString("miui.focus.param", hyperOsFocusJson)
            putString("miui.focus.ticker", activeFrame.marqueeFullText)
            val focusPics = Bundle().apply {
                val icon = Icon.createWithBitmap(badgeBitmap)
                putParcelable("miui.focus.pic_ticker", icon)
                putParcelable("miui.focus.pic_small", icon)
                putParcelable("miui.focus.pic_imageText", icon)
                putParcelable("miui.focus.pic_aod", icon)
            }
            putBundle("miui.focus.pics", focusPics)

            putInt("notification.live.operation", if (isCarouselTick) 1 else 0)
            putString("notification.live.event", "SCHEDULE")
            putInt("notification.live.type", 3)
            putCharSequence("notification.live.titleOverlay", titleText)
            putCharSequence("notification.live.contentOverlay", subtitleText)
            putInt("notification.live.progress", progressCurrent)
            putString("notification.live.capsule", magicOsCapsuleJson)
            putString("com.hihonor.notification.live_view", magicOsLiveViewJson)
        }

        return if (Build.VERSION.SDK_INT >= 36) {
            // 移除 addProgressPoint，彻底消除 ColorOS 进度条上突兀的纸飞机导航箭头
            val progressStyle = Notification.ProgressStyle()
                .setStyledByProgress(true)
                .setProgress(progressCurrent)
                .addProgressSegment(
                    Notification.ProgressStyle.Segment(progressCurrent.coerceIn(1, 99))
                        .setColor(themeAccentArgb)
                        .setId(1)
                )
                .addProgressSegment(
                    Notification.ProgressStyle.Segment((100 - progressCurrent).coerceIn(1, 99))
                        .setColor(0xFF334155.toInt())
                        .setId(2)
                )

            val nativeAction = Notification.Action.Builder(
                Icon.createWithResource(appContext, R.drawable.ic_stat_schedule),
                "已到教室 · 收起提醒",
                dismissPendingIntent
            ).build()

            val nativeBuilder = Notification.Builder(appContext, CHANNEL_ID_FLUID_CLOUD)
                .setSmallIcon(R.drawable.ic_stat_schedule)
                .setLargeIcon(badgeBitmap)
                .setContentTitle(titleText)
                .setContentText(subtitleText)
                .setShortCriticalText(capsuleShortText)
                .setStyle(progressStyle)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setGroupSummary(false)
                .setColorized(false)
                .setColor(themeAccentArgb)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setWhen(sessionFixedWhenMs)
                .setShowWhen(false)
                .setUsesChronometer(false)
                .setContentIntent(contentPendingIntent)
                .addExtras(fluidExtras)
                .addAction(nativeAction)

            if (watchBridgeEnabled && !isCarouselTick) {
                nativeBuilder.setVibrate(longArrayOf(0, 180, 100, 180))
            }

            nativeBuilder.build().apply {
                flags = flags or
                    Notification.FLAG_ONGOING_EVENT or
                    Notification.FLAG_PROMOTED_ONGOING or
                    Notification.FLAG_ONLY_ALERT_ONCE
            }
        } else {
            val compatBuilder = NotificationCompat.Builder(appContext, CHANNEL_ID_FLUID_CLOUD)
                .setSmallIcon(R.drawable.ic_stat_schedule)
                .setLargeIcon(badgeBitmap)
                .setContentTitle(titleText)
                .setContentText(subtitleText)
                .setTicker(activeFrame.marqueeFullText)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setColor(themeAccentArgb)
                .setColorized(false)
                .setWhen(sessionFixedWhenMs)
                .setShowWhen(false)
                .setUsesChronometer(false)
                .setProgress(100, progressCurrent, false)
                .setContentIntent(contentPendingIntent)
                .addExtras(fluidExtras)
                .addAction(
                    R.drawable.ic_stat_schedule,
                    "已到教室 · 收起提醒",
                    dismissPendingIntent
                )

            if (watchBridgeEnabled && !isCarouselTick) {
                compatBuilder.setVibrate(longArrayOf(0, 180, 100, 180))
                val wearAction = NotificationCompat.Action.Builder(
                    R.drawable.ic_stat_schedule,
                    "已到教室",
                    dismissPendingIntent
                ).build()
                val wearableExtender = NotificationCompat.WearableExtender()
                    .addAction(wearAction)
                compatBuilder.extend(wearableExtender)
            }

            compatBuilder.build().apply {
                flags = flags or
                    Notification.FLAG_ONGOING_EVENT or
                    Notification.FLAG_ONLY_ALERT_ONCE
            }
        }
    }

    /**
     * 启动通知岛收回后的定宽单元轮播器（4.0 秒稳定节奏，静态复用图标不闪烁）
     */
    private fun startIslandCarousel(
        context: Context,
        track: TodayTrackUiModel,
        artistTheme: ArtistTheme,
        watchBridgeEnabled: Boolean,
        preferredOsFamily: LiveCapsuleOsFamily
    ) {
        stopIslandCarousel(resetPreview = false)

        FluidCloudForegroundService.startCarousel(context)

        var step = 1
        val runnable = object : Runnable {
            override fun run() {
                if (step > 600) return
                postFluidCloudTrackNotification(
                    context = context,
                    track = track,
                    artistTheme = artistTheme,
                    watchBridgeEnabled = watchBridgeEnabled,
                    preferredOsFamily = preferredOsFamily,
                    carouselFrameIndex = step,
                    isCarouselTick = true
                )
                step++
                islandCarouselHandler.postDelayed(this, 4000L)
            }
        }
        islandCarouselRunnable = runnable
        islandCarouselHandler.postDelayed(runnable, 2500L)
    }

    /**
     * 当退出应用或退到后台时自动触发流体云实况跑马灯
     */
    fun triggerAutoBackgroundFluidCloud(context: Context) {
        val appContext = context.applicationContext
        val repository = DataRepository(appContext)
        if (!repository.fluidCloudEnabled.value) return

        val tracks = repository.buildTracksForDay(
            dayOfWeek = repository.selectedDayOfWeek.value,
            week = repository.currentWeek.value
        )
        // 智能取课策略：优先取正在上课的课程，其次即将上课的课程；
        // 若当前不在上课时段（如课后、晚上或未上课），取当天最近或首门课程；若当天未安排，生成示范课程，确保无论何时退出测试流体云 100% 弹出展示
        val targetTrack = tracks.firstOrNull { it.playState == TrackPlayState.NOW_PLAYING }
            ?: tracks.firstOrNull { it.playState == TrackPlayState.UPCOMING }
            ?: tracks.firstOrNull()
            ?: TodayTrackUiModel(
                trackNumber = 1,
                course = CourseItem(
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

        postFluidCloudTrackNotification(
            context = appContext,
            track = targetTrack.copy(
                minutesUntilStartOrEnd = targetTrack.minutesUntilStartOrEnd.takeIf { it > 0 } ?: 15
            ),
            artistTheme = repository.artistTheme.value,
            watchBridgeEnabled = repository.watchBridgeEnabled.value
        )
    }

    private fun stopIslandCarousel(resetPreview: Boolean = true) {
        islandCarouselRunnable?.let { islandCarouselHandler.removeCallbacks(it) }
        islandCarouselRunnable = null
        if (resetPreview) {
            _islandPreviewState.value = IslandLivePreviewState(isActive = false)
        }
    }

    /**
     * 绘制并缓存高颜值静态日程时间徽章：
     * 上方显示节次（如 01-02节），下方显示开课时间（如 08:50），字号精准适配绝不截断，且在轮播期间复用同一 Bitmap 实例避免状态栏闪烁
     */
    private fun getOrCreateScheduleBadgeBitmap(
        topText: String,
        bottomText: String,
        primaryColor: Int,
        secondaryColor: Int
    ): Bitmap {
        val cacheKey = "${topText}_${bottomText}_${primaryColor}_$secondaryColor"
        cachedBadgeBitmap?.let { existing ->
            if (cachedBadgeKey == cacheKey && !existing.isRecycled) {
                return existing
            }
        }

        val size = 192
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 深邃圆角底板
        paint.style = Paint.Style.FILL
        paint.color = 0xFF0F172A.toInt()
        canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), 44f, 44f, paint)

        // 顶部柔和主题色药丸条
        paint.color = secondaryColor
        canvas.drawRoundRect(RectF(18f, 18f, size - 18f, 74f), 24f, 24f, paint)

        // 精细内描边
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = primaryColor
        canvas.drawRoundRect(RectF(6f, 6f, size - 6f, size - 6f), 40f, 40f, paint)

        // 顶部节次文字（如 "01-02节"）
        paint.style = Paint.Style.FILL
        paint.color = 0xFFFFFFFF.toInt()
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 26f
        paint.isFakeBoldText = true
        canvas.drawText(topText.take(6), size / 2f, 55f, paint)

        // 下方大字号开课时间（如 "08:50"）
        paint.color = primaryColor
        paint.textSize = 42f
        canvas.drawText(bottomText.take(5), size / 2f, 142f, paint)

        cachedBadgeKey = cacheKey
        cachedBadgeBitmap = bmp
        return bmp
    }

    /**
     * 1. OPPO / OnePlus ColorOS《流体云通知/意图共享规范》AndroidOppoIntelligentIntent
     */
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
            .put("text", capsuleShortText) // ColorOS 官方流体云关键字段
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

    /**
     * 2. vivo / iQOO OriginOS《原子岛推送指南》结构化参数
     */
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

    /**
     * 3. 小米 / Redmi HyperOS《澎湃超级岛 & 焦点通知开发指南》miui.focus.param (param_v2 + param_island)
     */
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

    /**
     * 4. 荣耀 MagicOS《灵动胶囊与实况窗开发指南》胶囊态与卡片态参数
     */
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

    /**
     * 专属智能手表穿戴同步通道（特别针对 OPPO Watch X2 / 欢太健康 HeyTap Health 体系）
     *
     * 核心设计：
     * 1. 欢太健康在读取系统通知转发给 OPPO Watch X2 时，默认会过滤所有带有 FLAG_ONGOING_EVENT 的通知；
     * 2. 因此手表穿戴通知绝不设为 ongoing，保证欢太健康 100% 将其识别为即时事件通知并推送到 OPPO Watch X2；
     * 3. 配置清脆的手腕脉冲振动 longArrayOf(0, 250, 150, 250)，手腕产生明显双击触感；
     * 4. 附带 WearableExtender 与“已到教室”打卡按钮，手腕一点即可收起手机与手表提醒。
     */
    fun postWatchBridgeNotification(
        context: Context,
        track: TodayTrackUiModel
    ) {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val dismissIntent = Intent(appContext, CourseReminderReceiver::class.java).apply {
            action = ACTION_DISMISS_FLUID_CLOUD
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
            TrackPlayState.UPCOMING -> "还有${minutes}分钟上课"
            TrackPlayState.COMPLETED -> "课程已结束"
        }

        val wearAction = NotificationCompat.Action.Builder(
            R.drawable.ic_stat_schedule,
            "已到教室",
            dismissPendingIntent
        ).build()

        val wearableExtender = NotificationCompat.WearableExtender()
            .addAction(wearAction)

        val cleanRoom = track.course.room.trim().removePrefix("@").trim()
        val hasValidRoom = cleanRoom.isNotBlank() && cleanRoom != "待定教室"
        val bodyLine = if (hasValidRoom) {
            "$cleanRoom · ${track.startTime}-${track.endTime} · $statusText"
        } else {
            "${track.startTime}-${track.endTime} · $statusText"
        }

        val watchNotification = NotificationCompat.Builder(appContext, CHANNEL_ID_WATCH_BRIDGE)
            .setSmallIcon(R.drawable.ic_stat_schedule)
            .setColor(0xFF2563EB.toInt())
            .setContentTitle(track.course.name)
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

    /**
     * 测试 OPPO Watch X2 / 穿戴设备即时通知与手腕振动
     */
    fun sendWatchTestNotification(context: Context) {
        val testTrack = TodayTrackUiModel(
            trackNumber = 1,
            course = CourseItem(
                name = "高等数学",
                room = "二教 209",
                dayOfWeek = 1,
                startSection = 1,
                endSection = 2
            ),
            startTime = "08:00",
            endTime = "09:35",
            playState = TrackPlayState.UPCOMING,
            progressFraction = 0.35f,
            minutesUntilStartOrEnd = 15
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

    fun scheduleCourseReminderAlarm(
        context: Context,
        course: CourseItem,
        triggerAtMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CourseReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_COURSE_REMINDER
            putExtra(CourseAlarmScheduler.EXTRA_COURSE_NAME, course.name)
            putExtra(CourseAlarmScheduler.EXTRA_COURSE_ROOM, course.room)
            putExtra(CourseAlarmScheduler.EXTRA_COURSE_TIME, course.sectionRangeText())
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            course.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }
    }
}

class CourseReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ColorOsFluidCloudHelper.ACTION_DISMISS_FLUID_CLOUD -> {
                ColorOsFluidCloudHelper.cancelFluidCloudNotification(context)
            }
            ColorOsFluidCloudHelper.ACTION_TRIGGER_COURSE_REMINDER -> {
                val name = intent.getStringExtra(CourseAlarmScheduler.EXTRA_COURSE_NAME) ?: "即将上课课程"
                val room = intent.getStringExtra(CourseAlarmScheduler.EXTRA_COURSE_ROOM) ?: "教学楼"
                val timeRange = intent.getStringExtra(CourseAlarmScheduler.EXTRA_COURSE_TIME) ?: "08:00-09:35"
                val leadMin = intent.getIntExtra(CourseAlarmScheduler.EXTRA_LEAD_MINUTES, 20)

                val parts = timeRange.split("-")
                val startStr = parts.getOrNull(0)?.trim() ?: "08:00"
                val endStr = parts.getOrNull(1)?.trim() ?: "09:35"

                val reminderTrack = TodayTrackUiModel(
                    trackNumber = 1,
                    course = CourseItem(
                        name = name,
                        room = room,
                        dayOfWeek = 1,
                        startSection = 1,
                        endSection = 2
                    ),
                    startTime = startStr,
                    endTime = endStr,
                    playState = TrackPlayState.UPCOMING,
                    progressFraction = 0.25f,
                    minutesUntilStartOrEnd = leadMin
                )

                val repository = DataRepository(context)
                ColorOsFluidCloudHelper.postFluidCloudTrackNotification(
                    context = context,
                    track = reminderTrack,
                    artistTheme = repository.artistTheme.value,
                    watchBridgeEnabled = repository.watchBridgeEnabled.value
                )

                // 触发后续闹钟自动轮转规划，实现全自动持续推送
                CourseAlarmScheduler.rescheduleAll(context)
            }
            ColorOsFluidCloudHelper.ACTION_TEST_WATCH_BRIDGE -> {
                ColorOsFluidCloudHelper.sendWatchTestNotification(context)
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                // 手机开机重启或系统时间变更时，全自动重新计算未来一周全部课程的后台提醒闹钟
                CourseAlarmScheduler.rescheduleAll(context)
            }
        }
    }
}
