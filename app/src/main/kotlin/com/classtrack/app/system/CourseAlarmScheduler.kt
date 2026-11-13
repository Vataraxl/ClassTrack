package com.classtrack.app.system

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.classtrack.app.MainActivity
import com.classtrack.app.data.DataRepository
import com.classtrack.app.data.model.CourseItem
import com.classtrack.app.data.model.TimeSlot
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全自动硬件级 RTC 课轨状态机调度器 (CourseAlarmScheduler)
 *
 * 彻底解决「不打开 App 不提醒」与「到点未切入上课状态」两大核心问题：
 * 1. 全生命周期三事件节点：对每门课同时调度 `PRE_CLASS`（课前预热）、`CLASS_START`（准点切入上课）、`CLASS_END`（下课自动收起并顺延下一节）；
 * 2. 强力闹钟级唤醒（AlarmClock）：对最近 36 小时内即将触发的关键节点优先使用 `AlarmManager.setAlarmClock`，可穿透 ColorOS 墓碑机制 / 省电冻结；
 * 3. 与状态栏时钟 :00 秒同频的每分钟精确心跳（LIVE_TICK）：只要卡片处于活跃窗口，逐分准时刷新剩余分钟与高清点阵进度；
 * 4. 跨周教学周锚点 + JobScheduler 15 分钟保活巡检看门狗：即使整周不打开应用，开机、跨周、系统重启后仍自动接力唤醒。
 */
object CourseAlarmScheduler {

    const val EXTRA_COURSE_ID = "extra_course_id"
    const val EXTRA_COURSE_NAME = "extra_course_name"
    const val EXTRA_COURSE_ROOM = "extra_course_room"
    const val EXTRA_COURSE_TIME = "extra_course_time"
    const val EXTRA_LEAD_MINUTES = "extra_lead_minutes"
    const val EXTRA_EVENT_TYPE = "extra_event_type"
    const val EXTRA_SESSION_KEY = "extra_session_key"

    const val EVENT_PRE_CLASS = "PRE_CLASS"
    const val EVENT_CLASS_START = "CLASS_START"
    const val EVENT_CLASS_END = "CLASS_END"

    private const val BASE_REQUEST_CODE = 10000
    private const val MAX_ALARM_SLOTS = 24
    private const val ALARM_SHOW_INTENT_REQUEST_CODE = 10099
    private const val LIVE_TICK_REQUEST_CODE = 19999
    private const val ALARM_CLOCK_LOOKAHEAD_HOURS = 36L

    private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    data class AlarmReadinessStatus(
        val areNotificationsEnabled: Boolean,
        val canScheduleExactAlarms: Boolean,
        val isIgnoringBatteryOptimizations: Boolean,
        val canPostPromoted: Boolean
    ) {
        val isReadyForReliableBackgroundWakeup: Boolean
            get() = areNotificationsEnabled && canScheduleExactAlarms && isIgnoringBatteryOptimizations
    }

    data class ScheduledMilestone(
        val course: CourseItem,
        val eventType: String,
        val triggerTime: LocalDateTime,
        val startTimeStr: String,
        val endTimeStr: String,
        val sessionKey: String
    )

    private val _nextReminderInfo = MutableStateFlow("自动推送系统已就绪")
    val nextReminderInfo: StateFlow<String> = _nextReminderInfo.asStateFlow()

    /**
     * 诊断当前系统通知、精确闹钟、电池白名单与 Android 16 实况资格状态
     */
    fun inspectReadiness(context: Context): AlarmReadinessStatus {
        val appContext = context.applicationContext
        val notificationsEnabled = NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val exactAlarmAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val batteryIgnored = powerManager?.isIgnoringBatteryOptimizations(appContext.packageName) ?: false
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val promotedAllowed = if (Build.VERSION.SDK_INT >= 36 && nm != null) {
            runCatching { nm.canPostPromotedNotifications() }.getOrDefault(false)
        } else {
            false
        }
        return AlarmReadinessStatus(
            areNotificationsEnabled = notificationsEnabled,
            canScheduleExactAlarms = exactAlarmAllowed,
            isIgnoringBatteryOptimizations = batteryIgnored,
            canPostPromoted = promotedAllowed
        )
    }

