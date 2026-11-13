package com.classtrack.app.system

import com.classtrack.app.data.DataRepository
import com.classtrack.app.data.model.CourseItem
import com.classtrack.app.data.model.TimeSlot
import com.classtrack.app.data.model.TrackPlayState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseTimelineStateTest {

    private val chemistryCourse = CourseItem(
        id = 101L,
        name = "无机及分析化学",
        room = "二教208",
        teacher = "主讲教师",
        dayOfWeek = 4,
        startSection = 10,
        endSection = 11,
        startWeek = 1,
        endWeek = 16
    )

    private val eveningSlots = listOf(
        TimeSlot(section = 10, startTime = "18:30", endTime = "19:15"),
        TimeSlot(section = 11, startTime = "19:20", endTime = "20:05")
    )

    @Test
    fun computeTracksForDay_exactBoundaryTransitions_matchExpectedCountdownAndProgress() {
        val courses = listOf(chemistryCourse)

        val at1824 = DataRepository.computeTracksForDay(
            courses = courses,
            timeSlots = eveningSlots,
            dayOfWeek = 4,
            week = 10,
            nowTime = LocalTime.of(18, 24, 0)
        ).single()
        assertEquals(TrackPlayState.UPCOMING, at1824.playState)
        assertEquals(6, at1824.minutesUntilStartOrEnd)
        assertEquals(0f, at1824.progressFraction, 0.0001f)

        val at182959 = DataRepository.computeTracksForDay(
            courses = courses,
            timeSlots = eveningSlots,
            dayOfWeek = 4,
            week = 10,
            nowTime = LocalTime.of(18, 29, 59)
        ).single()
        assertEquals(TrackPlayState.UPCOMING, at182959.playState)
        assertEquals(1, at182959.minutesUntilStartOrEnd)
        assertEquals(0f, at182959.progressFraction, 0.0001f)

        val at183000 = DataRepository.computeTracksForDay(
            courses = courses,
            timeSlots = eveningSlots,
            dayOfWeek = 4,
            week = 10,
            nowTime = LocalTime.of(18, 30, 0)
        ).single()
        assertEquals(TrackPlayState.NOW_PLAYING, at183000.playState)
        assertEquals(95, at183000.minutesUntilStartOrEnd)
        assertEquals(0f, at183000.progressFraction, 0.0001f)

        val at183200 = DataRepository.computeTracksForDay(
            courses = courses,
            timeSlots = eveningSlots,
            dayOfWeek = 4,
            week = 10,
            nowTime = LocalTime.of(18, 32, 0)
        ).single()
        assertEquals(TrackPlayState.NOW_PLAYING, at183200.playState)
        assertEquals(93, at183200.minutesUntilStartOrEnd)
        assertEquals(2f / 95f, at183200.progressFraction, 0.001f)

        val at200459 = DataRepository.computeTracksForDay(
            courses = courses,
            timeSlots = eveningSlots,
            dayOfWeek = 4,
            week = 10,
            nowTime = LocalTime.of(20, 4, 59)
        ).single()
        assertEquals(TrackPlayState.NOW_PLAYING, at200459.playState)
        assertEquals(1, at200459.minutesUntilStartOrEnd)
        assertTrue(at200459.progressFraction > 0.98f)

        val at200500 = DataRepository.computeTracksForDay(
            courses = courses,
            timeSlots = eveningSlots,
            dayOfWeek = 4,
            week = 10,
            nowTime = LocalTime.of(20, 5, 0)
        ).single()
        assertEquals(TrackPlayState.COMPLETED, at200500.playState)
        assertEquals(0, at200500.minutesUntilStartOrEnd)
        assertEquals(1f, at200500.progressFraction, 0.0001f)
    }

    @Test
    fun collectUpcomingMilestones_openedMidPreClassWindow_stillSchedulesClassStartAndEnd() {
        val thursday1824 = LocalDateTime.of(2026, 11, 12, 18, 24, 0)
        val milestones = CourseAlarmScheduler.collectUpcomingMilestones(
            courses = listOf(chemistryCourse),
            timeSlots = eveningSlots,
            leadMinutes = 20,
            now = thursday1824,
            weekForDateResolver = { 10 }
        )

        val thursdayMilestones = milestones.filter { it.triggerTime.toLocalDate() == LocalDate.of(2026, 11, 12) }
        assertEquals(2, thursdayMilestones.size)
        assertEquals(CourseAlarmScheduler.EVENT_CLASS_START, thursdayMilestones[0].eventType)
        assertEquals(LocalDateTime.of(2026, 11, 12, 18, 30, 1), thursdayMilestones[0].triggerTime)
        assertEquals(CourseAlarmScheduler.EVENT_CLASS_END, thursdayMilestones[1].eventType)
        assertEquals(LocalDateTime.of(2026, 11, 12, 20, 5, 1), thursdayMilestones[1].triggerTime)
    }

    @Test
    fun collectUpcomingMilestones_beforePreClass_emitsThreeOrderedMilestones() {
        val thursday1800 = LocalDateTime.of(2026, 11, 12, 18, 0, 0)
        val milestones = CourseAlarmScheduler.collectUpcomingMilestones(
            courses = listOf(chemistryCourse),
            timeSlots = eveningSlots,
            leadMinutes = 20,
            now = thursday1800,
            weekForDateResolver = { 10 }
        ).filter { it.triggerTime.toLocalDate() == LocalDate.of(2026, 11, 12) }

        assertEquals(3, milestones.size)
        assertEquals(CourseAlarmScheduler.EVENT_PRE_CLASS, milestones[0].eventType)
        assertEquals(LocalDateTime.of(2026, 11, 12, 18, 10, 1), milestones[0].triggerTime)
        assertEquals(CourseAlarmScheduler.EVENT_CLASS_START, milestones[1].eventType)
        assertEquals(CourseAlarmScheduler.EVENT_CLASS_END, milestones[2].eventType)
    }

    @Test
    fun computeNextMinuteTickMillis_alwaysAlignsAfterNextMinuteBoundary() {
        val exactMinute = 1_794_479_040_000L // :00.000
        assertEquals(exactMinute + 60_150L, CourseAlarmScheduler.computeNextMinuteTickMillis(exactMinute))

        val midMinute = exactMinute + 32_789L
        assertEquals(exactMinute + 60_150L, CourseAlarmScheduler.computeNextMinuteTickMillis(midMinute))

        val justBeforeEnd = exactMinute + 59_999L
        assertEquals(exactMinute + 60_150L, CourseAlarmScheduler.computeNextMinuteTickMillis(justBeforeEnd))
    }

    @Test
    fun resolveEffectiveWeek_automaticallyAdvancesAcrossMondayWithoutAppLaunch() {
        // 假设第10周周一为 2026-11-09，则第1周周一为 2026-09-07
        val week1Monday = LocalDate.of(2026, 9, 7)
        assertEquals(10, DataRepository.resolveEffectiveWeek(week1Monday, LocalDate.of(2026, 11, 12), 18))
        assertEquals(10, DataRepository.resolveEffectiveWeek(week1Monday, LocalDate.of(2026, 11, 15), 18))
        assertEquals(11, DataRepository.resolveEffectiveWeek(week1Monday, LocalDate.of(2026, 11, 16), 18))
    }
}
