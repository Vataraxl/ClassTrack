package com.classtrack.app.data

import android.content.Context
import android.content.SharedPreferences
import com.classtrack.app.data.model.ArtistTheme
import com.classtrack.app.data.model.CourseItem
import com.classtrack.app.data.model.CustomThemeConfig
import com.classtrack.app.data.model.DefaultPresets
import com.classtrack.app.data.model.LyricQuote
import com.classtrack.app.data.model.ScheduleBackup
import com.classtrack.app.data.model.TimeSlot
import com.classtrack.app.data.model.TodayTrackUiModel
import com.classtrack.app.data.model.TrackPlayState
import com.classtrack.app.data.model.UiStyle
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * 《课轨》(ClassTrack) 核心数据仓库：
 * 管理本地 SQLite 课程数据、自定义节数与每节课起止时间、三大内置主题 + 自定义 JSON 主题配置
 */
class DataRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dbHelper = ClassTrackDatabaseHelper(appContext)
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("classtrack_prefs", Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    private val _customThemes = MutableStateFlow(loadSavedCustomThemes())
    val customThemes: StateFlow<List<CustomThemeConfig>> = _customThemes.asStateFlow()

    private val _customThemeConfig = MutableStateFlow(
        _customThemes.value.firstOrNull { it.id == prefs.getString(KEY_ACTIVE_CUSTOM_THEME_ID, null) }
            ?: _customThemes.value.firstOrNull()
            ?: CustomThemeConfig(id = SAMPLE_CUSTOM_THEME_ID)
    )
    val customThemeConfig: StateFlow<CustomThemeConfig> = _customThemeConfig.asStateFlow()

    // 导课页当前选中的高校（只主展示一个）与最近使用的高校记录
    private val _selectedPortalId = MutableStateFlow(prefs.getString(KEY_SELECTED_PORTAL_ID, "") ?: "")
    val selectedPortalId: StateFlow<String> = _selectedPortalId.asStateFlow()

    private val _recentPortalIds = MutableStateFlow(
        (prefs.getString(KEY_RECENT_PORTAL_IDS, "") ?: "").split(",").filter { it.isNotBlank() }
    )
    val recentPortalIds: StateFlow<List<String>> = _recentPortalIds.asStateFlow()

    private val _artistTheme = MutableStateFlow(loadSavedTheme())
    val artistTheme: StateFlow<ArtistTheme> = _artistTheme.asStateFlow()

    private val _scheduleName = MutableStateFlow(
        prefs.getString(KEY_SCHEDULE_NAME, "我的学期课表") ?: "我的学期课表"
    )
    val scheduleName: StateFlow<String> = _scheduleName.asStateFlow()

    private val _currentWeek = MutableStateFlow(prefs.getInt(KEY_CURRENT_WEEK, 8).coerceIn(1, 24))
    val currentWeek: StateFlow<Int> = _currentWeek.asStateFlow()

    private val _totalWeeks = MutableStateFlow(prefs.getInt(KEY_TOTAL_WEEKS, 18).coerceIn(1, 24))
    val totalWeeks: StateFlow<Int> = _totalWeeks.asStateFlow()

    private val _maxSections = MutableStateFlow(prefs.getInt(KEY_MAX_SECTIONS, 12).coerceIn(4, 18))
    val maxSections: StateFlow<Int> = _maxSections.asStateFlow()

    private val _selectedDayOfWeek = MutableStateFlow(LocalDate.now().dayOfWeek.value)
    val selectedDayOfWeek: StateFlow<Int> = _selectedDayOfWeek.asStateFlow()

    private val _courses = MutableStateFlow(loadAndHealCourses())
    val courses: StateFlow<List<CourseItem>> = _courses.asStateFlow()

    /**
     * 启动时加载课程并自动修复遗留脏数据：
     * 1. 旧版周次正则误判遗留的跨度异常脏数据（如 02-12 节、08-12 节）；
     * 2. 旧版强智 div.kbcontent id 末段 "-2" 误判导致全表所有课程都被挤在 03-04 节且同天多课互相覆盖的脏数据。
     */
    private fun loadAndHealCourses(): List<CourseItem> {
        val raw = dbHelper.getAllCourses()
        val hasCorruptedSpan = raw.any { (it.endSection - it.startSection + 1) >= 5 && it.endSection >= 10 }
        val allStuckInSection3To4 = raw.size >= 4 &&
            raw.all { it.startSection == 3 && it.endSection == 4 } &&
            raw.groupBy { it.dayOfWeek }.any { (_, dayCourses) -> dayCourses.size >= 2 }

        if (!hasCorruptedSpan && !allStuckInSection3To4) {
            return raw
        }

        val slotStarts = intArrayOf(1, 3, 5, 7, 9, 11)
        val unstacked = if (allStuckInSection3To4) {
            raw.groupBy { it.dayOfWeek }.flatMap { (_, dayCourses) ->
                dayCourses.sortedBy { it.id }.mapIndexed { idx, course ->
                    val start = slotStarts.getOrElse(idx) { (idx * 2 + 1).coerceAtMost(11) }
                    course.copy(startSection = start, endSection = start + 1)
                }
            }
        } else {
            raw
        }

        val healed = CourseImporter.mergeAndSanitizeCourses(unstacked)
        dbHelper.replaceAllCourses(healed)
        return dbHelper.getAllCourses()
    }

    private val _timeSlots = MutableStateFlow(loadSavedTimeSlots(_maxSections.value))
    val timeSlots: StateFlow<List<TimeSlot>> = _timeSlots.asStateFlow()

    private val _fluidCloudEnabled = MutableStateFlow(prefs.getBoolean(KEY_FLUID_CLOUD, true))
    val fluidCloudEnabled: StateFlow<Boolean> = _fluidCloudEnabled.asStateFlow()

    private val _watchBridgeEnabled = MutableStateFlow(prefs.getBoolean(KEY_WATCH_BRIDGE, true))
    val watchBridgeEnabled: StateFlow<Boolean> = _watchBridgeEnabled.asStateFlow()

    private val _leadMinutes = MutableStateFlow(prefs.getInt(KEY_LEAD_MINUTES, 20))
    val leadMinutes: StateFlow<Int> = _leadMinutes.asStateFlow()

    private val _uiStyle = MutableStateFlow(loadSavedUiStyle())
    val uiStyle: StateFlow<UiStyle> = _uiStyle.asStateFlow()

    private fun loadSavedUiStyle(): UiStyle {
        val raw = prefs.getString(KEY_UI_STYLE, UiStyle.CLASSIC.name) ?: UiStyle.CLASSIC.name
        return runCatching { UiStyle.valueOf(raw) }.getOrDefault(UiStyle.CLASSIC)
    }

    fun setUiStyle(style: UiStyle) {
        _uiStyle.value = style
        prefs.edit().putString(KEY_UI_STYLE, style.name).apply()
    }

    private val _lyricIndex = MutableStateFlow(0)
    private val _currentLyric = MutableStateFlow(pickLyricForTheme(_artistTheme.value, 0))
    val currentLyric: StateFlow<LyricQuote> = _currentLyric.asStateFlow()

    private fun loadSavedTheme(): ArtistTheme {
        val raw = prefs.getString(KEY_THEME, ArtistTheme.ZTMY.name) ?: ArtistTheme.ZTMY.name
        return runCatching { ArtistTheme.valueOf(raw) }.getOrDefault(ArtistTheme.ZTMY)
    }

    /**
     * 读取已保存的全部自定义主题；兼容旧版本仅保存单个主题的数据
     */
    private fun loadSavedCustomThemes(): List<CustomThemeConfig> {
        val rawList = prefs.getString(KEY_CUSTOM_THEMES_LIST_JSON, null)
        if (rawList != null) {
            val parsed = runCatching {
                json.decodeFromString(ListSerializer(CustomThemeConfig.serializer()), rawList)
            }.getOrNull()
            if (parsed != null) return parsed
        }
        val legacy = prefs.getString(KEY_CUSTOM_THEME_JSON, null)?.let { raw ->
            runCatching { json.decodeFromString(CustomThemeConfig.serializer(), raw) }.getOrNull()
        }
        val sample = CustomThemeConfig(id = SAMPLE_CUSTOM_THEME_ID)
        return if (legacy != null) {
            listOf(sample, legacy.copy(id = legacy.id.ifBlank { newCustomThemeId() }))
                .distinctBy { it.copy(id = "") }
        } else {
            listOf(sample)
        }
    }

    private fun newCustomThemeId(): String = "custom_${System.currentTimeMillis()}"

    private fun persistCustomThemes(list: List<CustomThemeConfig>) {
        _customThemes.value = list
        prefs.edit()
            .putString(
                KEY_CUSTOM_THEMES_LIST_JSON,
                json.encodeToString(ListSerializer(CustomThemeConfig.serializer()), list)
            )
            .apply()
    }

    private fun loadSavedTimeSlots(targetCount: Int): List<TimeSlot> {
        val raw = prefs.getString(KEY_TIME_SLOTS_JSON, null)
        val baseList = if (raw != null) {
            runCatching {
                json.decodeFromString(ListSerializer(TimeSlot.serializer()), raw)
            }.getOrDefault(DefaultPresets.defaultTimeSlots)
        } else {
            DefaultPresets.defaultTimeSlots
        }
        return normalizeTimeSlots(baseList, targetCount)
    }

    private fun normalizeTimeSlots(existing: List<TimeSlot>, targetCount: Int): List<TimeSlot> {
        val map = existing.associateBy { it.section }
        val defaultsMap = DefaultPresets.defaultTimeSlots.associateBy { it.section }
        return (1..targetCount).map { sec ->
            map[sec] ?: defaultsMap[sec] ?: run {
                val baseHour = (8 + sec - 1).coerceIn(6, 22)
                TimeSlot(
                    section = sec,
                    startTime = "%02d:00".format(baseHour),
                    endTime = "%02d:45".format(baseHour)
                )
            }
        }
    }

    private fun pickLyricForTheme(theme: ArtistTheme, indexOffset: Int): LyricQuote {
        if (theme == ArtistTheme.CUSTOM_JSON) {
            val customList = _customThemeConfig.value.customLyrics
            if (customList.isNotEmpty()) {
                val item = customList[indexOffset.mod(customList.size)]
                return LyricQuote(
                    id = 900 + indexOffset,
                    artistTheme = ArtistTheme.CUSTOM_JSON,
                    songTitle = item.songTitle,
                    japaneseLyric = item.japaneseLyric,
                    chineseTranslation = item.chineseTranslation,
                    moodTag = _customThemeConfig.value.themeName
                )
            }
        }
        val list = DefaultPresets.defaultLyrics.filter { it.artistTheme == theme }
            .ifEmpty { DefaultPresets.defaultLyrics }
        return list[indexOffset.mod(list.size)]
    }

    fun setArtistTheme(theme: ArtistTheme) {
        _artistTheme.value = theme
        prefs.edit().putString(KEY_THEME, theme.name).apply()
        _currentLyric.value = pickLyricForTheme(theme, _lyricIndex.value)
    }

    /**
     * 新建一个自定义主题（永不覆盖已有主题），并立即启用。
     * 若内容与已有主题完全相同则直接切换到该主题，避免重复。
     * @return 实际保存/启用的主题
     */
    fun addCustomTheme(config: CustomThemeConfig): CustomThemeConfig {
        val existing = _customThemes.value
        val duplicate = existing.firstOrNull { it.copy(id = "") == config.copy(id = "") }
        if (duplicate != null) {
            selectCustomTheme(duplicate.id)
            return duplicate
        }
        // 主题重名时自动追加序号，便于在列表中区分
        val baseName = config.themeName.ifBlank { "我的主题" }
        var finalName = baseName
        var suffix = 2
        while (existing.any { it.themeName == finalName }) {
            finalName = "$baseName ($suffix)"
            suffix++
        }
        val created = config.copy(id = newCustomThemeId(), themeName = finalName)
        persistCustomThemes(existing + created)
        selectCustomTheme(created.id)
        return created
    }

    fun selectCustomTheme(id: String) {
        val target = _customThemes.value.firstOrNull { it.id == id } ?: return
        _customThemeConfig.value = target
        prefs.edit()
            .putString(KEY_ACTIVE_CUSTOM_THEME_ID, target.id)
            .putString(KEY_THEME, ArtistTheme.CUSTOM_JSON.name)
            .apply()
        _artistTheme.value = ArtistTheme.CUSTOM_JSON
        _currentLyric.value = pickLyricForTheme(ArtistTheme.CUSTOM_JSON, _lyricIndex.value)
    }

    /**
     * 删除自定义主题；若删除的是当前正在使用的主题，则自动回退
     */
    fun deleteCustomTheme(id: String) {
        val remaining = _customThemes.value.filterNot { it.id == id }
        persistCustomThemes(remaining)
        if (_customThemeConfig.value.id == id) {
            val fallback = remaining.firstOrNull()
            if (fallback != null) {
                _customThemeConfig.value = fallback
                prefs.edit().putString(KEY_ACTIVE_CUSTOM_THEME_ID, fallback.id).apply()
            }
            if (_artistTheme.value == ArtistTheme.CUSTOM_JSON) {
                setArtistTheme(if (fallback != null) ArtistTheme.CUSTOM_JSON else ArtistTheme.ZTMY)
            }
        }
    }

    /**
     * 兼容旧调用：导入主题时一律走「新建」逻辑
     */
    fun applyCustomThemeConfig(config: CustomThemeConfig) {
        addCustomTheme(config)
    }

    fun selectPortal(portalId: String) {
        _selectedPortalId.value = portalId
        val recents = (listOf(portalId) + _recentPortalIds.value.filterNot { it == portalId })
            .filter { it.isNotBlank() }
            .take(6)
        _recentPortalIds.value = recents
        prefs.edit()
            .putString(KEY_SELECTED_PORTAL_ID, portalId)
            .putString(KEY_RECENT_PORTAL_IDS, recents.joinToString(","))
            .apply()
    }

    fun exportCustomThemeJson(): String {
        return json.encodeToString(CustomThemeConfig.serializer(), _customThemeConfig.value)
    }

    fun nextLyricQuote() {
        val nextIdx = _lyricIndex.value + 1
        _lyricIndex.value = nextIdx
        _currentLyric.value = pickLyricForTheme(_artistTheme.value, nextIdx)
    }

    fun setCurrentWeek(week: Int) {
        val coerced = week.coerceIn(1, _totalWeeks.value)
        _currentWeek.value = coerced
        prefs.edit().putInt(KEY_CURRENT_WEEK, coerced).apply()
    }

    fun setTotalWeeks(weeks: Int) {
        val coerced = weeks.coerceIn(1, 30)
        _totalWeeks.value = coerced
        prefs.edit().putInt(KEY_TOTAL_WEEKS, coerced).apply()
        if (_currentWeek.value > coerced) {
            setCurrentWeek(coerced)
        }
    }

    /**
     * 设置每日课程总节数（支持 4 ~ 18 节动态增减）
     */
    fun setMaxSections(count: Int) {
        val coerced = count.coerceIn(4, 18)
        _maxSections.value = coerced
        val updatedSlots = normalizeTimeSlots(_timeSlots.value, coerced)
        _timeSlots.value = updatedSlots
        val serialized = json.encodeToString(ListSerializer(TimeSlot.serializer()), updatedSlots)
        prefs.edit()
            .putInt(KEY_MAX_SECTIONS, coerced)
            .putString(KEY_TIME_SLOTS_JSON, serialized)
            .apply()
    }

    /**
     * 更新某一节课的开始时间与结束时间
     */
    fun updateSingleTimeSlot(section: Int, startTime: String, endTime: String) {
        val updated = _timeSlots.value.map { slot ->
            if (slot.section == section) {
                slot.copy(
                    startTime = startTime.trim().ifBlank { slot.startTime },
                    endTime = endTime.trim().ifBlank { slot.endTime }
                )
            } else {
                slot
            }
        }
        _timeSlots.value = updated
        val serialized = json.encodeToString(ListSerializer(TimeSlot.serializer()), updated)
        prefs.edit().putString(KEY_TIME_SLOTS_JSON, serialized).apply()
    }

    /**
     * 批量更新全部作息时间段（例如从课表 JSON 导入时）
     */
    fun updateAllTimeSlots(slots: List<TimeSlot>, newMaxSections: Int? = null) {
        val targetMax = (newMaxSections ?: slots.maxOfOrNull { it.section } ?: _maxSections.value)
            .coerceIn(4, 18)
        _maxSections.value = targetMax
        val normalized = normalizeTimeSlots(slots, targetMax)
        _timeSlots.value = normalized
        val serialized = json.encodeToString(ListSerializer(TimeSlot.serializer()), normalized)
        prefs.edit()
            .putInt(KEY_MAX_SECTIONS, targetMax)
            .putString(KEY_TIME_SLOTS_JSON, serialized)
            .apply()
    }

    fun resetTimeSlotsToDefault() {
        _maxSections.value = 12
        _timeSlots.value = DefaultPresets.defaultTimeSlots
        prefs.edit()
            .putInt(KEY_MAX_SECTIONS, 12)
            .remove(KEY_TIME_SLOTS_JSON)
            .apply()
    }

    fun setScheduleName(name: String) {
        if (name.isNotBlank()) {
            _scheduleName.value = name.trim()
            prefs.edit().putString(KEY_SCHEDULE_NAME, name.trim()).apply()
        }
    }

    fun setSelectedDayOfWeek(day: Int) {
        _selectedDayOfWeek.value = day.coerceIn(1, 7)
    }

    fun setFluidCloudEnabled(enabled: Boolean) {
        _fluidCloudEnabled.value = enabled
        prefs.edit().putBoolean(KEY_FLUID_CLOUD, enabled).apply()
    }

    fun setWatchBridgeEnabled(enabled: Boolean) {
        _watchBridgeEnabled.value = enabled
        prefs.edit().putBoolean(KEY_WATCH_BRIDGE, enabled).apply()
    }

    fun setLeadMinutes(minutes: Int) {
        _leadMinutes.value = minutes.coerceIn(5, 60)
        prefs.edit().putInt(KEY_LEAD_MINUTES, _leadMinutes.value).apply()
    }

    fun saveCourse(course: CourseItem) {
        dbHelper.upsertCourse(course)
        _courses.value = dbHelper.getAllCourses()
    }

    fun deleteCourse(courseId: Long) {
        dbHelper.deleteCourse(courseId)
        _courses.value = dbHelper.getAllCourses()
    }

    fun clearAllCourses() {
        dbHelper.clearAllCourses()
        _courses.value = emptyList()
    }

    fun resetToSampleCourses() {
        clearAllCourses()
    }

    fun replaceCoursesFromImport(imported: List<CourseItem>, backupMeta: ScheduleBackup? = null) {
        dbHelper.replaceAllCourses(imported)
        _courses.value = dbHelper.getAllCourses()
        if (backupMeta != null) {
            if (backupMeta.scheduleName.isNotBlank()) setScheduleName(backupMeta.scheduleName)
            if (backupMeta.totalWeeks > 0) setTotalWeeks(backupMeta.totalWeeks)
            if (backupMeta.currentWeek > 0) setCurrentWeek(backupMeta.currentWeek)
            if (backupMeta.timeSlots.isNotEmpty() || backupMeta.maxSections > 0) {
                updateAllTimeSlots(
                    slots = backupMeta.timeSlots.ifEmpty { _timeSlots.value },
                    newMaxSections = backupMeta.maxSections.takeIf { it > 0 }
                )
            }
            if (backupMeta.customTheme != null) {
                applyCustomThemeConfig(backupMeta.customTheme)
            } else {
                setArtistTheme(backupMeta.artistTheme)
            }
        }
    }

    fun createBackup(): ScheduleBackup {
        return ScheduleBackup(
            scheduleName = _scheduleName.value,
            currentWeek = _currentWeek.value,
            totalWeeks = _totalWeeks.value,
            maxSections = _maxSections.value,
            artistTheme = _artistTheme.value,
            courses = _courses.value,
            timeSlots = _timeSlots.value,
            customTheme = if (_artistTheme.value == ArtistTheme.CUSTOM_JSON) _customThemeConfig.value else null
        )
    }

    fun buildTracksForDay(
        dayOfWeek: Int,
        week: Int,
        nowTime: LocalTime = LocalTime.now()
    ): List<TodayTrackUiModel> {
        val slotsMap = _timeSlots.value.associateBy { it.section }
        val activeCourses = _courses.value
            .filter { it.dayOfWeek == dayOfWeek && it.isActiveInWeek(week) }
            .sortedBy { it.startSection }

        val nowMinutes = nowTime.hour * 60 + nowTime.minute

        return activeCourses.mapIndexed { idx, course ->
            val startStr = slotsMap[course.startSection]?.startTime ?: "08:00"
            val endStr = slotsMap[course.endSection]?.endTime ?: "09:35"

            val startMin = parseMinutes(startStr)
            val endMin = parseMinutes(endStr).coerceAtLeast(startMin + 45)

            val playState: TrackPlayState
            val progress: Float
            val remainingMin: Int

            when {
                nowMinutes < startMin -> {
                    playState = TrackPlayState.UPCOMING
                    progress = 0f
                    remainingMin = startMin - nowMinutes
                }
                nowMinutes in startMin..endMin -> {
                    playState = TrackPlayState.NOW_PLAYING
                    val total = (endMin - startMin).coerceAtLeast(1)
                    progress = ((nowMinutes - startMin).toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    remainingMin = (endMin - nowMinutes).coerceAtLeast(0)
                }
                else -> {
                    playState = TrackPlayState.COMPLETED
                    progress = 1f
                    remainingMin = 0
                }
            }

            TodayTrackUiModel(
                trackNumber = idx + 1,
                course = course,
                startTime = startStr,
                endTime = endStr,
                playState = playState,
                progressFraction = progress,
                minutesUntilStartOrEnd = remainingMin
            )
        }
    }

    private fun parseMinutes(hhmm: String): Int {
        val parts = hhmm.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }

    companion object {
        private const val KEY_THEME = "key_artist_theme"
        private const val KEY_CUSTOM_THEME_JSON = "key_custom_theme_json"
        private const val KEY_CUSTOM_THEMES_LIST_JSON = "key_custom_themes_list_json"
        private const val KEY_ACTIVE_CUSTOM_THEME_ID = "key_active_custom_theme_id"
        private const val KEY_SELECTED_PORTAL_ID = "key_selected_portal_id"
        private const val KEY_RECENT_PORTAL_IDS = "key_recent_portal_ids"
        const val SAMPLE_CUSTOM_THEME_ID = "sample_gunjou"
        private const val KEY_SCHEDULE_NAME = "key_schedule_name"
        private const val KEY_CURRENT_WEEK = "key_current_week"
        private const val KEY_TOTAL_WEEKS = "key_total_weeks"
        private const val KEY_MAX_SECTIONS = "key_max_sections"
        private const val KEY_TIME_SLOTS_JSON = "key_time_slots_json"
        private const val KEY_FLUID_CLOUD = "key_fluid_cloud_enabled"
        private const val KEY_WATCH_BRIDGE = "key_watch_bridge_enabled"
        private const val KEY_LEAD_MINUTES = "key_lead_minutes"
        private const val KEY_UI_STYLE = "key_ui_style"
    }
}