    /**
     * 根据精确时间推导未来 7 天的三态关键节点（课前 / 上课 / 下课），供调度器与单元测试共用
     */
    fun collectUpcomingMilestones(
        courses: List<CourseItem>,
        timeSlots: List<TimeSlot>,
        leadMinutes: Int,
        now: LocalDateTime,
        weekForDateResolver: (LocalDate) -> Int,
        sessionKeyBuilder: (LocalDate, Long, Int) -> String = { d, id, sec -> "${d}_${id}_$sec" }
    ): List<ScheduledMilestone> {
        val slotsMap = timeSlots.associateBy { it.section }
        val normalizedLead = leadMinutes.coerceIn(1, 60).toLong()
        val milestones = mutableListOf<ScheduledMilestone>()

        for (dayOffset in 0L..7L) {
            val targetDate = now.toLocalDate().plusDays(dayOffset)
            val targetDayOfWeek = targetDate.dayOfWeek.value
            val weekForDate = weekForDateResolver(targetDate)

            val activeCourses = courses.filter {
                it.dayOfWeek == targetDayOfWeek && it.isActiveInWeek(weekForDate)
            }

            for (course in activeCourses) {
                val startTimeStr = slotsMap[course.startSection]?.startTime ?: "08:00"
                val endTimeStr = slotsMap[course.endSection]?.endTime ?: "09:35"

                val startLocalTime = runCatching { LocalTime.parse(startTimeStr, TIME_FORMATTER) }
                    .getOrDefault(LocalTime.of(8, 0))
                val endLocalTime = runCatching { LocalTime.parse(endTimeStr, TIME_FORMATTER) }
                    .getOrDefault(startLocalTime.plusMinutes(45))

                val courseStartDateTime = LocalDateTime.of(targetDate, startLocalTime)
                val rawEndDateTime = LocalDateTime.of(targetDate, endLocalTime)
                val courseEndDateTime = if (rawEndDateTime.isAfter(courseStartDateTime)) {
                    rawEndDateTime
                } else {
                    courseStartDateTime.plusMinutes(45)
                }
                val preClassDateTime = courseStartDateTime.minusMinutes(normalizedLead)
                val sessionKey = sessionKeyBuilder(targetDate, course.id, course.startSection)

                if (preClassDateTime.isAfter(now)) {
                    milestones.add(
                        ScheduledMilestone(
                            course = course,
                            eventType = EVENT_PRE_CLASS,
                            triggerTime = preClassDateTime.plusSeconds(1),
                            startTimeStr = startTimeStr,
                            endTimeStr = endTimeStr,
                            sessionKey = sessionKey
                        )
                    )
                }
                if (courseStartDateTime.isAfter(now)) {
                    milestones.add(
                        ScheduledMilestone(
                            course = course,
                            eventType = EVENT_CLASS_START,
                            triggerTime = courseStartDateTime.plusSeconds(1),
                            startTimeStr = startTimeStr,
                            endTimeStr = endTimeStr,
                            sessionKey = sessionKey
                        )
                    )
                }
                if (courseEndDateTime.isAfter(now)) {
                    milestones.add(
                        ScheduledMilestone(
                            course = course,
                            eventType = EVENT_CLASS_END,
                            triggerTime = courseEndDateTime.plusSeconds(1),
                            startTimeStr = startTimeStr,
                            endTimeStr = endTimeStr,
                            sessionKey = sessionKey
                        )
                    )
                }
            }
        }

        return milestones.sortedWith(
            compareBy<ScheduledMilestone>(
                { it.triggerTime },
                { eventSortRank(it.eventType) }
            )
        )
    }

    private fun eventSortRank(eventType: String): Int = when (eventType) {
        EVENT_CLASS_END -> 0
        EVENT_PRE_CLASS -> 1
        EVENT_CLASS_START -> 2
        else -> 3
    }

