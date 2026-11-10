package com.classtrack.app.data

import android.util.Base64
import com.classtrack.app.data.model.CourseItem
import com.classtrack.app.data.model.CustomThemeConfig
import com.classtrack.app.data.model.ScheduleBackup
import com.classtrack.app.data.model.WeekParity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 多渠道课表导入解析器：
 * 支持：
 * 1. 剪贴板「课轨口令」压缩分享码 (【课轨Track】#...#)
 * 2. 《课轨》JSON 备份格式
 * 3. WakeUp 课表导出文件（5 行分段式 JSON）
 * 4. CSV / Excel 通用表格格式
 * 5. 高校教务系统 WebView 提取结果 & AI 课表截图识别结果
 */
object CourseImporter {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    private val compactJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }

    data class ImportResult(
        val success: Boolean,
        val sourceType: String,
        val courses: List<CourseItem>,
        val message: String,
        val backupMetadata: ScheduleBackup? = null
    )

    @Serializable
    data class WebExtractedCourse(
        val name: String,
        val room: String = "",
        val teacher: String = "",
        val dayOfWeek: Int = 1,
        val startSection: Int = 1,
        val endSection: Int = 2,
        val startWeek: Int = 1,
        val endWeek: Int = 18,
        val weeksSpec: String = "",
        val parity: String = "ALL"
    )

    fun exportToJson(backup: ScheduleBackup): String {
        return json.encodeToString(ScheduleBackup.serializer(), backup)
    }

    /**
     * 主题解析专用：允许 AI 输出 null / 缺字段时回退为默认值
     */
    private val themeJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val hexColorRegex = Regex("^#[0-9A-Fa-f]{6}$")

    /**
     * 解析主题 JSON 文件内容（自动剥离 DeepSeek 等 AI 回复可能带有的 ```json 代码块包裹），
     * 并对颜色、数量等字段做合法性校正
     */
    fun parseThemeJson(rawText: String): Result<CustomThemeConfig> = runCatching {
        val cleaned = stripMarkdownCodeBlock(rawText)
        val jsonStart = cleaned.indexOf('{')
        val jsonEnd = cleaned.lastIndexOf('}')
        val payload = if (jsonStart >= 0 && jsonEnd > jsonStart) {
            cleaned.substring(jsonStart, jsonEnd + 1)
        } else {
            cleaned
        }
        sanitizeTheme(themeJson.decodeFromString(CustomThemeConfig.serializer(), payload))
    }

    /**
     * 校正 AI 生成的主题：非法颜色回退默认值、配色列表补齐为 6 个、圆角限制范围、字体限定可选值
     */
    private fun sanitizeTheme(cfg: CustomThemeConfig): CustomThemeConfig {
        val def = CustomThemeConfig()
        fun color(value: String, fallback: String): String {
            val v = value.trim().uppercase().let { if (it.length == 6 && !it.startsWith("#")) "#$it" else it }
            return if (hexColorRegex.matches(v)) v else fallback
        }
        fun colorList(values: List<String>, fallback: List<String>): List<String> =
            (0 until 6).map { i -> color(values.getOrElse(i) { "" }, fallback[i]) }

        return cfg.copy(
            id = "",
            themeName = cfg.themeName.trim().ifBlank { "我的主题" }.take(16),
            artistName = cfg.artistName.trim().take(24),
            subtitle = cfg.subtitle.trim().take(40),
            fontStyle = cfg.fontStyle.uppercase().takeIf { it in setOf("SANS", "SERIF", "MONO") } ?: "SANS",
            cardCornerDp = cfg.cardCornerDp.coerceIn(0, 28),
            backgroundHex = color(cfg.backgroundHex, def.backgroundHex),
            surfaceHex = color(cfg.surfaceHex, def.surfaceHex),
            surfaceVariantHex = color(cfg.surfaceVariantHex, def.surfaceVariantHex),
            primaryHex = color(cfg.primaryHex, def.primaryHex),
            secondaryHex = color(cfg.secondaryHex, def.secondaryHex),
            tertiaryHex = color(cfg.tertiaryHex, def.tertiaryHex),
            onBackgroundHex = color(cfg.onBackgroundHex, def.onBackgroundHex),
            onSurfaceHex = color(cfg.onSurfaceHex, def.onSurfaceHex),
            mutedTextHex = color(cfg.mutedTextHex, def.mutedTextHex),
            borderHex = color(cfg.borderHex, def.borderHex),
            trackBackgroundHexList = colorList(cfg.trackBackgroundHexList, def.trackBackgroundHexList),
            trackAccentHexList = colorList(cfg.trackAccentHexList, def.trackAccentHexList),
            customLyrics = cfg.customLyrics
                .filter { it.songTitle.isNotBlank() && it.japaneseLyric.isNotBlank() }
                .take(8)
                .ifEmpty { def.customLyrics }
        )
    }

    /**
     * 生成「空值主题模板」JSON：所有字段保留、值留空，供 AI 逐项填写
     */
    fun buildEmptyThemeTemplateJson(): String = """
{
  "themeName": "",
  "artistName": "",
  "subtitle": "",
  "isDark": null,
  "fontStyle": "",
  "cardCornerDp": null,
  "backgroundHex": "",
  "surfaceHex": "",
  "surfaceVariantHex": "",
  "primaryHex": "",
  "secondaryHex": "",
  "tertiaryHex": "",
  "onBackgroundHex": "",
  "onSurfaceHex": "",
  "mutedTextHex": "",
  "borderHex": "",
  "trackBackgroundHexList": ["", "", "", "", "", ""],
  "trackAccentHexList": ["", "", "", "", "", ""],
  "customLyrics": [
    {
      "songTitle": "",
      "japaneseLyric": "",
      "chineseTranslation": ""
    },
    {
      "songTitle": "",
      "japaneseLyric": "",
      "chineseTranslation": ""
    },
    {
      "songTitle": "",
      "japaneseLyric": "",
      "chineseTranslation": ""
    }
  ]
}
    """.trimIndent()

    /**
     * 生成供用户发给 DeepSeek / 豆包 / Kimi 等 AI 的「新建主题」提示词：
     * 逐项规定每个字段的含义、类型与取值范围，并附带空值模板
     * @param styleDescription 用户想要的风格描述（可为空，为空时让 AI 先询问）
     */
    fun buildDeepSeekThemePrompt(styleDescription: String = ""): String {
        val style = styleDescription.trim().ifBlank { "（我还没想好，请你先推荐 3 个风格方向让我选，我选定后再输出 JSON）" }
        return """
你是一名 UI 配色设计师。请为安卓课表 App《课轨》设计一套【全新】的自定义主题，以 JSON 文件形式输出。

【我想要的风格】$style

【输出要求】
1. 只输出一个 ```json 代码块，代码块外不要有任何解释文字。
2. 必须包含下表中的全部字段，字段名大小写完全一致，不得增删字段。
3. 我已附上一个「空值模板 JSON」文件（如果附件没有成功发送，模板也贴在本消息末尾），请把其中每一个空值都填好。

【字段规范】（类型 | 取值要求 | 用途）
- themeName：字符串 | 2~8 个字 | 主题名称，显示在主题列表和顶栏
- artistName：字符串 | 不超过 20 字 | 灵感来源的歌手/乐队/作品名
- subtitle：字符串 | 不超过 24 字 | 一句话描述主题氛围
- isDark：布尔值 true 或 false（不加引号）| 深色主题填 true，浅色主题填 false
- fontStyle：字符串 | 只能是 "SANS"（无衬线）、"SERIF"（衬线/书卷感）、"MONO"（等宽/科技感）三者之一
- cardCornerDp：整数（不加引号）| 0~28 | 课程卡片圆角大小，越大越圆润，推荐 6~16
- backgroundHex：颜色 | App 整体背景色
- surfaceHex：颜色 | 卡片、顶栏、底栏的底色，应与背景色有轻微区分
- surfaceVariantHex：颜色 | 选中态/次级卡片底色，比 surfaceHex 稍亮（深色）或稍暗（浅色）
- primaryHex：颜色 | 主强调色：按钮、「本周」高亮、「今日」列，需在背景上非常醒目
- secondaryHex：颜色 | 副强调色：用于「非本周」提醒等警示信息，应与 primaryHex 明显不同
- tertiaryHex：颜色 | 第三点缀色，用于装饰
- onBackgroundHex：颜色 | 背景上的主要文字颜色，与 backgroundHex 对比度 ≥ 7:1
- onSurfaceHex：颜色 | 卡片上的主要文字颜色，与 surfaceHex 对比度 ≥ 7:1
- mutedTextHex：颜色 | 次要说明文字颜色，与 surfaceHex 对比度 ≥ 4.5:1
- borderHex：颜色 | 卡片描边和分割线颜色，低调不抢眼
- trackBackgroundHexList：恰好 6 个颜色的数组 | 6 种课程卡片的底色，彼此色相区分明显
- trackAccentHexList：恰好 6 个颜色的数组 | 与上面一一对应的课程卡片文字/描边色，与对应底色对比度 ≥ 4.5:1
- customLyrics：3~6 个对象的数组 | 底部歌词条轮播内容，每个对象包含：
    - songTitle：歌曲名（字符串，必填）
    - japaneseLyric：一句原文歌词（字符串，必填，不超过 40 字，可以是任何语言）
    - chineseTranslation：对应中文翻译（字符串，原文是中文时可填空字符串 ""）

【颜色格式】所有颜色必须是 "#RRGGBB" 形式的 6 位十六进制字符串（例如 "#1A2B3C"），不要带透明度、不要用颜色英文名或 rgb()。

【空值模板】
${buildEmptyThemeTemplateJson()}
        """.trimIndent()
    }

    /**
     * 生成供用户发给 DeepSeek 等免费 AI 的「课表 JSON 转换提示词 + 模板」
     */
    fun buildDeepSeekScheduleJsonPrompt(): String {
        return """
            请帮我把下面的课表内容整理为《课轨 (ClassTrack)》App 支持的标准课表 JSON 格式（只输出纯 JSON，不要多余解释）：
            {
              "scheduleName": "2026-2027 学期课表",
              "currentWeek": 8,
              "totalWeeks": 18,
              "maxSections": 12,
              "timeSlots": [
                {"section": 1, "startTime": "08:00", "endTime": "08:45"},
                {"section": 2, "startTime": "08:50", "endTime": "09:35"}
              ],
              "courses": [
                {
                  "name": "高等数学",
                  "room": "二教209",
                  "teacher": "孙颖",
                  "dayOfWeek": 1,
                  "startSection": 3,
                  "endSection": 5,
                  "startWeek": 1,
                  "endWeek": 18,
                  "weeksSpec": "2,6,8-9,12-18",
                  "trackColorIndex": 0,
                  "note": ""
                }
              ]
            }
            ---
            待整理的课表原始信息如下：
            【请在此粘贴你从教务网复制的文字】
        """.trimIndent()
    }

    private fun stripMarkdownCodeBlock(input: String): String {
        var s = input.trim().removePrefix("\uFEFF").trim()
        if (s.startsWith("```")) {
            s = s.removePrefix("```json").removePrefix("```JSON").removePrefix("```").trim()
        }
        if (s.endsWith("```")) {
            s = s.removeSuffix("```").trim()
        }
        return s
    }

    /**
     * 生成可直接在微信/QQ分享的压缩「课轨口令」
     */
    fun exportToShareToken(backup: ScheduleBackup): String {
        val rawJson = compactJson.encodeToString(ScheduleBackup.serializer(), backup)
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { gzip ->
            gzip.write(rawJson.toByteArray(Charsets.UTF_8))
        }
        val b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
        return "复制整段口令打开《课轨》即可导入专辑《${backup.scheduleName}》：【课轨Track】#$b64#"
    }

    private fun tryDecodeShareToken(text: String): ScheduleBackup? {
        val regex = Regex("#([A-Za-z0-9+/=_\\-]+)#")
        val match = regex.find(text) ?: return null
        val payload = match.groupValues[1]
        return runCatching {
            val compressed = Base64.decode(payload, Base64.DEFAULT)
            val decompressed = GZIPInputStream(ByteArrayInputStream(compressed))
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            compactJson.decodeFromString(ScheduleBackup.serializer(), decompressed)
        }.getOrNull()
    }

    /**
     * 智能识别输入文本格式并解析为课程列表
     */
    fun parseAuto(rawInput: String): ImportResult {
        val trimmed = stripMarkdownCodeBlock(rawInput)
        if (trimmed.isEmpty()) {
            return ImportResult(false, "空内容", emptyList(), "导入内容为空，请粘贴或选择有效课表数据。")
        }

        // 1. 尝试解析「课轨口令」压缩分享码 (【课轨Track】#...#)
        if (trimmed.contains("【课轨Track】") || trimmed.count { it == '#' } >= 2) {
            val tokenBackup = tryDecodeShareToken(trimmed)
            if (tokenBackup != null && tokenBackup.courses.isNotEmpty()) {
                return ImportResult(
                    success = true,
                    sourceType = "课轨口令分享码",
                    courses = tokenBackup.courses,
                    message = "成功通过分享口令导入《${tokenBackup.scheduleName}》共 ${tokenBackup.courses.size} 门课程",
                    backupMetadata = tokenBackup
                )
            }
        }

        // 2. 尝试解析《课轨》原生 ScheduleBackup JSON
        if (trimmed.startsWith("{") && trimmed.contains("\"courses\"")) {
            runCatching {
                val backup = json.decodeFromString(ScheduleBackup.serializer(), trimmed)
                if (backup.courses.isNotEmpty()) {
                    return ImportResult(
                        success = true,
                        sourceType = "课轨专辑备份 (JSON)",
                        courses = backup.courses,
                        message = "成功解析《${backup.scheduleName}》共 ${backup.courses.size} 门课程",
                        backupMetadata = backup
                    )
                }
            }
        }

        // 3. 尝试解析高校教务系统原生接口返回的 JSON（如新正方 kbList、强智接口、树维、金智接口等）
        if (trimmed.startsWith("{") && (trimmed.contains("\"kcmc\"") || trimmed.contains("\"courseName\"") || trimmed.contains("\"kbList\"") || trimmed.contains("\"lessonList\""))) {
            val apiCourses = parseGenericUniversityApiJson(trimmed)
            if (apiCourses.isNotEmpty()) {
                return ImportResult(
                    success = true,
                    sourceType = "教务系统原生数据接口",
                    courses = apiCourses,
                    message = "成功解析教务接口数据共 ${apiCourses.size} 门课程"
                )
            }
        }

        // 4. 尝试解析教务 WebView / AI 结构化提取的 JSON 数组
        if (trimmed.startsWith("[")) {
            // 4.1 内置浏览器抓取的教务网页原始格子（强智 / 新正方 / 通用表格）
            if (TimetableCellParser.looksLikeCellPayload(trimmed)) {
                val cellCourses = TimetableCellParser.parse(trimmed)
                if (cellCourses.isNotEmpty()) {
                    return ImportResult(
                        success = true,
                        sourceType = "教务网页课表",
                        courses = cellCourses,
                        message = "成功识别并合并共 ${cellCourses.size} 门课程"
                    )
                }
            }

            // 4.2 教务接口常见的外层数组格式
            val apiCourses = parseGenericUniversityApiJson(trimmed)
            if (apiCourses.isNotEmpty()) {
                return ImportResult(
                    success = true,
                    sourceType = "教务系统原生数据接口",
                    courses = apiCourses,
                    message = "成功解析教务接口数据共 ${apiCourses.size} 门课程"
                )
            }

            runCatching {
                val webList = json.decodeFromString<List<WebExtractedCourse>>(trimmed)
                if (webList.isNotEmpty()) {
                    val mapped = webList.mapIndexed { index, item ->
                        CourseItem(
                            name = item.name.trim(),
                            room = item.room.ifBlank { "待定教室" },
                            teacher = item.teacher,
                            dayOfWeek = item.dayOfWeek.coerceIn(1, 7),
                            startSection = item.startSection.coerceIn(1, 18),
                            endSection = item.endSection.coerceAtLeast(item.startSection).coerceIn(1, 18),
                            startWeek = item.startWeek.coerceAtLeast(1),
                            endWeek = item.endWeek.coerceAtLeast(item.startWeek),
                            weeksSpec = item.weeksSpec.trim(),
                            weekParity = when (item.parity.uppercase()) {
                                "ODD", "单" -> WeekParity.ODD
                                "EVEN", "双" -> WeekParity.EVEN
                                else -> WeekParity.ALL
                            },
                            trackColorIndex = index % 6
                        )
                    }
                    val sanitized = mergeAndSanitizeCourses(mapped)
                    return ImportResult(
                        success = true,
                        sourceType = "智能结构化提取",
                        courses = sanitized,
                        message = "成功提取并合并共 ${sanitized.size} 门课程"
                    )
                }
            }
        }

        // 4. 尝试解析 WakeUp 课表多行 JSON 格式（第 4 行为课程字典，第 5 行为排课明细）
        val lines = trimmed.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size >= 5 && lines[3].startsWith("[") && lines[4].startsWith("[")) {
            runCatching {
                val parsedWakeUp = parseWakeUpMultiLineJson(lines[3], lines[4])
                if (parsedWakeUp.isNotEmpty()) {
                    return ImportResult(
                        success = true,
                        sourceType = "WakeUp 课表备份格式",
                        courses = parsedWakeUp,
                        message = "成功从 WakeUp 备份解析 ${parsedWakeUp.size} 门课程"
                    )
                }
            }
        }

        // 5. 优先尝试解析 CSV 格式（多列逗号或制表符分隔表格）
        if (trimmed.lines().any { it.count { c -> c == ',' || c == '，' || c == '\t' } >= 3 }) {
            val csvCourses = parseCsvSchedule(trimmed)
            if (csvCourses.isNotEmpty()) {
                return ImportResult(
                    success = true,
                    sourceType = "CSV 表格数据",
                    courses = csvCourses,
                    message = "成功从 CSV 解析 ${csvCourses.size} 门课程"
                )
            }
        }

        // 6. 尝试解析从默认浏览器复制的教务网页文本或保存的 HTML 源码（含 "1-16(周)" / "[01-02节]" 等特征）
        if (trimmed.contains("节") && trimmed.contains("周")) {
            val browserCourses = parseBrowserCopiedScheduleText(trimmed)
            if (browserCourses.isNotEmpty()) {
                return ImportResult(
                    success = true,
                    sourceType = "浏览器教务课表解析",
                    courses = browserCourses,
                    message = "成功从浏览器课表内容解析出 ${browserCourses.size} 门课程"
                )
            }
        }

        return ImportResult(
            success = false,
            sourceType = "未知格式",
            courses = emptyList(),
            message = "未能识别该格式。支持：浏览器全选复制的教务课表文本、课轨口令、WakeUp 备份或 CSV 表格。"
        )
    }

    private fun extractString(obj: JsonObject, vararg keys: String): String {
        for (k in keys) {
            val v = obj[k]?.jsonPrimitive?.content
            if (!v.isNullOrBlank()) return v.trim()
        }
        return ""
    }

    private fun extractInt(obj: JsonObject, vararg keys: String): Int? {
        for (k in keys) {
            val elem = obj[k] ?: continue
            val directInt = elem.jsonPrimitive.intOrNull
            if (directInt != null) return directInt
            val str = elem.jsonPrimitive.content
            val parsed = str.toIntOrNull()
            if (parsed != null) return parsed
        }
        return null
    }

    private fun findCourseJsonArray(element: JsonElement): JsonArray? {
        when (element) {
            is JsonArray -> {
                if (element.isNotEmpty() && element[0] is JsonObject) {
                    val first = element[0].jsonObject
                    if (first.keys.any {
                        it.contains("kcmc", ignoreCase = true) ||
                            it.contains("course", ignoreCase = true) ||
                            it.contains("lesson", ignoreCase = true) ||
                            it.contains("name", ignoreCase = true)
                    }) {
                        return element
                    }
                }
            }
            is JsonObject -> {
                val candidateKeys = listOf("kbList", "data", "lessons", "courses", "result", "list", "rows", "items", "kbcxAzc", "scheduleList")
                for (key in candidateKeys) {
                    val target = element[key] ?: continue
                    if (target is JsonArray) {
                        val found = findCourseJsonArray(target)
                        if (found != null) return found
                    } else if (target is JsonObject) {
                        val found = findCourseJsonArray(target)
                        if (found != null) return found
                    }
                }
                for ((_, v) in element) {
                    if (v is JsonArray) {
                        val found = findCourseJsonArray(v)
                        if (found != null) return found
                    }
                }
            }
            else -> return null
        }
        return null
    }

    /**
     * 规范化课程名称：去除前缀特殊符号与全角/半角修饰标签（必修、选修、重修、教学班、学时等），便于识别同门课程的加长课
     */
    fun normalizeCourseName(name: String): String {
        var s = name.trim()
            .replace(Regex("^[◆★●▪\\s]+"), "")
            .replace("（", "(")
            .replace("）", ")")
            .replace("【", "[")
            .replace("】", "]")
            .replace("，", ",")
            .replace("：", ":")

        // 去除课程性质与考核标签（覆盖全量高校公选、校选、专选、核心、网课等）
        s = s.replace(Regex("\\((?:必修|选修|限选|任选|公选|校选|通选|专选|核心|专业|考查|考试|理论|实验|实践|上机|重修|双学位|辅修|双语|全英|通识|研讨|讲座|网课|慕课|线上|线下|翻转)\\)"), "")
            .replace(Regex("\\[(?:必修|选修|限选|任选|公选|校选|通选|专选|核心|专业|考查|考试|理论|实验|实践|上机|重修|双学位|辅修|双语|全英|通识|研讨|讲座|网课|慕课|线上|线下|翻转)\\]"), "")

        // 去除学时/节数修饰（如 (3节), (3课时), (3学时), [3节], (3.0学时), (3节连上), (连上3节) 等）
        s = s.replace(Regex("\\((?:连上|连续)?\\d+(?:\\.\\d+)?\\s*(?:节(?:课|连上)?|学时|课时)\\)"), "")
            .replace(Regex("\\[(?:连上|连续)?\\d+(?:\\.\\d+)?\\s*(?:节(?:课|连上)?|学时|课时)\\]"), "")
            .replace(Regex("\\(\\d+(?:\\.\\d+)?-\\d+(?:\\.\\d+)?节?\\)"), "")
            .replace(Regex("\\[\\d+(?:\\.\\d+)?-\\d+(?:\\.\\d+)?节?\\]"), "")

        // 去除学分标注（如 [3学分], (2.5学分)）
        s = s.replace(Regex("\\([0-9.]+\\s*学分\\)"), "")
            .replace(Regex("\\[[0-9.]+\\s*学分\\]"), "")

        // 去除教学班或编号后缀（如 [01], (01), _01, -01）
        s = s.replace(Regex("\\[[A-Za-z0-9_\\-]+\\]$"), "")
            .replace(Regex("\\(0[0-9]+\\)$"), "")
            .replace(Regex("\\([0-9]{2,}\\)$"), "")
            .replace(Regex("_[A-Za-z0-9_]+$"), "")
            .replace(Regex("-[0-9]+$"), "")

        return s.trim()
    }

    /**
     * 智能节次解析：支持 1-3节, [01-03节], 1~3节, 第1-3节, 1-2-3节, 1,2,3节, 3节, 1大节, (1-2), (03-04) 等任意形式
     * 严密防护：
     * 1. 当文本为 "[1-16周]"、"1-16周" 等纯周次时，严禁误判为节次！
     * 2. 当文本同时包含周次和节次（如 "1-16周 1-2节", "1-16周 3-4"）时，先安全剥离周次干扰，精准提取节次！
     */
    fun parseSectionRange(text: String): Pair<Int, Int>? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        // 先剥离显式周次干扰（覆盖 "1-16周"、"2-12(周)"、"2,6,8-9,12-18(周)"、"[01-16周]"、"(1-8单周)"、"{第1-16周|2节/单周}" 等所有变体），
        // 严防在复合文本中误将周次数字（如 2-12(周)）或每周期节数（2节/周）当作节次！
        val sanitized = trimmed
            .replace(Regex("\\{[^}]*周[^}]*\\}"), " ")
            .replace(Regex("\\[[^\\]]*周[^\\]]*\\]"), " ")
            .replace(Regex("\\b\\d{1,2}(?:\\s*(?:-|~|至|到|,|，|、)\\s*\\d{1,2})*\\s*(?:\\([单双]?周\\)|（[单双]?周）|[单双]?周)(?:\\([^)]*\\)|（[^）]*）)?"), " ")
            .replace(Regex("[\\(（]\\d{1,2}(?:\\s*(?:-|~|至|到|,|，|、)\\s*\\d{1,2})*\\s*[单双]?周[\\)）]"), " ")
            .replace("，", ",")
            .replace("～", "~")
            .replace("—", "-")
            .replace("－", "-")
            .replace("至", "-")
            .replace("到", "-")
            .replace("、", ",")
            .trim()

        if (sanitized.isEmpty()) return null

        // 如果包含 "周" 且不包含 "节" 且不包含 "大节"，说明剥离后仍为纯周次，严禁误判为节次！
        if (sanitized.contains("周") && !sanitized.contains("节") && !sanitized.contains("大节")) {
            return null
        }

        // 1. 优先匹配大学排课常见的大节（1大节=1-2, 2大节=3-4, 3大节=5-6, 4大节=7-8, 5大节=9-10）
        val bigSectionMatch = Regex("第?([1-6])大节").find(sanitized)
        if (bigSectionMatch != null) {
            val big = bigSectionMatch.groupValues[1].toIntOrNull() ?: 1
            val start = (big - 1) * 2 + 1
            return Pair(start, start + 1)
        }

        // 2. 优先匹配多节连排枚举（如 [01-02-03节]、1-2-3节、第1,2,3节、01-02-03-04节），防止被双数字正则截断前两节
        val listMatch = Regex("(?<!\\d)(?:第|[\\[(（【])?\\s*(\\d{1,2}(?:\\s*(?:,|-|~)\\s*\\d{1,2}){2,})\\s*(?:小?节|[\\])）】])").find(sanitized)
        if (listMatch != null) {
            val nums = Regex("\\d+").findAll(listMatch.groupValues[1]).mapNotNull { it.value.toIntOrNull() }.filter { it in 1..18 }.toList()
            if (nums.isNotEmpty()) {
                return Pair(nums.minOrNull()!!, nums.maxOrNull()!!)
            }
        }

        // 3. 匹配明确标注 "节" 或带有括号区间的节次范围，例如：
        // [01-03节], 第1-3节, 1-3节, 1~3节, (1-2), (01-03), [1-3], (1-3节)
        val explicitRange = Regex("(?<!\\d)(?:第|[\\[(（【])?\\s*(\\d{1,2})\\s*(?:-|~|,)\\s*(\\d{1,2})\\s*(?:小?节|[\\])）】])").find(sanitized)
        if (explicitRange != null) {
            val start = explicitRange.groupValues[1].toIntOrNull()
            val end = explicitRange.groupValues[2].toIntOrNull()
            if (start != null && end != null && start in 1..18 && end in 1..18) {
                return Pair(minOf(start, end), maxOf(start, end))
            }
        }

        // 3.5 匹配紧凑双位或非补零连续节次（如 [0102节]、[010203节]、1011节、34节、67节、89节、910节、101112节、[0102]）
        val compactMatch = Regex("(?<!\\d)(?:第|[\\[(（【])?\\s*(\\d{2,12})\\s*(?:小?节|[\\])）】])").find(sanitized)
        if (compactMatch != null) {
            val tok = compactMatch.groupValues[1]
            val decoded = TimetableCellParser.decodeDigitTokenToSections(tok, -1, sanitized)
            if (decoded.size >= 2) {
                return Pair(decoded.minOrNull()!!, decoded.maxOrNull()!!)
            }
        }

        // 4. 匹配单节，如：第3节, 3节, [03节], [03], (03)
        val singleMatch = Regex("(?<!\\d)(?:第|[\\[(（【])?\\s*(\\d{1,2})\\s*(?:小?节|[\\])）】])").find(sanitized)
        if (singleMatch != null) {
            val sec = singleMatch.groupValues[1].toIntOrNull()
            if (sec != null && sec in 1..18) {
                return Pair(sec, sec)
            }
        }

        // 5. 纯数字简写（仅当整段文本就是纯数字区间时才匹配，如接口字段 jcs="1-3" 或 "01-02"）
        val pureRange = Regex("^[\\(（\\[【]?\\s*(\\d{1,2})\\s*-\\s*(\\d{1,2})\\s*[\\)）\\]】]?$").find(sanitized)
        if (pureRange != null) {
            val start = pureRange.groupValues[1].toIntOrNull()
            val end = pureRange.groupValues[2].toIntOrNull()
            if (start != null && end != null && start in 1..18 && end in 1..18) {
                return Pair(minOf(start, end), maxOf(start, end))
            }
        }

        // 6. 纯单数字简写 (如 jcs="3", "(3)")
        val pureSingle = Regex("^[\\(（\\[【]?\\s*(\\d{1,2})\\s*[\\)）\\]】]?$").find(sanitized)
        if (pureSingle != null) {
            val sec = pureSingle.groupValues[1].toIntOrNull()
            if (sec != null && sec in 1..18) {
                return Pair(sec, sec)
            }
        }

        return null
    }

    data class ParsedWeeks(
        val startWeek: Int,
        val endWeek: Int,
        val weeksSpec: String,
        val parity: WeekParity
    )

    /**
     * 将周次列表压缩为紧凑字符串，例如 [1, 2, 3, 4, 6, 7, 8] -> "1-4,6-8"
     */
    private fun compressWeeksList(weeks: Collection<Int>): String {
        val sorted = weeks.filter { it in 1..40 }.distinct().sorted()
        if (sorted.isEmpty()) return ""
        val intervals = mutableListOf<String>()
        var start = sorted[0]
        var prev = sorted[0]

        for (i in 1 until sorted.size) {
            val curr = sorted[i]
            if (curr == prev + 1) {
                prev = curr
            } else {
                intervals.add(if (start == prev) "$start" else "$start-$prev")
                start = curr
                prev = curr
            }
        }
        intervals.add(if (start == prev) "$start" else "$start-$prev")
        return intervals.joinToString(",")
    }

    /**
     * 智能周次解析：支持全量格式（连续周、跨周断点 "1-9周, 11-17周"、单双周、01二进制掩码等）
     * 防护：当文本同时包含节次（如 "9-10节 1-8周"）时，必须先剥离节次数字，防止节次被误算入活跃周次！
     */
    fun parseWeekInfo(text: String): ParsedWeeks {
        var startW = 1
        var endW = 18
        var weeksSpec = ""
        val parity = when {
            text.contains("单周") || text.contains("(单)") || text.contains("【单】") ||
                text.contains("[单]") || text.contains("（单）") || text.contains("（单周）") -> WeekParity.ODD
            text.contains("双周") || text.contains("(双)") || text.contains("【双】") ||
                text.contains("[双]") || text.contains("（双）") || text.contains("（双周）") -> WeekParity.EVEN
            else -> WeekParity.ALL
        }

        // 剥离显式节次信息与时间/星期前缀（如 "9-10节", "第1-2节", "[01-02节]", "(3-5节)", "[0102节]", "周一第1,2节", "2节/单周", "08:00-09:35"）
        val withoutSections = text
            .replace(Regex("\\d{1,2}\\s*[:：]\\s*\\d{2}"), " ")
            .replace(Regex("(?:周|星期|礼拜)[一二三四五六日天七1-7]"), " ")
            .replace(Regex("\\d+\\s*小?节\\s*/\\s*[单双]?周"), " ")
            .replace(Regex("(?:第|[\\[(（【])?\\s*\\d{1,2}(?:\\s*(?:-|~|～|至|到|,|，|、)\\s*\\d{1,2})+\\s*(?:小?节|[\\])）】])(?!\\s*周)"), " ")
            .replace(Regex("(?:第|[\\[(（【])?\\s*(?:0[1-9]|1[0-8]){2,6}\\s*(?:小?节|[\\])）】])"), " ")
            .replace(Regex("第?\\s*[0-9一二三四五六七八九十]+\\s*大节"), " ")
            .replace(Regex("(?:第|[\\[(（【])?\\s*\\d{1,2}\\s*小?节"), " ")
            .replace(Regex("[\\[(（【]\\s*\\d{1,2}\\s*[\\])）】](?!\\s*周)"), " ")

        val s = withoutSections
            .replace("，", ",")
            .replace("～", "-")
            .replace("~", "-")
            .replace("—", "-")
            .replace("－", "-")
            .replace("至", "-")
            .replace("到", "-")

        // 1. 处理 01 二进制掩码字符串 (例如 "1111111101111111")
        val maskMatch = Regex("[01]{10,30}").find(s)
        if (maskMatch != null) {
            val mask = maskMatch.value
            val activeWeeks = mask.mapIndexedNotNull { idx, char -> if (char == '1') idx + 1 else null }
            if (activeWeeks.isNotEmpty()) {
                startW = activeWeeks.first()
                endW = activeWeeks.last()
                weeksSpec = if (activeWeeks.size < (endW - startW + 1)) compressWeeksList(activeWeeks) else ""
                return ParsedWeeks(startW, endW, weeksSpec, parity)
            }
        }

        // 2. 提取所有形如 "1-8周, 10-17周" 或 "2,6,8-9,12-18(周)" 或 "1-16周" 的周次集合
        val allActiveWeeks = mutableSetOf<Int>()

        // 提取所有区间和单个数字
        val allMatches = Regex("(\\d{1,2})\\s*-\\s*(\\d{1,2})|(?<!-)\\b(\\d{1,2})\\b(?!-)").findAll(s)
        for (m in allMatches) {
            val rStart = m.groups[1]?.value?.toIntOrNull()
            val rEnd = m.groups[2]?.value?.toIntOrNull()
            val single = m.groups[3]?.value?.toIntOrNull()

            if (rStart != null && rEnd != null && rStart in 1..40 && rEnd in 1..40) {
                val low = minOf(rStart, rEnd)
                val high = maxOf(rStart, rEnd)
                for (w in low..high) {
                    allActiveWeeks.add(w)
                }
            } else if (single != null && single in 1..40) {
                allActiveWeeks.add(single)
            }
        }

        if (allActiveWeeks.isNotEmpty()) {
            startW = allActiveWeeks.minOrNull() ?: 1
            endW = allActiveWeeks.maxOrNull() ?: startW
            weeksSpec = if (allActiveWeeks.size < (endW - startW + 1)) {
                compressWeeksList(allActiveWeeks.sorted())
            } else {
                ""
            }
        } else {
            val simpleMatch = Regex("(\\d{1,2})\\s*-\\s*(\\d{1,2})").find(s)
            if (simpleMatch != null) {
                startW = simpleMatch.groupValues[1].toIntOrNull() ?: 1
                endW = simpleMatch.groupValues[2].toIntOrNull() ?: startW
            }
        }

        return ParsedWeeks(startW, endW, weeksSpec, parity)
    }

    /**
     * 获取课程实际活跃的周次集合
     */
    private fun getActiveWeeks(course: CourseItem): Set<Int> {
        if (course.weeksSpec.isNotBlank()) {
            val parsed = CourseItem.parseWeeksSpec(course.weeksSpec)
            if (parsed.isNotEmpty()) {
                return if (course.weekParity != WeekParity.ALL) {
                    parsed.filter { course.weekParity.matchesWeek(it) }.toSet()
                } else {
                    parsed
                }
            }
        }
        val start = course.startWeek.coerceAtLeast(1)
        val end = course.endWeek.coerceAtLeast(start)
        return (start..end).filter { course.weekParity.matchesWeek(it) }.toSet()
    }

    private val threeSectionHintRegex = Regex(
        "(?<!第)(?:共|连上|连续)?(?:3|3\\.0|三)\\s*(?:节(?:课|连上)?|学时|课时)" +
            "|[\\(\\[【（](?:3|3\\.0|三)\\s*(?:节(?:课|连上)?|学时|课时)[\\)\\]】）]" +
            "|(?:^|[^\\d])(?:1-3|01-03|3-5|03-05|5-7|05-07|6-8|06-08|7-9|07-09|8-10|08-10|9-11|09-11|10-12)\\s*节(?!周)" +
            "|[\\(\\[【（](?:1-3|01-03|3-5|03-05|5-7|05-07|6-8|06-08|7-9|07-09|8-10|08-10|9-11|09-11|10-12)(?:节)?[\\)\\]】）](?!周)"
    )

    /**
     * 检查文本中是否含有 3 节加长课的明确标识
     * 精确区分：
     * - "3节"、"3学时"、"3课时"、"1-3节"、"5-7节" 等为时长/区间标识（返回 true）
     * - "第3节" 为节次索引序号（如随堂测试、周二第3节），绝不误判为 3 节长课标识（返回 false）
     */
    fun hasThreeSectionHint(vararg texts: String): Boolean {
        for (raw in texts) {
            if (raw.isBlank()) continue
            val t = raw.trim()
            if (threeSectionHintRegex.containsMatchIn(t)) {
                return true
            }
        }
        return false
    }

    /**
     * 旗舰级智能课程合并与消毒引擎 (Smart Course Interval Merger)：
     * 解决教务系统将 3 节加长课切散为两个连续 2 节课、或不同格子重复拆分的痛点：
     * 1. 识别相同星期、相同课程名称的相邻或重叠连续节次（如 1-2 节 + 3 节合并为 1-3 节；1-2 节 + 3-4 节连堂合并；1-1 + 2-2 + 3-3 合并为 1-3 节）；
     * 2. 多轮收敛合并 (Fixpoint Multi-Pass Merge)，直到所有相邻块完全融合；
     * 3. 严格周次活跃度验证：不同节次合并（加长课缝合）必须具有完全一致的活跃周次（aWeeks == bWeeks），
     *    严禁将前半学期 1-8 周的周一 1-2 节与后半学期 9-16 周的周一 3-4 节错误合并为全天 1-4 节，彻底杜绝幽灵课程！
     * 4. 智能合并周次范围与离散周次并集，继承更详细的教室与教师；
     * 5. 彻底避免课表破碎分散，100% 还原真实连贯课程体验！
     */
    fun mergeAndSanitizeCourses(rawCourses: List<CourseItem>): List<CourseItem> {
        if (rawCourses.isEmpty()) return emptyList()

        val validCourses = rawCourses
            .filter { it.name.trim().length >= 2 }
            .map { item ->
                // 单门课节次异常跨度防护：正常单次排课不会跨越 6 节以上且直达第 10~18 节（通常由旧版周次 2-12(周) 误判引起）
                val span = item.endSection - item.startSection + 1
                if (span >= 5 && item.endSection >= 10 && !hasThreeSectionHint(item.name, item.note)) {
                    val fixedStart = if (item.startSection in listOf(2, 4, 6, 8, 10)) item.startSection - 1 else item.startSection
                    val fixedEnd = (fixedStart + 1).coerceAtMost(12)
                    item.copy(startSection = fixedStart, endSection = fixedEnd)
                } else {
                    item
                }
            }
        if (validCourses.isEmpty()) return emptyList()

        val grouped = validCourses.groupBy { "${it.dayOfWeek}_${normalizeCourseName(it.name).lowercase()}" }
        val mergedResult = mutableListOf<CourseItem>()

        for ((_, group) in grouped) {
            val pool = group.toMutableList()

            fun applyWeeksSet(template: CourseItem, weeks: Set<Int>): CourseItem {
                val sorted = weeks.filter { it in 1..40 }.sorted()
                if (sorted.isEmpty()) return template
                val minW = sorted.first()
                val maxW = sorted.last()
                val spec = if (sorted.size < (maxW - minW + 1)) compressWeeksList(sorted) else ""
                return template.copy(
                    startWeek = minW,
                    endWeek = maxW,
                    weeksSpec = spec,
                    weekParity = if (spec.isNotBlank()) WeekParity.ALL else template.weekParity
                )
            }

            // -----------------------------------------------------------------
            // 阶段 1：跨节次纵向缝合优先（如 3-4节 + 5节 → 3-5节；10-11节 + 12节 → 10-12节；6-7节 + 8-9节 → 6-9节）
            // 必须在同节次跨周次合并之前执行，并支持周次交集拆分：
            // 即使 10-11节 已跨 12-15周、而 12节 仅在 12-13周，也会将 12-13周 精准缝合为 10-12节（3节长课），14-15周 保留为 10-11节！
            // -----------------------------------------------------------------
            var verticalModified = true
            while (verticalModified) {
                verticalModified = false
                outerVertical@ for (i in 0 until pool.size) {
                    for (j in i + 1 until pool.size) {
                        val a = pool[i]
                        val b = pool[j]

                        val isSameSection = (a.startSection == b.startSection && a.endSection == b.endSection)
                        val isSectionAdjacentOrOverlapping =
                            (a.startSection <= b.endSection + 1) && (b.startSection <= a.endSection + 1)
                        if (isSameSection || !isSectionAdjacentOrOverlapping) continue

                        val roomCompatible = a.room.isBlank() || a.room == "待定教室" ||
                            b.room.isBlank() || b.room == "待定教室" ||
                            a.room == b.room
                        val teacherCompatible = a.teacher.isBlank() || b.teacher.isBlank() ||
                            a.teacher == b.teacher
                        if (!roomCompatible || !teacherCompatible) continue

                        val aWeeks = getActiveWeeks(a)
                        val bWeeks = getActiveWeeks(b)
                        val commonWeeks = aWeeks intersect bWeeks
                        if (commonWeeks.isEmpty()) continue

                        var mergedStartSec = minOf(a.startSection, b.startSection)
                        var mergedEndSec = maxOf(a.endSection, b.endSection)
                        val isThreeSectionHint = hasThreeSectionHint(a.name, b.name, a.note, b.note)
                        if (isThreeSectionHint && (mergedEndSec - mergedStartSec + 1) > 3) {
                            mergedEndSec = mergedStartSec + 2
                        }

                        val preferredRoom = when {
                            a.room.isNotBlank() && a.room != "待定教室" -> a.room
                            b.room.isNotBlank() && b.room != "待定教室" -> b.room
                            else -> "待定教室"
                        }
                        val preferredTeacher = when {
                            a.teacher.isNotBlank() -> a.teacher
                            b.teacher.isNotBlank() -> b.teacher
                            else -> ""
                        }
                        val preferredName = if (a.name.length >= b.name.length) a.name else b.name
                        val preferredParity = if (a.weekParity == b.weekParity) a.weekParity else WeekParity.ALL

                        val stitchedBase = a.copy(
                            name = preferredName,
                            room = preferredRoom,
                            teacher = preferredTeacher,
                            startSection = mergedStartSec,
                            endSection = mergedEndSec,
                            weekParity = preferredParity
                        )
                        val stitched = applyWeeksSet(stitchedBase, commonWeeks)
                        val aOnlyWeeks = aWeeks - commonWeeks
                        val bOnlyWeeks = bWeeks - commonWeeks

                        pool.removeAt(j)
                        pool.removeAt(i)
                        pool.add(stitched)
                        if (aOnlyWeeks.isNotEmpty()) {
                            pool.add(applyWeeksSet(a, aOnlyWeeks))
                        }
                        if (bOnlyWeeks.isNotEmpty()) {
                            pool.add(applyWeeksSet(b, bOnlyWeeks))
                        }
                        verticalModified = true
                        break@outerVertical
                    }
                }
            }

            // -----------------------------------------------------------------
            // 阶段 2：同节次跨周次收敛合并（如相同节次、相同教室、相同教师的离散周次合并为一条）
            // -----------------------------------------------------------------
            var horizontalModified = true
            while (horizontalModified) {
                horizontalModified = false
                outerHorizontal@ for (i in 0 until pool.size) {
                    for (j in i + 1 until pool.size) {
                        val a = pool[i]
                        val b = pool[j]

                        if (a.startSection != b.startSection || a.endSection != b.endSection) continue

                        val aWeeks = getActiveWeeks(a)
                        val bWeeks = getActiveWeeks(b)

                        // 若周次完全相同（重复格子），允许一方教室/教师为空并互补；若为不同周次，则要求教室与教师一致才合并
                        val sameWeeks = aWeeks == bWeeks
                        val roomCompatible = if (sameWeeks) {
                            a.room.isBlank() || a.room == "待定教室" || b.room.isBlank() || b.room == "待定教室" || a.room == b.room
                        } else {
                            a.room == b.room
                        }
                        val teacherCompatible = if (sameWeeks) {
                            a.teacher.isBlank() || b.teacher.isBlank() || a.teacher == b.teacher
                        } else {
                            a.teacher == b.teacher
                        }
                        if (!roomCompatible || !teacherCompatible) continue

                        val preferredRoom = when {
                            a.room.isNotBlank() && a.room != "待定教室" -> a.room
                            b.room.isNotBlank() && b.room != "待定教室" -> b.room
                            else -> "待定教室"
                        }
                        val preferredTeacher = when {
                            a.teacher.isNotBlank() -> a.teacher
                            b.teacher.isNotBlank() -> b.teacher
                            else -> ""
                        }
                        val preferredName = if (a.name.length >= b.name.length) a.name else b.name
                        val unionWeeks = aWeeks union bWeeks
                        val merged = applyWeeksSet(
                            a.copy(
                                name = preferredName,
                                room = preferredRoom,
                                teacher = preferredTeacher,
                                weekParity = if (a.weekParity == b.weekParity) a.weekParity else WeekParity.ALL
                            ),
                            unionWeeks
                        )

                        pool.removeAt(j)
                        pool[i] = merged
                        horizontalModified = true
                        break@outerHorizontal
                    }
                }
            }

            mergedResult.addAll(pool)
        }

        // 按课程规范名称统一分配 trackColorIndex，使得同一课程无论在周几都保持一致的主题色
        val courseNameToColorIndex = mutableMapOf<String, Int>()
        var nextColorIdx = 0

        return mergedResult
            .sortedWith(compareBy({ it.dayOfWeek }, { it.startSection }, { it.startWeek }))
            .map { item ->
                val norm = normalizeCourseName(item.name).lowercase()
                val colorIdx = courseNameToColorIndex.getOrPut(norm) {
                    (nextColorIdx++) % 6
                }
                item.copy(trackColorIndex = colorIdx)
            }
    }

    /**
     * 安全提取星期几（支持整数 1..7、0=周日、数字字符串、中文 "周一".."周日"、"星期一".."星期七"、"周1".."周7" 等）
     */
    fun extractDayOfWeek(obj: JsonObject, vararg keys: String): Int? {
        for (k in keys) {
            val elem = obj[k] ?: continue
            val directInt = elem.jsonPrimitive.intOrNull
            if (directInt != null) {
                if (directInt in 1..7) return directInt
                if (directInt == 0) return 7
            }
            val str = elem.jsonPrimitive.content.trim()
            val parsedInt = str.toIntOrNull()
            if (parsedInt != null) {
                if (parsedInt in 1..7) return parsedInt
                if (parsedInt == 0) return 7
            }
            val chineseDay = parseChineseDayOfWeek(str)
            if (chineseDay != null) return chineseDay
        }
        return null
    }

    /**
     * 解析高校教务系统原生接口返回的 JSON（新正方、强智、树维、青果、URP等）
     */
    fun parseGenericUniversityApiJson(jsonStr: String): List<CourseItem> {
        return runCatching {
            val root = json.parseToJsonElement(jsonStr)
            val courseArray = findCourseJsonArray(root) ?: return emptyList()
            val result = mutableListOf<CourseItem>()
            courseArray.forEachIndexed { index, elem ->
                if (elem !is JsonObject) return@forEachIndexed
                val obj = elem.jsonObject
                val name = extractString(obj, "kcmc", "courseName", "className", "kcm", "name", "course_name")
                if (name.isBlank() || name.length < 2) return@forEachIndexed

                val room = extractString(obj, "cdmc", "jxcdmc", "room", "classroom", "place", "location", "cd_id", "jsmc", "jasyl").ifBlank { "待定教室" }
                val teacher = extractString(obj, "xm", "teName", "teacher", "zjsxm", "rkjs", "teacherName", "jsmc")
                val dayOfWeek = extractDayOfWeek(obj, "xqj", "dayOfWeek", "day", "xq", "weekday", "week", "skxq", "xqjmc", "skxqmc", "day_of_week") ?: 1

                var startSec = extractInt(obj, "startUnit", "ksjc", "startSection", "beginSection", "qsdj") ?: 1
                val rawEndSec = extractInt(obj, "endUnit", "jsjc", "endSection", "zzdj")
                val step = extractInt(obj, "step", "skcd", "duration", "length", "djc", "totalUnit")
                var endSec = when {
                    rawEndSec != null -> rawEndSec
                    step != null && step > 0 -> startSec + step - 1
                    else -> startSec + 1
                }
                val jcsStr = extractString(obj, "jcs", "jc", "skjc", "sksj", "section", "sectionRange", "ksjc_jsjc", "sjd", "time", "kcsj")
                if (jcsStr.isNotBlank()) {
                    val parsedRange = parseSectionRange(jcsStr)
                    if (parsedRange != null) {
                        startSec = parsedRange.first
                        endSec = parsedRange.second
                    }
                }

                val note = extractString(obj, "note", "bz", "beizhu", "memo", "remark", "kcbz")
                val weeksText = extractString(obj, "zcd", "weeks", "zc", "zcs", "allWeeks", "weeksText", "weekList", "qsjsz", "kkzc", "skzc")
                var startWeek = extractInt(obj, "startWeek", "beginWeek", "qsz") ?: 1
                var endWeek = extractInt(obj, "endWeek", "jsz") ?: 18
                var weeksSpec = ""
                var parity = WeekParity.ALL

                if (weeksText.isNotBlank()) {
                    if (weeksText.all { it == '0' || it == '1' } && weeksText.length >= 10) {
                        val activeWeeks = weeksText.mapIndexedNotNull { wIdx, char -> if (char == '1') wIdx + 1 else null }
                        if (activeWeeks.isNotEmpty()) {
                            startWeek = activeWeeks.first()
                            endWeek = activeWeeks.last()
                            weeksSpec = if (activeWeeks.size < (endWeek - startWeek + 1)) compressWeeksList(activeWeeks) else ""
                        }
                    } else {
                        val parsed = parseWeekInfo(weeksText)
                        startWeek = parsed.startWeek
                        endWeek = parsed.endWeek
                        weeksSpec = parsed.weeksSpec
                        parity = parsed.parity
                    }
                }

                result.add(
                    CourseItem(
                        name = name.replace(Regex("^[◆★●▪\\s]+"), ""),
                        room = room,
                        teacher = teacher,
                        dayOfWeek = dayOfWeek.coerceIn(1, 7),
                        startSection = startSec.coerceIn(1, 18),
                        endSection = endSec.coerceIn(startSec, 18),
                        startWeek = startWeek.coerceAtLeast(1),
                        endWeek = endWeek.coerceAtLeast(startWeek),
                        weeksSpec = weeksSpec,
                        weekParity = parity,
                        note = note,
                        trackColorIndex = index % 6
                    )
                )
            }
            mergeAndSanitizeCourses(result)
        }.getOrDefault(emptyList())
    }

    /**
     * 基于语义块与实体评分重构的教务课表文本解析器：
     * 解决脆弱固定行偏移与随机星期分发的致命缺陷，精准提取 1-3 节、1-4 节等任意跨度课程！
     */
    private fun parseBrowserCopiedScheduleText(raw: String): List<CourseItem> {
        val cleaned = if (raw.contains("<table", ignoreCase = true) || raw.contains("<div", ignoreCase = true)) {
            raw.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("</(td|tr|div|p)>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("<[^>]+>"), " ")
                .replace("&nbsp;", " ")
        } else {
            raw
        }

        val lines = cleaned.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val rawCourses = mutableListOf<CourseItem>()
        var currentDay = 1

        var idx = 0
        while (idx < lines.size) {
            val line = lines[idx]

            // 1. 星期标头识别（若该行同时包含节次，更新星期后继续解析该行；纯标头行则递增跳过）
            val matchedDay = parseChineseDayOfWeek(line)
            val lineSecRange = parseSectionRange(line)
            if (matchedDay != null && lineSecRange == null && !line.contains("节") &&
                (line.length <= 5 || line.startsWith("周") || line.startsWith("星期") || line.startsWith("礼拜"))
            ) {
                currentDay = matchedDay
                idx++
                continue
            } else if (matchedDay != null && (line.startsWith("周") || line.startsWith("星期") || line.startsWith("礼拜"))) {
                currentDay = matchedDay
            }

            // 2. 检查是否为节次或周次行
            val secRange = lineSecRange ?: parseSectionRange(line)
            val weekInfoInLine = parseWeekInfo(line)
            val hasDirectSec = secRange != null
            val hasExplicitWeek = Regex("\\d{1,2}\\s*(?:-|~|至|到)\\s*\\d{1,2}\\s*周").containsMatchIn(line) ||
                Regex("\\d{1,2}\\s*周").containsMatchIn(line) ||
                weekInfoInLine.weeksSpec.isNotBlank()

            // 仅当该行包含节次，或者该行包含周次且前面3行内均无节次行（防止将单纯周次行当作独立新课二次入库）时才触发课程块解析
            val hasPrecedingSec = (0 until idx).any { prevIdx ->
                prevIdx >= idx - 3 && parseSectionRange(lines[prevIdx]) != null
            }

            if (hasDirectSec || (hasExplicitWeek && !hasPrecedingSec)) {
                val startWindow = (idx - 3).coerceAtLeast(0)
                val endWindow = (idx + 3).coerceAtMost(lines.size - 1)
                val windowLines = (startWindow..endWindow).map { lines[it] }

                val finalSec = secRange ?: windowLines.firstNotNullOfOrNull { parseSectionRange(it) } ?: Pair(1, 2)
                val finalWeek = if (hasExplicitWeek) {
                    weekInfoInLine
                } else {
                    windowLines.firstNotNullOfOrNull { ln ->
                        if (Regex("\\d{1,2}\\s*周").containsMatchIn(ln) || Regex("\\d{1,2}\\s*(?:-|~)\\s*\\d{1,2}").containsMatchIn(ln)) {
                            val w = parseWeekInfo(ln)
                            if (w.startWeek != 1 || w.endWeek != 18 || ln.contains("18周") || w.weeksSpec.isNotBlank()) w else null
                        } else null
                    } ?: ParsedWeeks(1, 18, "", WeekParity.ALL)
                }

                var courseName = ""
                var room = "待定教室"
                var teacher = ""

                val candidateLines = windowLines.filter { ln ->
                    ln != line &&
                    parseSectionRange(ln) == null &&
                    !ln.contains("周") &&
                    !ln.contains("学分") &&
                    !ln.contains("考核") &&
                    !ln.contains("必修") &&
                    !ln.contains("选修") &&
                    parseChineseDayOfWeek(ln) == null
                }

                for (cand in candidateLines) {
                    val isRoom = cand.contains("楼") || cand.contains("教") || cand.contains("室") ||
                        cand.contains("实") || cand.contains("机房") || cand.contains("中心") ||
                        cand.contains("馆") || Regex("^[A-Za-z]\\d{2,4}").containsMatchIn(cand)

                    val isTeacher = cand.contains("教授") || cand.contains("讲师") || cand.contains("老师") ||
                        (cand.length in 2..4 && !isRoom && cand.all { it.isLetter() })

                    if (isRoom && room == "待定教室") {
                        room = cand
                    } else if (isTeacher && teacher.isBlank()) {
                        teacher = cand
                    } else if (courseName.isBlank() && cand.length in 2..30) {
                        courseName = cand
                    }
                }

                if (courseName.isBlank()) {
                    val strippedLine = line
                        .replace(Regex("(?:周|星期|礼拜)[一二三四五六日天七1-7]"), " ")
                        .replace(Regex("(?:第|[\\[(（【])?\\d{1,2}\\s*(?:-|~)\\s*\\d{1,2}\\s*(?:节|[\\])）】])?"), " ")
                        .replace(Regex("[\\[(（【]?\\d{1,2}\\s*(?:-|~)\\s*\\d{1,2}\\s*周[^\\]]*[\\])）】]?"), " ")
                        .trim()
                    courseName = if (strippedLine.length in 2..30) {
                        strippedLine
                    } else {
                        candidateLines.firstOrNull() ?: "课程 #${rawCourses.size + 1}"
                    }
                }

                rawCourses.add(
                    CourseItem(
                        name = courseName.replace(Regex("^[◆★●▪\\s]+"), ""),
                        room = room,
                        teacher = teacher,
                        dayOfWeek = currentDay,
                        startSection = finalSec.first.coerceIn(1, 18),
                        endSection = finalSec.second.coerceIn(finalSec.first, 18),
                        startWeek = finalWeek.startWeek,
                        endWeek = finalWeek.endWeek,
                        weeksSpec = finalWeek.weeksSpec,
                        weekParity = finalWeek.parity
                    )
                )
            }
            idx++
        }

        // 统一经过加长课合并与去重引擎！
        return mergeAndSanitizeCourses(rawCourses)
    }

    /**
     * 解析 WakeUp 课表导出的第 4 行 (课程基本信息) 与第 5 行 (上课节次安排)
     */
    private fun parseWakeUpMultiLineJson(coursesLine: String, detailsLine: String): List<CourseItem> {
        val courseArr = json.parseToJsonElement(coursesLine).jsonArray
        val detailArr = json.parseToJsonElement(detailsLine).jsonArray

        val courseNameMap = mutableMapOf<Int, String>()
        for (elem in courseArr) {
            val obj = elem.jsonObject
            val id = obj["id"]?.jsonPrimitive?.intOrNull ?: continue
            val name = obj["courseName"]?.jsonPrimitive?.content ?: continue
            courseNameMap[id] = name
        }

        val result = mutableListOf<CourseItem>()
        detailArr.forEachIndexed { idx, elem ->
            val obj = elem.jsonObject
            val id = obj["id"]?.jsonPrimitive?.intOrNull ?: return@forEachIndexed
            val name = courseNameMap[id] ?: "课程 #$id"
            val day = obj["day"]?.jsonPrimitive?.intOrNull ?: 1
            val room = obj["room"]?.jsonPrimitive?.content ?: "待定教室"
            val teacher = obj["teacher"]?.jsonPrimitive?.content ?: ""
            val startNode = obj["startNode"]?.jsonPrimitive?.intOrNull ?: 1
            val step = obj["step"]?.jsonPrimitive?.intOrNull ?: 2
            val startWeek = obj["startWeek"]?.jsonPrimitive?.intOrNull ?: 1
            val endWeek = obj["endWeek"]?.jsonPrimitive?.intOrNull ?: 16
            val type = obj["type"]?.jsonPrimitive?.intOrNull ?: 0
            val parity = when (type) {
                1 -> WeekParity.ODD
                2 -> WeekParity.EVEN
                else -> WeekParity.ALL
            }
            result.add(
                CourseItem(
                    name = name,
                    room = room,
                    teacher = teacher,
                    dayOfWeek = day.coerceIn(1, 7),
                    startSection = startNode.coerceIn(1, 18),
                    endSection = (startNode + step - 1).coerceIn(startNode, 18),
                    startWeek = startWeek,
                    endWeek = endWeek,
                    weekParity = parity,
                    trackColorIndex = idx % 6
                )
            )
        }
        return mergeAndSanitizeCourses(result)
    }

    /**
     * 解析 CSV：支持表头跳过，格式：课程名称,星期,开始节次,结束节次,教室,教师,开始周,结束周
     */
    private fun parseCsvSchedule(csvText: String): List<CourseItem> {
        val result = mutableListOf<CourseItem>()
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        lines.forEachIndexed { index, line ->
            val cols = line.split(",", "，", "\t").map { it.trim() }
            if (cols.size >= 4) {
                val day = cols[1].toIntOrNull() ?: parseChineseDayOfWeek(cols[1])
                val startSec = cols[2].toIntOrNull()
                val endSec = cols[3].toIntOrNull()
                if (day != null && startSec != null && endSec != null) {
                    val room = cols.getOrNull(4)?.ifBlank { "待定教室" } ?: "待定教室"
                    val teacher = cols.getOrNull(5) ?: ""
                    val startWeek = cols.getOrNull(6)?.toIntOrNull() ?: 1
                    val endWeek = cols.getOrNull(7)?.toIntOrNull() ?: 16
                    result.add(
                        CourseItem(
                            name = cols[0],
                            room = room,
                            teacher = teacher,
                            dayOfWeek = day.coerceIn(1, 7),
                            startSection = startSec.coerceIn(1, 18),
                            endSection = endSec.coerceIn(startSec, 18),
                            startWeek = startWeek,
                            endWeek = endWeek,
                            trackColorIndex = index % 6
                        )
                    )
                }
            }
        }
        return mergeAndSanitizeCourses(result)
    }

    fun parseChineseDayOfWeek(text: String): Int? {
        val t = text.trim()
        return when {
            t.contains("周一") || t.contains("星期一") || t.contains("礼拜一") || t.contains("周1") || t.contains("星期1") || t.contains("礼拜1") || t.contains("Mon", ignoreCase = true) -> 1
            t.contains("周二") || t.contains("星期二") || t.contains("礼拜二") || t.contains("周2") || t.contains("星期2") || t.contains("礼拜2") || t.contains("Tue", ignoreCase = true) -> 2
            t.contains("周三") || t.contains("星期三") || t.contains("礼拜三") || t.contains("周3") || t.contains("星期3") || t.contains("礼拜3") || t.contains("Wed", ignoreCase = true) -> 3
            t.contains("周四") || t.contains("星期四") || t.contains("礼拜四") || t.contains("周4") || t.contains("星期4") || t.contains("礼拜4") || t.contains("Thu", ignoreCase = true) -> 4
            t.contains("周五") || t.contains("星期五") || t.contains("礼拜五") || t.contains("周5") || t.contains("星期5") || t.contains("礼拜5") || t.contains("Fri", ignoreCase = true) -> 5
            t.contains("周六") || t.contains("星期六") || t.contains("礼拜六") || t.contains("周6") || t.contains("星期6") || t.contains("礼拜6") || t.contains("Sat", ignoreCase = true) -> 6
            t.contains("周日") || t.contains("周天") || t.contains("周七") || t.contains("周7") ||
                t.contains("星期日") || t.contains("星期天") || t.contains("星期七") || t.contains("星期7") ||
                t.contains("礼拜日") || t.contains("礼拜天") || t.contains("礼拜七") || t.contains("礼拜7") ||
                t.contains("Sun", ignoreCase = true) -> 7
            t == "一" || t == "1" -> 1
            t == "二" || t == "2" -> 2
            t == "三" || t == "3" -> 3
            t == "四" || t == "4" -> 4
            t == "五" || t == "5" -> 5
            t == "六" || t == "6" -> 6
            t == "日" || t == "天" || t == "七" || t == "7" || t == "0" -> 7
            else -> null
        }
    }

    /**
     * 注入高校教务系统内置 WebView 的课表「原始格子」提取脚本。
     *
     * 只做 DOM 层面的工作：还原表格视觉网格（处理 rowspan / colspan），
     * 计算每个课程块所在的星期列与行头文本，原样输出文本和带 title 的字段，
     * 语义解析统一交由 [TimetableCellParser] 在 Kotlin 侧完成（可单元测试）。
     *
     * 适配：
     * 1. 强智教务 (div.kbcontent 详细层，缺失时回退 div.kbcontent1；按 "-----" 拆分同格多门课)
     * 2. 新正方教务 (.timetable_con，td.id = "星期-节次"，rowspan = 节数)
     * 3. 通用 HTML 课表表格兜底
     * 支持跨多层 iframe / frame 穿透。
     */
    val eduSystemExtractJs: String = """
        (function() {
            var out = [];
            var docs = [];
            function collectDocs(d) {
                if (!d || docs.indexOf(d) >= 0) return;
                docs.push(d);
                try {
                    var frames = d.querySelectorAll('iframe, frame');
                    for (var i = 0; i < frames.length; i++) {
                        try {
                            var sub = frames[i].contentDocument || (frames[i].contentWindow && frames[i].contentWindow.document);
                            if (sub) collectDocs(sub);
                        } catch (e1) {}
                    }
                } catch (e) {}
            }
            collectDocs(document);

            function txt(el) {
                if (!el) return '';
                return String(el.innerText || el.textContent || '').replace(/\u00a0/g, ' ').trim();
            }

            function htmlToText(html) {
                return String(html || '')
                    .replace(/<br\s*\/?>/gi, '\n')
                    .replace(/<\/(font|div|p|li|tr|td|span|h\d)>/gi, '\n')
                    .replace(/<[^>]+>/g, ' ')
                    .replace(/&nbsp;/gi, ' ')
                    .replace(/&amp;/gi, '&')
                    .replace(/&lt;/gi, '<')
                    .replace(/&gt;/gi, '>')
                    .split('\n')
                    .map(function(s) { return s.replace(/\s+/g, ' ').trim(); })
                    .filter(function(s) { return s.length > 0; })
                    .join('\n');
            }

            function titledFields(html) {
                var res = [];
                try {
                    var tmp = document.createElement('div');
                    tmp.innerHTML = html;
                    var nodes = tmp.querySelectorAll('[title]');
                    for (var i = 0; i < nodes.length; i++) {
                        var t = String(nodes[i].getAttribute('title') || '').trim();
                        var v = txt(nodes[i]) || String(nodes[i].textContent || '').trim();
                        if (t && v) res.push({ t: t, v: v });
                    }
                } catch (e) {}
                return res;
            }

            // ---------- 表格视觉网格还原 ----------
            var layoutCache = [];
            function dayFromHeader(s) {
                s = String(s || '').replace(/\s+/g, '');
                if (!s || s.length > 30) return 0;
                if (s.indexOf('节') >= 0 || s.indexOf('周次') >= 0 || s.indexOf('时间') >= 0) return 0;
                var m = s.match(/(?:星期|周|礼拜)([一二三四五六日天七1-7])/);
                var ch = m ? m[1] : (s.length === 1 ? s : '');
                var map = { '一': 1, '1': 1, '二': 2, '2': 2, '三': 3, '3': 3, '四': 4, '4': 4, '五': 5, '5': 5, '六': 6, '6': 6, '日': 7, '天': 7, '七': 7, '7': 7 };
                if (map[ch]) return map[ch];
                var en = s.toLowerCase();
                var ens = ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'];
                for (var i = 0; i < ens.length; i++) if (en.indexOf(ens[i]) === 0) return i + 1;
                return 0;
            }

            function layoutTable(table) {
                for (var x = 0; x < layoutCache.length; x++) if (layoutCache[x].table === table) return layoutCache[x];
                var grid = [];
                var posList = [];
                var rows = table.rows || [];
                for (var r = 0; r < rows.length; r++) {
                    grid[r] = grid[r] || [];
                    var c = 0;
                    var cells = rows[r].cells || [];
                    for (var k = 0; k < cells.length; k++) {
                        var cell = cells[k];
                        while (grid[r][c]) c++;
                        var rs = Math.max(1, parseInt(cell.rowSpan) || 1);
                        var cs = Math.max(1, parseInt(cell.colSpan) || 1);
                        for (var dr = 0; dr < rs; dr++) {
                            grid[r + dr] = grid[r + dr] || [];
                            for (var dc = 0; dc < cs; dc++) grid[r + dr][c + dc] = cell;
                        }
                        posList.push({ cell: cell, row: r, col: c, rs: rs, cs: cs });
                        c += cs;
                    }
                }
                // 星期列：在前 8 行中严格匹配「星期X / 周X / Mon」，杜绝 "第一大节" 之类的误判
                var dayByCol = {};
                var headerRow = -1;
                var minDayCol = 999;
                for (var hr = 0; hr < Math.min(grid.length, 8); hr++) {
                    var found = 0;
                    var seen = [];
                    for (var hc = 0; hc < (grid[hr] || []).length; hc++) {
                        var hcell = grid[hr][hc];
                        if (!hcell || seen.indexOf(hcell) >= 0) continue;
                        seen.push(hcell);
                        var d = dayFromHeader(txt(hcell));
                        if (d) {
                            dayByCol[hc] = d;
                            if (hc < minDayCol) minDayCol = hc;
                            found++;
                        }
                    }
                    if (found >= 3) { headerRow = hr; break; }
                    dayByCol = {};
                    minDayCol = 999;
                }
                // 行头文本与课程行序号（跳过午休/备注等非课程分隔行）
                var rowHeader = [];
                var rowOrdinal = [];
                var ord = 0;
                for (var rr = 0; rr < grid.length; rr++) {
                    var parts = [];
                    var seenH = [];
                    var limit = minDayCol === 999 ? 1 : minDayCol;
                    for (var cc = 0; cc < limit; cc++) {
                        var hcl = (grid[rr] || [])[cc];
                        if (hcl && seenH.indexOf(hcl) < 0) { seenH.push(hcl); parts.push(txt(hcl)); }
                    }
                    rowHeader[rr] = parts.join(' ').replace(/\s+/g, ' ').trim();
                    var rh = rowHeader[rr];
                    var rowCellsCount = (rows[rr] && rows[rr].cells) ? rows[rr].cells.length : 0;
                    var isSeparator = !rh || rh.indexOf('备注') >= 0 || rh.indexOf('说明') >= 0 || rh.indexOf('合计') >= 0 || rh.indexOf('安排') >= 0 ||
                        (rowCellsCount <= 2 && (rh === '午休' || rh === '中午' || rh === '下午' || rh === '晚上' || rh === '晚自习'));
                    if (rr > headerRow && !isSeparator) {
                        rowOrdinal[rr] = ord++;
                    } else {
                        rowOrdinal[rr] = -1;
                    }
                }
                var lay = { table: table, posList: posList, dayByCol: dayByCol, minDayCol: minDayCol, headerRow: headerRow, rowHeader: rowHeader, rowOrdinal: rowOrdinal };
                layoutCache.push(lay);
                return lay;
            }

            function locate(td) {
                var table = td ? td.closest('table') : null;
                if (!table) return null;
                var lay = layoutTable(table);
                for (var i = 0; i < lay.posList.length; i++) {
                    var p = lay.posList[i];
                    if (p.cell === td) {
                        var day = lay.dayByCol[p.col] || 0;
                        if (!day && lay.minDayCol !== 999) {
                            var guess = p.col - lay.minDayCol + 1;
                            if (guess >= 1 && guess <= 7) day = guess;
                        }
                        var headers = [];
                        for (var dr = 0; dr < p.rs; dr++) {
                            var h = lay.rowHeader[p.row + dr];
                            if (h && headers.indexOf(h) < 0) headers.push(h);
                        }
                        return {
                            day: day,
                            rowHeader: headers.join(' '),
                            rowOrdinal: lay.rowOrdinal[p.row] == null ? -1 : lay.rowOrdinal[p.row],
                            rs: p.rs
                        };
                    }
                }
                return null;
            }

            function splitCellSegments(html) {
                return String(html || '').split(/(?:<br\s*\/?>|\n|^)\s*(?:-{2,}|={2,}|─{2,}|—{2,})\s*(?=<br\s*\/?>|\n|$)|-{3,}|={3,}|<hr\b[^>]*>/i);
            }

            function emit(source, day, rowStart, rowEnd, rowHeader, rowOrdinal, html) {
                var text = htmlToText(html);
                if (!text || text.replace(/\s/g, '').length < 2) return;
                out.push({
                    kind: 'cell',
                    source: source,
                    day: day || 0,
                    rowStart: rowStart || 0,
                    rowEnd: rowEnd || 0,
                    rowHeader: rowHeader || '',
                    rowOrdinal: rowOrdinal == null ? -1 : rowOrdinal,
                    text: text,
                    fields: titledFields(html)
                });
            }

            for (var di = 0; di < docs.length; di++) {
                var doc = docs[di];
                var before = out.length;

                // 1. 强智：详细层 div.kbcontent 优先（含教师/节次），单格缺失或为空时回退同格简略层 div.kbcontent1
                var qz = doc.querySelectorAll('div.kbcontent');
                if (!qz || qz.length === 0) qz = doc.querySelectorAll('div.kbcontent1');
                for (var qi = 0; qi < qz.length; qi++) {
                    var div = qz[qi];
                    var td = div.closest('td');
                    if (!txt(div) && td) {
                        var alt = td.querySelector('div.kbcontent1');
                        if (alt && txt(alt)) div = alt;
                    }
                    var loc = locate(td) || { day: 0, rowHeader: '', rowOrdinal: -1 };
                    var day = loc.day;
                    // 强智 id 格式：{节次行哈希}-{星期}-{1简略|2详细}，仅用于兜底星期，绝不用于推断节次
                    var idParts = String(div.id || '').split('-');
                    if (!day && idParts.length >= 3) {
                        var idDay = parseInt(idParts[idParts.length - 2]);
                        if (idDay >= 1 && idDay <= 7) day = idDay;
                    }
                    var segs = splitCellSegments(div.innerHTML);
                    for (var si = 0; si < segs.length; si++) {
                        emit('qiangzhi', day, 0, 0, loc.rowHeader, loc.rowOrdinal, segs[si]);
                    }
                }

                // 2. 新正方：td.id = "星期-起始节次"，rowspan = 节数
                var zf = doc.querySelectorAll('.timetable_con');
                for (var zi = 0; zi < zf.length; zi++) {
                    var con = zf[zi];
                    var ztd = con.closest('td');
                    var zloc = locate(ztd) || { day: 0, rowHeader: '', rowOrdinal: -1, rs: 1 };
                    var zday = zloc.day;
                    var rowStart = 0, rowEnd = 0;
                    var m = String(ztd ? ztd.id : '').match(/^(\d)-(\d{1,2})/);
                    if (m) {
                        zday = parseInt(m[1]) || zday;
                        rowStart = parseInt(m[2]) || 0;
                        rowEnd = rowStart + Math.max(1, parseInt(ztd.rowSpan) || 1) - 1;
                    }
                    var zsegs = splitCellSegments(con.innerHTML);
                    for (var zsi = 0; zsi < zsegs.length; zsi++) {
                        emit('zhengfang', zday, rowStart, rowEnd, zloc.rowHeader, zloc.rowOrdinal, zsegs[zsi]);
                    }
                }

                // 3. 通用兜底：含「周」的课表格子
                if (out.length === before) {
                    var tds = doc.querySelectorAll('td');
                    for (var ti = 0; ti < tds.length; ti++) {
                        var gtd = tds[ti];
                        if (gtd.querySelector('table')) continue;
                        var t = txt(gtd);
                        if (t.length < 4 || t.indexOf('周') < 0) continue;
                        var gloc = locate(gtd);
                        if (!gloc || !gloc.day) continue;
                        var gsegs = splitCellSegments(gtd.innerHTML);
                        for (var gi = 0; gi < gsegs.length; gi++) {
                            emit('generic', gloc.day, 0, 0, gloc.rowHeader, gloc.rowOrdinal, gsegs[gi]);
                        }
                    }
                }
            }
            return JSON.stringify(out);
        })();
    """.trimIndent()

    /**
     * 在教务系统页面注入的轻量 AJAX/Fetch 拦截脚本
     * 自动拦截 SPA 页面异步拉取的课表 JSON 数据，并通过 ClassTrackBridge 发送给 Android 端
     */
    val eduSystemAjaxHookJs: String = """
        (function() {
            if (window._classtrack_hook_installed) return;
            window._classtrack_hook_installed = true;

            function checkAndNotify(text) {
                if (!text || text.length < 30) return;
                if (text.indexOf('kcmc') >= 0 || text.indexOf('courseName') >= 0 || text.indexOf('kbList') >= 0 || text.indexOf('lessonList') >= 0) {
                    if (window.ClassTrackBridge && window.ClassTrackBridge.onCaptureTimetableJson) {
                        window.ClassTrackBridge.onCaptureTimetableJson(text);
                    }
                }
            }

            var origOpen = XMLHttpRequest.prototype.open;
            var origSend = XMLHttpRequest.prototype.send;
            XMLHttpRequest.prototype.open = function(method, url) {
                this._ct_url = url;
                return origOpen.apply(this, arguments);
            };
            XMLHttpRequest.prototype.send = function(body) {
                this.addEventListener('load', function() {
                    try {
                        checkAndNotify(this.responseText);
                    } catch(e) {}
                });
                return origSend.apply(this, arguments);
            };

            if (window.fetch) {
                var origFetch = window.fetch;
                window.fetch = function() {
                    return origFetch.apply(this, arguments).then(function(res) {
                        try {
                            var clone = res.clone();
                            clone.text().then(function(t) {
                                checkAndNotify(t);
                            }).catch(function(){});
                        } catch(e) {}
                        return res;
                    });
                };
            }
        })();
    """.trimIndent()
}
