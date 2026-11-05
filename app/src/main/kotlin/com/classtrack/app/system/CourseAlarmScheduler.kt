package com.classtrack.app.system

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.classtrack.app.data.DataRepository
import com.classtrack.app.data.model.CourseItem
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
 * 全自动课前提醒调度系统 (CourseAlarmScheduler)
 *
 * 核心目标：
 * 让用户哪怕一次都不打开应用，在上课前（默认提前 20 分钟），系统依然能够
 * 准时、精准唤醒 CPU，向状态栏弹出流体云胶囊并同步向用户的 OPPO Watch X2 / 智能穿戴设备推送手腕震动提醒！
 *
 * 核心机制：
 * 1. 使用 Android 系统硬件级 `AlarmManager.RTC_WAKEUP` + `setExactAndAllowWhileIdle`，穿透系统 Doze 低电休眠；
 * 2. 计算未来 7 天内每一门符合周次条件的课程的精准触发时间戳（`上课时间 - leadMinutes`）；
 * 3. 手机开机自启（BOOT_COMPLETED）、时间变更（TIME_SET）、修改课表或提醒时间时全自动重算调度；
 * 4. 提醒触发后自动安排后续提醒，永不失联。
 */
object CourseAlarmScheduler {

    const val EXTRA_COURSE_ID = "extra_course_id"
    const val EXTRA_COURSE_NAME = "extra_course_name"
    const val EXTRA_COURSE_ROOM = "extra_course_room"
    const val EXTRA_COURSE_TIME = "extra_course_time"
    const val EXTRA_LEAD_MINUTES = "extra_lead_minutes"

    private val _nextReminderInfo = MutableStateFlow("自动推送系统已就绪")
    val nextReminderInfo: StateFlow<String> = _nextReminderInfo.asStateFlow()

    data class ScheduledReminder(
        val course: CourseItem,
        val triggerTime: LocalDateTime,
        val startTimeStr: String,
        val endTimeStr: String
    )

    /**
     * 重新规划并注册未来所有课程的精准闹钟
     */
    fun rescheduleAll(context: Context) {
        val appContext = context.applicationContext
        val repository = DataRepository(appContext)

        if (!repository.fluidCloudEnabled.value) {
            cancelAll(appContext)
            _nextReminderInfo.value = "自动推送已关闭（流体云主开关未开启）"
            return
        }

        val courses = repository.courses.value
        val slotsMap = repository.timeSlots.value.associateBy { it.section }
        val leadMinutes = repository.leadMinutes.value
        val currentWeek = repository.currentWeek.value
        val now = LocalDateTime.now()
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val upcomingList = mutableListOf<ScheduledReminder>()

        // 检索未来 7 天内的课程安排
        for (dayOffset in 0..7) {
            val targetDate = now.toLocalDate().plusDays(dayOffset.toLong())
            val targetDayOfWeek = targetDate.dayOfWeek.value // 1..7 (周一至周日)

            // 若跨周，计算对应周次
            val weekForDate = if (dayOffset == 0) {
                currentWeek
            } else {
                val currentMonday = now.toLocalDate().with(DayOfWeek.MONDAY)
                val targetMonday = targetDate.with(DayOfWeek.MONDAY)
                val weeksDiff = java.time.temporal.ChronoUnit.WEEKS.between(currentMonday, targetMonday).toInt()
                (currentWeek + weeksDiff).coerceIn(1, 30)
            }

            val activeCourses = courses.filter {
                it.dayOfWeek == targetDayOfWeek && it.isActiveInWeek(weekForDate)
            }

            for (course in activeCourses) {
                val startSlot = slotsMap[course.startSection]
                val endSlot = slotsMap[course.endSection]
                val startTimeStr = startSlot?.startTime ?: "08:00"
                val endTimeStr = endSlot?.endTime ?: "09:35"

                val startLocalTime = runCatching {
                    LocalTime.parse(startTimeStr, DateTimeFormatter.ofPattern("HH:mm"))
                }.getOrDefault(LocalTime.of(8, 0))

                val courseStartDateTime = LocalDateTime.of(targetDate, startLocalTime)
                val triggerDateTime = courseStartDateTime.minusMinutes(leadMinutes.toLong())

                // 仅对未来发生的提醒进行调度
                if (triggerDateTime.isAfter(now)) {
                    upcomingList.add(
                        ScheduledReminder(
                            course = course,
                            triggerTime = triggerDateTime,
                            startTimeStr = startTimeStr,
                            endTimeStr = endTimeStr
                        )
                    )
                }
            }
        }

        // 按时间先后升序排列
        upcomingList.sortBy { it.triggerTime }

        if (upcomingList.isEmpty()) {
            _nextReminderInfo.value = "近期暂无待上课程（课表空闲）"
            return
        }

        val zoneId = ZoneId.systemDefault()

        // 注册最近的几个精准闹钟（最多预定 8 个，避免超出系统 PendingIntent 限制）
        upcomingList.take(8).forEachIndexed { index, reminder ->
            val triggerMillis = reminder.triggerTime.atZone(zoneId).toInstant().toEpochMilli()
            val intent = Intent(appContext, CourseReminderReceiver::class.java).apply {
                action = ColorOsFluidCloudHelper.ACTION_TRIGGER_COURSE_REMINDER
                putExtra(EXTRA_COURSE_ID, reminder.course.id)
                putExtra(EXTRA_COURSE_NAME, reminder.course.name)
                putExtra(EXTRA_COURSE_ROOM, reminder.course.room)
                putExtra(EXTRA_COURSE_TIME, "${reminder.startTimeStr}-${reminder.endTimeStr}")
                putExtra(EXTRA_LEAD_MINUTES, leadMinutes)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                appContext,
                10000 + index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            }
        }

        val nearest = upcomingList.first()
        val isToday = nearest.triggerTime.toLocalDate().isEqual(now.toLocalDate())
        val isTomorrow = nearest.triggerTime.toLocalDate().isEqual(now.toLocalDate().plusDays(1))
        val dayLabel = when {
            isToday -> "今天"
            isTomorrow -> "明天"
            else -> "周" + when (nearest.triggerTime.dayOfWeek) {
                DayOfWeek.MONDAY -> "一"
                DayOfWeek.TUESDAY -> "二"
                DayOfWeek.WEDNESDAY -> "三"
                DayOfWeek.THURSDAY -> "四"
                DayOfWeek.FRIDAY -> "五"
                DayOfWeek.SATURDAY -> "六"
                DayOfWeek.SUNDAY -> "日"
            }
        }
        val timeLabel = nearest.triggerTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        _nextReminderInfo.value = "下次自动提醒：$dayLabel $timeLabel · ${nearest.course.name} (@${nearest.course.room})"
    }

    /**
     * 清理所有闹钟
     */
    fun cancelAll(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (index in 0..10) {
            val intent = Intent(appContext, CourseReminderReceiver::class.java).apply {
                action = ColorOsFluidCloudHelper.ACTION_TRIGGER_COURSE_REMINDER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                appContext,
                10000 + index,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }
}