    /**
     * 计算与状态栏时钟 :00 秒严格对齐的下一分钟更新时间戳（+150ms 确保时间已跨过分钟分界线）
     */
    fun computeNextMinuteTickMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        return ((nowMillis / 60_000L) + 1L) * 60_000L + 150L
    }

    /**
     * 重新规划并注册未来所有课程的硬件级 RTC 状态切换闹钟（PRE_CLASS / CLASS_START / CLASS_END）
     */
    @Synchronized
    fun rescheduleAll(context: Context) {
        val appContext = context.applicationContext
        val repository = DataRepository(appContext)

        if (!repository.fluidCloudEnabled.value) {
            cancelAll(appContext)
            cancelLiveTick(appContext)
            CourseAlarmWatchdogJobService.cancel(appContext)
            _nextReminderInfo.value = "自动推送已关闭（流体云主开关未开启）"
            return
        }

        // 确保系统级 15 分钟保活巡检看门狗在线（关机重启后亦能恢复）
        CourseAlarmWatchdogJobService.ensureScheduled(appContext)

        // 先清除旧的里程碑槽位，防止旧时间或已删课程幽灵残留
        cancelAll(appContext)

        val leadMinutes = repository.leadMinutes.value.coerceIn(1, 60)
        val alarmClockPriority = repository.alarmClockPriorityEnabled.value
        val now = LocalDateTime.now()
        val readiness = inspectReadiness(appContext)

        val upcomingMilestones = collectUpcomingMilestones(
            courses = repository.courses.value,
            timeSlots = repository.timeSlots.value,
            leadMinutes = leadMinutes,
            now = now,
            weekForDateResolver = { targetDate -> repository.getEffectiveCurrentWeek(targetDate) },
            sessionKeyBuilder = { date, courseId, startSection ->
                repository.buildSessionKey(date, courseId, startSection)
            }
        )

        if (upcomingMilestones.isEmpty()) {
            _nextReminderInfo.value = "近期暂无待上课程（第${repository.getEffectiveCurrentWeek()}周课表空闲）"
            return
        }

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val zoneId = ZoneId.systemDefault()
        val alarmClockCutoff = now.plusHours(ALARM_CLOCK_LOOKAHEAD_HOURS)

        // 仅对最近 36 小时内首个未到来的课前/上课节点启用 setAlarmClock()，避免多枚系统闹钟冲突同时穿透 ColorOS 墓碑
        val primaryAlarmClockTarget = if (alarmClockPriority && readiness.canScheduleExactAlarms) {
            upcomingMilestones.firstOrNull { milestone ->
                (milestone.eventType == EVENT_PRE_CLASS || milestone.eventType == EVENT_CLASS_START) &&
                    !milestone.triggerTime.isAfter(alarmClockCutoff)
            }
        } else {
            null
        }

        val showAppPendingIntent = PendingIntent.getActivity(
            appContext,
            ALARM_SHOW_INTENT_REQUEST_CODE,
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        upcomingMilestones.take(MAX_ALARM_SLOTS).forEachIndexed { index, milestone ->
            val triggerMillis = milestone.triggerTime.atZone(zoneId).toInstant().toEpochMilli()
            val intent = Intent(appContext, CourseReminderReceiver::class.java).apply {
                action = ColorOsFluidCloudHelper.ACTION_TRIGGER_COURSE_REMINDER
                putExtra(EXTRA_COURSE_ID, milestone.course.id)
                putExtra(EXTRA_COURSE_NAME, milestone.course.name)
                putExtra(EXTRA_COURSE_ROOM, milestone.course.room)
                putExtra(EXTRA_COURSE_TIME, "${milestone.startTimeStr}-${milestone.endTimeStr}")
                putExtra(EXTRA_LEAD_MINUTES, leadMinutes)
                putExtra(EXTRA_EVENT_TYPE, milestone.eventType)
                putExtra(EXTRA_SESSION_KEY, milestone.sessionKey)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                appContext,
                BASE_REQUEST_CODE + index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            runCatching {
                when {
                    milestone === primaryAlarmClockTarget -> {
                        val info = AlarmManager.AlarmClockInfo(triggerMillis, showAppPendingIntent)
                        alarmManager.setAlarmClock(info, pendingIntent)
                    }
                    readiness.canScheduleExactAlarms -> {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    }
                    else -> {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    }
                }
            }
        }

        val nextWakeMilestone = upcomingMilestones.firstOrNull {
            it.eventType == EVENT_PRE_CLASS || it.eventType == EVENT_CLASS_START
        } ?: upcomingMilestones.first()

        val summaryPrefix = buildString {
            append(
                when (nextWakeMilestone.eventType) {
                    EVENT_CLASS_START -> "下次准点开课切换："
                    EVENT_CLASS_END -> "下次下课自动收起："
                    else -> "下次自动课前提醒："
                }
            )
            append(formatDayLabel(now.toLocalDate(), nextWakeMilestone.triggerTime.toLocalDate()))
            append(" ")
            append(nextWakeMilestone.triggerTime.format(TIME_FORMATTER))
            append(" · ")
            append(nextWakeMilestone.course.name)
            if (nextWakeMilestone.course.room.isNotBlank()) {
                append(" (@${nextWakeMilestone.course.room})")
            }
        }

        val diagnosticSuffix = buildList {
            if (!readiness.areNotificationsEnabled) add("通知权限未开")
            if (!readiness.canScheduleExactAlarms) add("未授权精确闹钟(可能延迟)")
            if (!readiness.isIgnoringBatteryOptimizations) add("建议关闭电池优化防墓碑")
            if (primaryAlarmClockTarget != null) add("已启用闹钟级穿透")
        }.joinToString(" · ")

        _nextReminderInfo.value = if (diagnosticSuffix.isBlank()) {
            summaryPrefix
        } else {
            "$summaryPrefix [$diagnosticSuffix]"
        }
    }

    /**
     * 注册下一分钟 :00.150 秒硬件级 RTC 实时心跳，确保卡片与状态栏分钟同步刷新
     */
    fun scheduleNextMinuteLiveTick(context: Context) {
        val appContext = context.applicationContext
        val repository = DataRepository(appContext)
        if (!repository.fluidCloudEnabled.value) {
            cancelLiveTick(appContext)
            return
        }

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val readiness = inspectReadiness(appContext)
        val nextTickMillis = computeNextMinuteTickMillis()
        val intent = Intent(appContext, CourseReminderReceiver::class.java).apply {
            action = ColorOsFluidCloudHelper.ACTION_LIVE_TICK_FLUID_CLOUD
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            LIVE_TICK_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        runCatching {
            if (readiness.canScheduleExactAlarms) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTickMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTickMillis, pendingIntent)
            }
        }
    }

    /**
     * 停止每分钟实时心跳（当课程结束或用户点击「已到教室」收起时调用）
     */
    fun cancelLiveTick(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(appContext, CourseReminderReceiver::class.java).apply {
            action = ColorOsFluidCloudHelper.ACTION_LIVE_TICK_FLUID_CLOUD
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            LIVE_TICK_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * 清理所有已注册的课前 / 上课 / 下课闹钟槽位
     */
    fun cancelAll(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (index in 0 until MAX_ALARM_SLOTS) {
            val intent = Intent(appContext, CourseReminderReceiver::class.java).apply {
                action = ColorOsFluidCloudHelper.ACTION_TRIGGER_COURSE_REMINDER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                appContext,
                BASE_REQUEST_CODE + index,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivitySafely(context, intent)
    }

    fun openExactAlarmPermissionSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            buildAppDetailsIntent(context)
        }
        startActivitySafely(context, intent)
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        val directIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val launched = runCatching {
            context.startActivity(directIntent)
            true
        }.getOrDefault(false)
        if (!launched) {
            startActivitySafely(
                context,
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun openOemAutoStartSettings(context: Context) {
        val candidates = listOf(
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            ComponentName("com.oplus.safecenter", "com.oplus.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
            ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
        )
        for (component in candidates) {
            val intent = Intent().apply {
                this.component = component
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (runCatching { context.startActivity(intent); true }.getOrDefault(false)) {
                return
            }
        }
        startActivitySafely(context, buildAppDetailsIntent(context))
    }

    private fun startActivitySafely(context: Context, intent: Intent) {
        val launched = runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
        if (!launched) {
            runCatching { context.startActivity(buildAppDetailsIntent(context)) }
        }
    }

    private fun buildAppDetailsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun formatDayLabel(today: LocalDate, targetDate: LocalDate): String {
        return when {
            targetDate.isEqual(today) -> "今天"
            targetDate.isEqual(today.plusDays(1)) -> "明天"
            else -> "周" + when (targetDate.dayOfWeek) {
                DayOfWeek.MONDAY -> "一"
                DayOfWeek.TUESDAY -> "二"
                DayOfWeek.WEDNESDAY -> "三"
                DayOfWeek.THURSDAY -> "四"
                DayOfWeek.FRIDAY -> "五"
                DayOfWeek.SATURDAY -> "六"
                DayOfWeek.SUNDAY -> "日"
            }
        }
    }
}
