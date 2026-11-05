package com.classtrack.app.ui.main

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.classtrack.app.data.CourseImporter
import com.classtrack.app.data.DataRepository
import com.classtrack.app.data.model.ArtistTheme
import com.classtrack.app.data.model.CourseItem
import com.classtrack.app.data.model.CustomThemeConfig
import com.classtrack.app.data.model.LyricQuote
import com.classtrack.app.data.model.TimeSlot
import com.classtrack.app.data.model.TodayTrackUiModel
import com.classtrack.app.data.model.TrackPlayState
import com.classtrack.app.data.model.UiStyle
import com.classtrack.app.system.ColorOsFluidCloudHelper
import com.classtrack.app.system.LiveCapsuleOsFamily
import com.classtrack.app.widget.ClassTrackWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class MainTab(val title: String) {
    WEEKLY_GRID("课表"),
    IMPORT_CENTER("导课"),
    THEME_STUDIO("主题"),
    SETTINGS("设置")
}

data class UiStyleTransitionEvent(
    val fromStyle: UiStyle,
    val toStyle: UiStyle,
    val timestamp: Long
)

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DataRepository(application)

    val artistTheme: StateFlow<ArtistTheme> = repository.artistTheme
    val customThemeConfig: StateFlow<CustomThemeConfig> = repository.customThemeConfig
    val customThemes: StateFlow<List<CustomThemeConfig>> = repository.customThemes
    val selectedPortalId: StateFlow<String> = repository.selectedPortalId
    val recentPortalIds: StateFlow<List<String>> = repository.recentPortalIds
    val scheduleName: StateFlow<String> = repository.scheduleName
    val currentWeek: StateFlow<Int> = repository.currentWeek
    val totalWeeks: StateFlow<Int> = repository.totalWeeks

    // 当前正在课表页浏览的周次（可自由翻页查看非本周，不影响实际当前周 currentWeek）
    private val _viewingWeek = MutableStateFlow(repository.currentWeek.value)
    val viewingWeek: StateFlow<Int> = _viewingWeek.asStateFlow()

    val maxSections: StateFlow<Int> = repository.maxSections
    val selectedDayOfWeek: StateFlow<Int> = repository.selectedDayOfWeek
    val courses: StateFlow<List<CourseItem>> = repository.courses
    val timeSlots: StateFlow<List<TimeSlot>> = repository.timeSlots
    val currentLyric: StateFlow<LyricQuote> = repository.currentLyric
    val fluidCloudEnabled: StateFlow<Boolean> = repository.fluidCloudEnabled
    val watchBridgeEnabled: StateFlow<Boolean> = repository.watchBridgeEnabled
    val leadMinutes: StateFlow<Int> = repository.leadMinutes
    val uiStyle: StateFlow<UiStyle> = repository.uiStyle
    val nextReminderInfo: StateFlow<String> = com.classtrack.app.system.CourseAlarmScheduler.nextReminderInfo

    private val _uiStyleTransitionTrigger = MutableStateFlow<UiStyleTransitionEvent?>(null)
    val uiStyleTransitionTrigger: StateFlow<UiStyleTransitionEvent?> = _uiStyleTransitionTrigger.asStateFlow()

    fun setUiStyle(newStyle: UiStyle) {
        if (uiStyle.value == newStyle) return
        val old = uiStyle.value
        repository.setUiStyle(newStyle)
        _uiStyleTransitionTrigger.value = UiStyleTransitionEvent(
            fromStyle = old,
            toStyle = newStyle,
            timestamp = System.currentTimeMillis()
        )
        _toastMessage.value = "已切换为【${newStyle.displayName}】风格"
    }

    fun clearUiStyleTransition() {
        _uiStyleTransitionTrigger.value = null
    }

    private val _activeTab = MutableStateFlow(MainTab.WEEKLY_GRID)
    val activeTab: StateFlow<MainTab> = _activeTab.asStateFlow()

    private val _editingCourse = MutableStateFlow<CourseItem?>(null)
    val editingCourse: StateFlow<CourseItem?> = _editingCourse.asStateFlow()

    private val _showCourseDialog = MutableStateFlow(false)
    val showCourseDialog: StateFlow<Boolean> = _showCourseDialog.asStateFlow()

    private val _showThemeCabinetSheet = MutableStateFlow(false)
    val showThemeCabinetSheet: StateFlow<Boolean> = _showThemeCabinetSheet.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun openThemeCabinetSheet() {
        _showThemeCabinetSheet.value = true
    }

    fun dismissThemeCabinetSheet() {
        _showThemeCabinetSheet.value = false
    }

    fun selectTab(tab: MainTab) {
        _activeTab.value = tab
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun showMessage(msg: String) {
        _toastMessage.value = msg
    }

    fun setArtistTheme(theme: ArtistTheme) {
        repository.setArtistTheme(theme)
        ClassTrackWidgetProvider.refreshAllWidgets(getApplication())
        val name = if (theme == ArtistTheme.CUSTOM_JSON) {
            customThemeConfig.value.themeName
        } else {
            theme.displayName
        }
        _toastMessage.value = "已切换至主题：【$name】"
    }

    fun cycleArtistTheme() {
        val builtIn = ArtistTheme.builtInThemes
        val customs = customThemes.value
        if (artistTheme.value != ArtistTheme.CUSTOM_JSON) {
            val idx = builtIn.indexOf(artistTheme.value)
            if (idx in 0 until builtIn.lastIndex) {
                setArtistTheme(builtIn[idx + 1])
            } else if (customs.isNotEmpty()) {
                selectCustomTheme(customs.first().id)
            } else {
                setArtistTheme(builtIn.first())
            }
        } else {
            val curId = customThemeConfig.value.id
            val idx = customs.indexOfFirst { it.id == curId }
            if (idx in 0 until customs.lastIndex) {
                selectCustomTheme(customs[idx + 1].id)
            } else {
                setArtistTheme(builtIn.first())
            }
        }
    }

    fun selectCustomTheme(id: String) {
        repository.selectCustomTheme(id)
        ClassTrackWidgetProvider.refreshAllWidgets(getApplication())
        _toastMessage.value = "已切换至自定义主题：【${customThemeConfig.value.themeName}】"
    }

    fun deleteCustomTheme(id: String) {
        val targetName = customThemes.value.firstOrNull { it.id == id }?.themeName ?: "自定义主题"
        repository.deleteCustomTheme(id)
        ClassTrackWidgetProvider.refreshAllWidgets(getApplication())
        _toastMessage.value = "已删除主题：【$targetName】"
    }

    fun selectPortal(portalId: String) {
        repository.selectPortal(portalId)
    }

    fun importThemeFromJson(rawJson: String): Boolean {
        val parsed = CourseImporter.parseThemeJson(rawJson)
        return parsed.fold(
            onSuccess = { cfg ->
                val created = repository.addCustomTheme(cfg)
                ClassTrackWidgetProvider.refreshAllWidgets(getApplication())
                _toastMessage.value = "已新建并启用主题：【${created.themeName}】"
                true
            },
            onFailure = { e ->
                _toastMessage.value = "主题 JSON 解析失败：${e.message?.take(80)}"
                false
            }
        )
    }

    fun exportCurrentThemeJson(): String {
        return repository.exportCustomThemeJson()
    }

    fun buildDeepSeekThemePrompt(styleDescription: String = ""): String {
        return CourseImporter.buildDeepSeekThemePrompt(styleDescription)
    }

    fun buildDeepSeekScheduleJsonPrompt(): String {
        return CourseImporter.buildDeepSeekScheduleJsonPrompt()
    }

    fun nextLyricQuote() {
        repository.nextLyricQuote()
        ClassTrackWidgetProvider.refreshAllWidgets(getApplication())
    }

    fun selectDayOfWeek(day: Int) {
        repository.setSelectedDayOfWeek(day)
    }

    /**
     * 翻阅查看周次（仅改变正在浏览的周次 viewingWeek，保持实际本周 currentWeek 不变）
     */
    fun changeWeek(delta: Int) {
        val target = (_viewingWeek.value + delta).coerceIn(1, totalWeeks.value)
        _viewingWeek.value = target
    }

    fun selectViewingWeek(week: Int) {
        _viewingWeek.value = week.coerceIn(1, totalWeeks.value)
    }

    fun returnToCurrentWeek() {
        _viewingWeek.value = currentWeek.value
    }

    /**
     * 修改当前实际所在周次（设为「本周」）
     */
    fun setActualCurrentWeek(week: Int) {
        val coerced = week.coerceIn(1, totalWeeks.value)
        repository.setCurrentWeek(coerced)
        _viewingWeek.value = coerced
        ClassTrackWidgetProvider.refreshAllWidgets(getApplication())
        _toastMessage.value = "已将第 $coerced 周设为当前周"
    }

    fun setTotalWeeks(weeks: Int) {
        repository.setTotalWeeks(weeks)
        if (_viewingWeek.value > repository.totalWeeks.value) {
            _viewingWeek.value = repository.totalWeeks.value
        }
    }

    fun setMaxSections(count: Int) {
        repository.setMaxSections(count)
        _toastMessage.value = "每日课程总节数已设为 ${repository.maxSections.value} 节"
    }

    fun updateSingleTimeSlot(section: Int, startTime: String, endTime: String) {
        repository.updateSingleTimeSlot(section, startTime, endTime)
        _toastMessage.value = "已更新第 $section 节时间：$startTime - $endTime"
    }

    fun resetTimeSlotsToDefault() {
        repository.resetTimeSlotsToDefault()
        _toastMessage.value = "已恢复默认 12 节作息时间"
    }

    fun setScheduleName(name: String) {
        repository.setScheduleName(name)
    }

    fun openAddCourseDialog(defaultDay: Int = selectedDayOfWeek.value, defaultStartSection: Int = 1) {
        val maxSec = maxSections.value
        _editingCourse.value = CourseItem(
            id = 0L,
            name = "",
            room = "",
            teacher = "",
            dayOfWeek = defaultDay,
            startSection = defaultStartSection.coerceIn(1, maxSec),
            endSection = (defaultStartSection + 1).coerceIn(1, maxSec),
            startWeek = 1,
            endWeek = totalWeeks.value,
            trackColorIndex = (courses.value.size) % 6
        )
        _showCourseDialog.value = true
    }

    fun openEditCourseDialog(course: CourseItem) {
        _editingCourse.value = course
        _showCourseDialog.value = true
    }

    fun dismissCourseDialog() {
        _showCourseDialog.value = false
        _editingCourse.value = null
    }

    fun saveCourse(course: CourseItem) {
        if (course.name.isBlank()) {
            _toastMessage.value = "请输入课程名称"
            return
        }
        repository.saveCourse(
            course.copy(
                room = course.room.ifBlank { "待定教室" }
            )
        )
        dismissCourseDialog()
        ClassTrackWidgetProvider.refreshAllWidgets(getApplication())
        _toastMessage.value = "已保存《${course.name}》"
    }

    fun deleteCourse(courseId: Long) {
        repository.deleteCourse(courseId)
        dismissCourseDialog()
        ClassTrackWidgetProvider.refreshAllWidgets(getApplication())
        _toastMessage.value = "已删除该课程"
    }

    fun buildTracksForSelectedDay(): List<TodayTrackUiModel> {
        return repository.buildTracksForDay(
            dayOfWeek = selectedDayOfWeek.value,
            week = currentWeek.value
        )
    }

    fun importScheduleFromText(rawText: String): CourseImporter.ImportResult {
        // 如果检测到用户粘贴的是主题 JSON（含 backgroundHex 且不含 courses），自动应用为主题
        if (rawText.contains("\"backgroundHex\"") && !rawText.contains("\"courses\"")) {
            importThemeFromJson(rawText)
            return CourseImporter.ImportResult(true, "自定义主题 JSON", emptyList(), "已识别并应用自定义主题 JSON")
        }
        val result = CourseImporter.parseAuto(rawText)
        if (result.success && result.courses.isNotEmpty()) {
            val sanitized = CourseImporter.mergeAndSanitizeCourses(result.courses)
            repository.replaceCoursesFromImport(sanitized, result.backupMetadata)
            _viewingWeek.value = repository.currentWeek.value
            ClassTrackWidgetProvider.refreshAllWidgets(getApplication())
        }
        _toastMessage.value = result.message
        return result
    }

    fun exportScheduleBackupJson(): String {
        return CourseImporter.exportToJson(repository.createBackup())
    }

    fun exportScheduleShareToken(): String {
        return CourseImporter.exportToShareToken(repository.createBackup())
    }

    fun applyImportResult(result: CourseImporter.ImportResult) {
        if (result.success && result.courses.isNotEmpty()) {
            val sanitized = CourseImporter.mergeAndSanitizeCourses(result.courses)
            repository.replaceCoursesFromImport(sanitized, result.backupMetadata)
            _viewingWeek.value = repository.currentWeek.value
            ClassTrackWidgetProvider.refreshAllWidgets(getApplication())
        }
        _toastMessage.value = result.message
    }

    fun clearAllCourses() {
        repository.clearAllCourses()
        ClassTrackWidgetProvider.refreshAllWidgets(getApplication())
        _toastMessage.value = "已清空当前课表"
    }

    fun resetToSampleCourses() {
        clearAllCourses()
    }

    fun setFluidCloudEnabled(enabled: Boolean) {
        repository.setFluidCloudEnabled(enabled)
        if (!enabled) {
            ColorOsFluidCloudHelper.cancelFluidCloudNotification(getApplication())
        }
        com.classtrack.app.system.CourseAlarmScheduler.rescheduleAll(getApplication())
    }

    fun setWatchBridgeEnabled(enabled: Boolean) {
        repository.setWatchBridgeEnabled(enabled)
    }

    fun setLeadMinutes(minutes: Int) {
        repository.setLeadMinutes(minutes)
        com.classtrack.app.system.CourseAlarmScheduler.rescheduleAll(getApplication())
    }

    fun testWatchBridge(context: Context) {
        ColorOsFluidCloudHelper.sendWatchTestNotification(context)
        _toastMessage.value = "已向 OPPO Watch X2 发送即时测试通知与手腕振动"
    }

    fun refreshAutomaticAlarms(context: Context) {
        com.classtrack.app.system.CourseAlarmScheduler.rescheduleAll(context)
        _toastMessage.value = "已重新规划全自动课前提醒闹钟"
    }

    fun triggerFluidCloudLivePreview(
        context: Context,
        preferredOsFamily: LiveCapsuleOsFamily = LiveCapsuleOsFamily.AUTO
    ) {
        val todayTracks = buildTracksForSelectedDay()
        val targetTrack = todayTracks.firstOrNull { it.playState == TrackPlayState.NOW_PLAYING }
            ?: todayTracks.firstOrNull { it.playState == TrackPlayState.UPCOMING }
            ?: todayTracks.firstOrNull()
            ?: TodayTrackUiModel(
                trackNumber = 1,
                course = CourseItem(
                    name = "高等数学",
                    room = "二教 209",
                    teacher = "指导教师",
                    dayOfWeek = selectedDayOfWeek.value,
                    startSection = 1,
                    endSection = 2
                ),
                startTime = "08:00",
                endTime = "09:35",
                playState = TrackPlayState.UPCOMING,
                progressFraction = 0.35f,
                minutesUntilStartOrEnd = 15
            )

        try {
            val diag = ColorOsFluidCloudHelper.postFluidCloudTrackNotification(
                context = context,
                track = targetTrack.copy(
                    minutesUntilStartOrEnd = targetTrack.minutesUntilStartOrEnd.takeIf { it > 0 } ?: 12
                ),
                artistTheme = artistTheme.value,
                watchBridgeEnabled = watchBridgeEnabled.value,
                preferredOsFamily = preferredOsFamily
            )
            _toastMessage.value = if (diag.canPostPromoted) {
                diag.statusSummary
            } else {
                "请先在系统通知设置中开启实况胶囊 / 流体云权限"
            }
        } catch (e: Exception) {
            _toastMessage.value = "实况已唤起: ${targetTrack.course.name}"
        }
    }
}
