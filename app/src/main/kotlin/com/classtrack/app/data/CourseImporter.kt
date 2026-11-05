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
            // 先尝试教务接口常见的外层数组格式
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

        // 先剥离显式周次干扰（覆盖 "1-16周"、"2-12(周)"、"2,6,8-9,12-18(周)"、"[01-16周]"、"(1-8单周)" 等所有变体），
        // 严防在复合文本中误将周次数字（如 2-12(周)）当作节次！
        val sanitized = trimmed
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
        val listMatch = Regex("(?:第|[\\[(（【])?\\s*(\\d{1,2}(?:\\s*(?:,|-|~)\\s*\\d{1,2}){2,})\\s*(?:节|[\\])）】])").find(sanitized)
        if (listMatch != null) {
            val nums = Regex("\\d+").findAll(listMatch.groupValues[1]).mapNotNull { it.value.toIntOrNull() }.filter { it in 1..18 }.toList()
            if (nums.isNotEmpty()) {
                return Pair(nums.minOrNull()!!, nums.maxOrNull()!!)
            }
        }

        // 3. 匹配明确标注 "节" 或带有括号区间的节次范围，例如：
        // [01-03节], 第1-3节, 1-3节, 1~3节, (1-2), (01-03), [1-3], (1-3节)
        val explicitRange = Regex("(?:第|[\\[(（【])?\\s*(\\d{1,2})\\s*(?:-|~|,)\\s*(\\d{1,2})\\s*(?:节|[\\])）】])").find(sanitized)
        if (explicitRange != null) {
            val start = explicitRange.groupValues[1].toIntOrNull()
            val end = explicitRange.groupValues[2].toIntOrNull()
            if (start != null && end != null && start in 1..18 && end in 1..18) {
                return Pair(minOf(start, end), maxOf(start, end))
            }
        }

        // 4. 匹配单节，如：第3节, 3节, [03节], [03], (03)
        val singleMatch = Regex("(?:第|[\\[(（【])?\\s*(\\d{1,2})\\s*(?:节|[\\])）】])").find(sanitized)
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

        // 剥离显式节次信息（如 "9-10节", "第1-2节", "[01-02节]", "1-2大节"）
        val withoutSections = text
            .replace(Regex("(?:第|\\[)?\\d{1,2}\\s*(?:-|~|至|到)\\s*\\d{1,2}\\s*(?:节|\\])"), " ")
            .replace(Regex("(?:第|\\[)?\\d{1,2}(?:(?:,|-)\\d{1,2})+\\s*(?:节|\\])"), " ")
            .replace(Regex("第?\\d{1,2}大节"), " ")
            .replace(Regex("(?:第|\\[)?\\d{1,2}\\s*(?:节|\\])"), " ")

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
            // 将课程放入可变池，执行多轮收敛合并 (Fixpoint Multi-Pass Merge)
            val pool = group.toMutableList()
            var modified = true

            while (modified) {
                modified = false
                outer@ for (i in 0 until pool.size) {
                    for (j in i + 1 until pool.size) {
                        val a = pool[i]
                        val b = pool[j]

                        val aWeeks = getActiveWeeks(a)
                        val bWeeks = getActiveWeeks(b)

                        // 场景 A：完全相同的节次，检查是否为不同周次的同门课延续（如 1-8 周 + 9-16 周）
                        val isSameSection = (a.startSection == b.startSection && a.endSection == b.endSection)

                        // 场景 B：节次相邻或重叠（如 1-2 节 + 3 节合并为 1-3 节；1-2 节 + 3-4 节；1-3 节 + 2-3 节去重）
                        val isSectionAdjacentOrOverlapping =
                            (a.startSection <= b.endSection + 1) && (b.startSection <= a.endSection + 1)

                        if (!isSameSection && !isSectionAdjacentOrOverlapping) continue

                        // 教师与教室兼容性校验
                        val roomCompatible = a.room.isBlank() || a.room == "待定教室" ||
                            b.room.isBlank() || b.room == "待定教室" ||
                            a.room == b.room
                        val teacherCompatible = a.teacher.isBlank() || b.teacher.isBlank() ||
                            a.teacher == b.teacher

                        if (!roomCompatible || !teacherCompatible) continue

                        // 周次兼容性校验：
                        // 1. 如果是不同节次相邻合并（加长课缝合），两门课在学期中的活跃周次必须完全一致（aWeeks == bWeeks）！
                        //    严禁将 1-16 周的 1-2 节与 1-8 周的 3-4 节错误合并为全学期 1-4 节，否则在 9-16 周会凭空产生幽灵课程！
                        // 2. 如果是相同节次跨周延续（如 1-8 周 + 9-16 周），两者的周次可相接或有交集进行周次延展。
                        if (!isSameSection) {
                            if (aWeeks.isEmpty() || bWeeks.isEmpty() || aWeeks != bWeeks) {
                                continue
                            }

                            // 单双周属性校验
                            if (a.weekParity != b.weekParity &&
                                a.weekParity != WeekParity.ALL &&
                                b.weekParity != WeekParity.ALL
                            ) {
                                continue
                            }
                        } else {
                            // 同节次跨周合并：无论是连续周还是离散周次（如 1-8 周 + 10-16 周跳周），只要属于同星期、同节次且教室教师兼容，均收敛合并为一个完整的离散周次课条
                        }

                        // 命中合并规则！
                        var mergedStartSec = minOf(a.startSection, b.startSection)
                        var mergedEndSec = maxOf(a.endSection, b.endSection)

                        // 专门处理 3 节加长课场景：
                        // 如果教务系统排成 [1-2] 和 [3-4]（两个2节课），或 [5-6] 和 [7-8]，但课程名或备注提示为 3 节/3学时/1-3节等，
                        // 且合并后的总跨度超过 3 节，纠正结束节次为 mergedStartSec + 2（精准 3 节），彻底杜绝变成 4 节！
                        val isThreeSectionHint = hasThreeSectionHint(a.name, b.name, a.note, b.note)
                        if (isThreeSectionHint && (mergedEndSec - mergedStartSec + 1) > 3) {
                            mergedEndSec = mergedStartSec + 2
                        }

                        val mergedStartWeek = minOf(a.startWeek, b.startWeek)
                        val mergedEndWeek = maxOf(a.endWeek, b.endWeek)

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
                        val preferredParity = when {
                            a.weekParity != WeekParity.ALL -> a.weekParity
                            b.weekParity != WeekParity.ALL -> b.weekParity
                            else -> WeekParity.ALL
                        }

                        val unionWeeks = (aWeeks union bWeeks).sorted()
                        val combinedWeeksSpec = if (unionWeeks.isNotEmpty()) {
                            compressWeeksList(unionWeeks)
                        } else {
                            listOf(a.weeksSpec, b.weeksSpec)
                                .filter { it.isNotBlank() }
                                .flatMap { it.split(",") }
                                .mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() } }
                                .distinct()
                                .joinToString(",")
                        }

                        val preferredName = if (a.name.length >= b.name.length) a.name else b.name

                        val mergedCourse = a.copy(
                            name = preferredName,
                            room = preferredRoom,
                            teacher = preferredTeacher,
                            startSection = mergedStartSec,
                            endSection = mergedEndSec,
                            startWeek = mergedStartWeek,
                            endWeek = mergedEndWeek,
                            weeksSpec = combinedWeeksSpec,
                            weekParity = preferredParity
                        )

                        pool.removeAt(j)
                        pool[i] = mergedCourse
                        modified = true
                        break@outer
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
     * 注入高校教务系统内置 WebView 的通用课程提取 JS 脚本
     * 深度适配：
     * 1. 强智教务系统 (div.kbcontent / div.kbcontent1，支持跨多层 iframe 穿透、离散周次与隐藏标签提取)
     * 2. 新正方教务系统 (.timetable_con / #kbgrid_table_0)
     * 3. 青果/树维/URP 及通用高校 HTML 课表表格智能识别
     */
    val eduSystemExtractJs: String = """
        (function() {
            var courses = [];
            var docs = [];
            function collectDocs(rootDoc) {
                if (!rootDoc || docs.indexOf(rootDoc) >= 0) return;
                docs.push(rootDoc);
                try {
                    var frames = rootDoc.querySelectorAll('iframe, frame');
                    for (var i = 0; i < frames.length; i++) {
                        try {
                            var sub = frames[i].contentDocument || (frames[i].contentWindow && frames[i].contentWindow.document);
                            if (sub) collectDocs(sub);
                        } catch (e1) {}
                    }
                } catch (e) {}
            }
            collectDocs(document);

            function htmlToLines(html) {
                var text = (html || '')
                    .replace(/<br\s*\/?>/gi, '\n')
                    .replace(/<\/(font|div|p|li|tr|span)>/gi, '\n')
                    .replace(/<[^>]+>/g, ' ')
                    .replace(/&nbsp;/gi, ' ');
                return text.split('\n').map(function(s){ return s.trim(); }).filter(Boolean);
            }

            function extractFontByTitle(rawHtml, keywords) {
                if (!rawHtml || rawHtml.indexOf('<') < 0) return '';
                try {
                    var temp = document.createElement('div');
                    temp.innerHTML = rawHtml;
                    var nodes = temp.querySelectorAll('font[title], span[title], p[title], div[title]');
                    for (var i = 0; i < nodes.length; i++) {
                        var t = (nodes[i].getAttribute('title') || '').trim();
                        for (var k = 0; k < keywords.length; k++) {
                            if (t.indexOf(keywords[k]) >= 0) {
                                var val = (nodes[i].innerText || nodes[i].textContent || '').trim();
                                if (val) return val;
                            }
                        }
                    }
                } catch (e) {}
                return '';
            }

            function parseBlock(rawHtmlOrText, dayOfWeek, fallbackStartSec) {
                var lines = htmlToLines(rawHtmlOrText);
                if (lines.length === 0) return;
                var name = lines[0].replace(/^[◆★●▪\s]+/, '').trim();
                if (!name || name.length < 2 || name === '&nbsp;') return;

                var fullText = lines.join('\n');
                var teacher = extractFontByTitle(rawHtmlOrText, ['老师', '教师', '任课']);
                var room = extractFontByTitle(rawHtmlOrText, ['教室', '地点', '场地', '教学楼', '位置']) || '待定教室';
                var weekSecFont = extractFontByTitle(rawHtmlOrText, ['周次', '节次', '时间']);

                var startSec = fallbackStartSec || 1;
                var endSec = startSec + 1;
                var startW = 1;
                var endW = 18;
                var weeksSpec = '';
                var parity = 'ALL';

                var secSearchTarget = weekSecFont ? (weekSecFont + '\n' + fullText) : fullText;
                // 先剥离所有周次表达式（如 2-12(周)、8-12(周)、1-16周、2,6,8-9(单周)），严防误将周次数字当作节次！
                var textWithoutWeeks = secSearchTarget
                    .replace(/\[[^\]]*周[^\]]*\]/g, ' ')
                    .replace(/\d{1,2}(?:\s*[-~,，、至到]\s*\d{1,2})*\s*(?:\([单双]?周\)|（[单双]?周）|[单双]?周)/g, ' ');

                // 严格匹配带“节”字的表达式，支持 [01-02节]、[01-02-03节]、1-3节、第1,2,3节、第3节
                var explicitSecMatch = textWithoutWeeks.match(/(?:第|\[|【|\(|（)?\s*(\d{1,2}(?:\s*[-~,，至到]\s*\d{1,2})*)\s*节\s*(?:\]|】|\)|）)?/);
                if (explicitSecMatch) {
                    var sNums = explicitSecMatch[1].match(/\d+/g);
                    if (sNums && sNums.length > 0) {
                        var firstS = parseInt(sNums[0]);
                        var lastS = parseInt(sNums[sNums.length - 1]);
                        if (firstS >= 1 && firstS <= 18 && lastS >= 1 && lastS <= 18) {
                            startSec = Math.min(firstS, lastS);
                            endSec = Math.max(firstS, lastS);
                        }
                    }
                } else {
                    // 匹配方括号内纯节次格式，如 [01-02] 或 [01-02-03]（已剥离周次）
                    var bracketSecMatch = textWithoutWeeks.match(/\[(\d{1,2}(?:-\d{1,2})+)\]/);
                    if (bracketSecMatch) {
                        var bNums = bracketSecMatch[1].match(/\d+/g);
                        if (bNums && bNums.length > 0) {
                            var bs = parseInt(bNums[0]);
                            var be = parseInt(bNums[bNums.length - 1]);
                            if (bs >= 1 && bs <= 18 && be >= 1 && be <= 18) {
                                startSec = Math.min(bs, be);
                                endSec = Math.max(bs, be);
                            }
                        }
                    }
                }

                var isThreeHint = /(?<!第)(?:共|连上)?(?:3|3\.0|三)\s*(?:节|学时|课时)/.test(fullText) ||
                    /[\(\[【（](?:3|3\.0|三)\s*(?:节|学时|课时)[\)\]】）]/.test(fullText) ||
                    /(?:^|[^\d])(?:1-3|01-03|3-5|03-05|5-7|05-07|6-8|06-08|7-9|07-09|8-10|9-11)\s*节(?!周)/.test(fullText) ||
                    /[\(\[【（](?:1-3|01-03|3-5|03-05|5-7|05-07|6-8|06-08|7-9|07-09|8-10|9-11)(?:节)?[\)\]】）](?!周)/.test(fullText);
                if (isThreeHint && (endSec - startSec + 1) > 3) {
                    endSec = startSec + 2;
                }

                // 匹配离散或连续周次，例如 2,6,8-9,12-18(周) 或 2-12(周) 或 1-16周
                var weekSpecMatch = secSearchTarget.match(/([0-9]+(?:\s*[-~,，、]\s*[0-9]+)*)\s*(?:\([单双]?周\)|（[单双]?周）|[单双]?周)/);
                if (weekSpecMatch) {
                    weeksSpec = weekSpecMatch[1].replace(/[，、]/g, ',').replace(/\s+/g, '').trim();
                    var nums = weeksSpec.match(/\d+/g);
                    if (nums && nums.length > 0) {
                        var minW = 99, maxW = 1;
                        for (var wIdx = 0; wIdx < nums.length; wIdx++) {
                            var wn = parseInt(nums[wIdx]);
                            if (wn >= 1 && wn <= 40) {
                                if (wn < minW) minW = wn;
                                if (wn > maxW) maxW = wn;
                            }
                        }
                        if (minW <= maxW) {
                            startW = minW;
                            endW = maxW;
                        }
                    }
                }

                if (secSearchTarget.indexOf('单周') >= 0 || secSearchTarget.indexOf('(单)') >= 0 || secSearchTarget.indexOf('（单）') >= 0) parity = 'ODD';
                else if (secSearchTarget.indexOf('双周') >= 0 || secSearchTarget.indexOf('(双)') >= 0 || secSearchTarget.indexOf('（双）') >= 0) parity = 'EVEN';

                for (var k = 1; k < lines.length; k++) {
                    var ln = lines[k];
                    if (ln.indexOf('周') >= 0 || ln.indexOf('节') >= 0 || ln.indexOf('学时') >= 0 || ln.indexOf('学分') >= 0) continue;
                    var looksLikeRoom = ln.indexOf('楼') >= 0 || ln.indexOf('教') >= 0 || ln.indexOf('室') >= 0 ||
                        ln.indexOf('馆') >= 0 || ln.indexOf('场') >= 0 || ln.indexOf('区') >= 0 ||
                        ln.indexOf('中心') >= 0 || ln.indexOf('机房') >= 0 || /[A-Za-z0-9]{2,}/.test(ln);
                    if (!teacher && !looksLikeRoom && ln.length <= 12) {
                        teacher = ln;
                    } else if (room === '待定教室' && ln !== teacher) {
                        room = ln;
                    }
                }

                courses.push({
                    name: name,
                    room: room,
                    teacher: teacher,
                    dayOfWeek: dayOfWeek,
                    startSection: startSec,
                    endSection: endSec,
                    startWeek: startW,
                    endWeek: endW,
                    weeksSpec: weeksSpec,
                    parity: parity
                });
            }

            function detectColToDayMap(table) {
                var map = {};
                if (!table) return map;
                var headerRows = table.querySelectorAll('tr');
                for (var r = 0; r < Math.min(headerRows.length, 3); r++) {
                    var cells = headerRows[r].querySelectorAll('th, td');
                    for (var c = 0; c < cells.length; c++) {
                        var txt = (cells[c].innerText || cells[c].textContent || '').trim();
                        if (txt.indexOf('一') >= 0 || txt.indexOf('Mon') >= 0) map[c] = 1;
                        else if (txt.indexOf('二') >= 0 || txt.indexOf('Tue') >= 0) map[c] = 2;
                        else if (txt.indexOf('三') >= 0 || txt.indexOf('Wed') >= 0) map[c] = 3;
                        else if (txt.indexOf('四') >= 0 || txt.indexOf('Thu') >= 0) map[c] = 4;
                        else if (txt.indexOf('五') >= 0 || txt.indexOf('Fri') >= 0) map[c] = 5;
                        else if (txt.indexOf('六') >= 0 || txt.indexOf('Sat') >= 0) map[c] = 6;
                        else if (txt.indexOf('日') >= 0 || txt.indexOf('天') >= 0 || txt.indexOf('七') >= 0 || txt.indexOf('Sun') >= 0) map[c] = 7;
                    }
                }
                return map;
            }

            function detectRowStartSec(tr, cellId) {
                // 优先从强智 cell.id 提取: 常见格式 "xxx-4-2" 或 "1-2-1"
                if (cellId) {
                    var idParts = cellId.split('-');
                    if (idParts.length >= 3) {
                        var lastPart = parseInt(idParts[idParts.length - 1]);
                        if (lastPart >= 1 && lastPart <= 6) {
                            return (lastPart - 1) * 2 + 1;
                        }
                    }
                }
                if (!tr) return 1;
                // 检查本行首列文本（如 "0102"、"01-02"、"第一大节"、"1-2节"）
                var firstCell = tr.querySelector('th, td');
                if (firstCell) {
                    var ht = (firstCell.innerText || firstCell.textContent || '').trim();
                    var m2 = ht.match(/0?(\d{1,2})\s*[-~]?\s*0?(\d{1,2})/);
                    if (m2) {
                        var s1 = parseInt(m2[1]);
                        if (s1 >= 1 && s1 <= 15) return s1;
                    }
                    if (ht.indexOf('一') >= 0) return 1;
                    if (ht.indexOf('二') >= 0) return 3;
                    if (ht.indexOf('三') >= 0) return 5;
                    if (ht.indexOf('四') >= 0) return 7;
                    if (ht.indexOf('五') >= 0) return 9;
                    if (ht.indexOf('六') >= 0) return 11;
                }
                if (tr.parentElement) {
                    var trs = Array.from(tr.parentElement.querySelectorAll('tr'));
                    var rowIdx = trs.indexOf(tr);
                    if (rowIdx > 0 && rowIdx <= 7) return (rowIdx - 1) * 2 + 1;
                }
                return 1;
            }

            for (var d = 0; d < docs.length; d++) {
                var doc = docs[d];
                // 1. 强智教务系统：优先只提取详细层 div.kbcontent（内含完整节次、老师、教室），
                // 仅当页面不存在 div.kbcontent 时才回退提取简略层 div.kbcontent1，彻底防止双层重复提取与无教室残缺数据污染！
                var qzCells = doc.querySelectorAll('div.kbcontent');
                if (!qzCells || qzCells.length === 0) {
                    qzCells = doc.querySelectorAll('div.kbcontent1');
                }
                if (qzCells && qzCells.length > 0) {
                    qzCells.forEach(function(cell, idx) {
                        var idStr = cell.id || '';
                        var td = cell.closest('td');
                        var tr = cell.closest('tr');
                        var table = cell.closest('table');
                        var colToDay = detectColToDayMap(table);
                        var day = (idx % 7) + 1;

                        if (td && tr) {
                            var tds = Array.from(tr.querySelectorAll('td'));
                            var colIdx = tds.indexOf(td);
                            if (colToDay[colIdx]) {
                                day = colToDay[colIdx];
                            } else if (colIdx >= 1 && colIdx <= 7) {
                                day = colIdx;
                            }
                        }

                        var idParts = idStr.split('-');
                        if (idParts.length >= 3) {
                            var parsedDay = parseInt(idParts[idParts.length - 2]);
                            if (parsedDay >= 1 && parsedDay <= 7) day = parsedDay;
                        }

                        var fallbackStartSec = detectRowStartSec(tr, idStr);
                        var html = cell.innerHTML || '';
                        var segments = html.split(/-----+/);
                        segments.forEach(function(seg) {
                            if (seg && seg.replace(/&nbsp;/g, '').trim().length >= 2) {
                                parseBlock(seg, day, fallbackStartSec);
                            }
                        });
                    });
                }

                // 2. 新正方教务系统 (.timetable_con / #kbgrid_table_0)
                var zfCells = doc.querySelectorAll('.timetable_con');
                if (zfCells && zfCells.length > 0) {
                    zfCells.forEach(function(cell, idx) {
                        var td = cell.closest('td');
                        var tr = cell.closest('tr');
                        var table = cell.closest('table');
                        var colToDay = detectColToDayMap(table);
                        var idAttr = td ? (td.id || '') : '';
                        var day = (idx % 7) + 1;

                        if (td && tr) {
                            var tds = Array.from(tr.querySelectorAll('td'));
                            var colIdx = tds.indexOf(td);
                            if (colToDay[colIdx]) {
                                day = colToDay[colIdx];
                            }
                        }
                        if (idAttr.indexOf('-') > 0) {
                            day = parseInt(idAttr.split('-')[0]) || day;
                        }
                        var fallbackStartSec = detectRowStartSec(tr, idAttr);
                        var html = cell.innerHTML || '';
                        if (html.trim().length >= 2) {
                            parseBlock(html, day, fallbackStartSec);
                        }
                    });
                }

                // 3. 通用高校课表 <td> 兜底扫描（当未命中特定 class 时）
                if (courses.length === 0) {
                    var allTds = doc.querySelectorAll('td');
                    allTds.forEach(function(td, idx) {
                        var text = (td.innerText || td.textContent || '').trim();
                        if (text.length >= 6 && text.indexOf('周') >= 0 && text.indexOf('节') >= 0) {
                            var tr = td.closest('tr');
                            var table = td.closest('table');
                            var colToDay = detectColToDayMap(table);
                            var day = 1;
                            if (tr) {
                                var tds = Array.from(tr.querySelectorAll('td'));
                                var colIdx = tds.indexOf(td);
                                if (colToDay[colIdx]) {
                                    day = colToDay[colIdx];
                                } else if (colIdx >= 1 && colIdx <= 7) {
                                    day = colIdx;
                                }
                            }
                            var fallbackStartSec = detectRowStartSec(tr, td.id || '');
                            var segs = (td.innerHTML || text).split(/-----+/);
                            segs.forEach(function(seg) {
                                parseBlock(seg, day, fallbackStartSec);
                            });
                        }
                    });
                }
            }
            return JSON.stringify(courses);
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
