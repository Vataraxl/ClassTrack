package com.classtrack.app.data

import com.classtrack.app.data.model.CourseItem
import com.classtrack.app.data.model.WeekParity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 教务网页课表「原始格子」解析器。
 *
 * 职责划分：
 * - WebView 注入的 JS（[CourseImporter.eduSystemExtractJs]）只负责 DOM 层面的工作：
 *   计算每个课程格子所在的「星期列」与「节次行」（含 rowspan / colspan 视觉网格还原），
 *   并原样输出格子文本与带 title 的字段，不做任何语义猜测。
 * - 本解析器负责全部语义解析（课程名 / 教师 / 教室 / 周次 / 节次），纯 Kotlin 实现，可单元测试。
 *
 * 节次判定优先级：
 * 1. 格子文本中显式的节次（如 `[01-02节]`、`(3-5节)`），且必须与所在行的节次范围有交集；
 * 2. 所在行的行头文本（如 `第一大节(01,02小节)`、`0102`、`1-2节`、`第3大节`）；
 * 3. JS 直接给出的绝对节次（新正方 td.id = "星期-节次" + rowspan）；
 * 4. 行序号兜底（第 n 个课程行 → 第 2n-1、2n 节）。
 *
 * 注意：绝不使用强智 div.kbcontent 的 id 末段推断节次——
 * 其格式为 `{节次行哈希}-{星期}-{1|2}`，末段 1/2 只代表简略层/详细层，旧版据此推算导致所有课程被挤到第 3-4 节。
 */
object TimetableCellParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Serializable
    data class RawField(
        val t: String = "",
        val v: String = ""
    )

    @Serializable
    data class RawTimetableCell(
        val kind: String = "",
        val source: String = "",
        val day: Int = 0,
        /** 绝对起始节次（仅当 JS 可直接确定时 > 0，如新正方 td.id） */
        val rowStart: Int = 0,
        val rowEnd: Int = 0,
        /** 所在行左侧行头文本（如 "上午 第一大节 (01,02小节) 08:00-09:35"） */
        val rowHeader: String = "",
        /** 在课程行中的序号（从 0 开始，不含表头行），-1 表示未知 */
        val rowOrdinal: Int = -1,
        /** 单门课程的纯文本（已按 <br> 拆行） */
        val text: String = "",
        val fields: List<RawField> = emptyList()
    )

    /** 快速判断字符串是否为 JS 输出的原始格子数组 */
    fun looksLikeCellPayload(raw: String): Boolean {
        val t = raw.trim()
        return t.startsWith("[") && t.contains("\"kind\"") && t.contains("\"cell\"")
    }

    fun decodeCells(raw: String): List<RawTimetableCell> = runCatching {
        json.decodeFromString<List<RawTimetableCell>>(raw.trim())
    }.getOrDefault(emptyList()).filter { it.kind == "cell" }

    /** 解析 JS 输出的原始格子并合并为最终课程列表 */
    fun parse(raw: String): List<CourseItem> = parseCells(decodeCells(raw))

    private val blockSeparatorLineRegex = Regex("^\\s*(?:-{2,}|={2,}|─{2,}|—{2,}|\\*{3,})\\s*$")

    fun parseCells(cells: List<RawTimetableCell>): List<CourseItem> {
        val parsed = cells
            .flatMap { cell -> expandSubCells(cell) }
            .mapNotNull { subCell -> parseCell(subCell) }
        return CourseImporter.mergeAndSanitizeCourses(parsed)
    }

    /**
     * 将同一个格子内以 `--` / `----` / `====` 分隔的多门课程拆分为独立子格子，
     * 防止第一门课吞并后续课程的周次/教室并导致同格第 2、3 门课丢失。
     */
    private fun expandSubCells(cell: RawTimetableCell): List<RawTimetableCell> {
        val rawLines = cell.text.lines()
        val blocks = mutableListOf<MutableList<String>>()
        var current = mutableListOf<String>()
        for (line in rawLines) {
            if (blockSeparatorLineRegex.matches(line)) {
                if (current.isNotEmpty()) {
                    blocks.add(current)
                    current = mutableListOf()
                }
            } else if (line.isNotBlank()) {
                current.add(line)
            }
        }
        if (current.isNotEmpty()) {
            blocks.add(current)
        }
        if (blocks.size <= 1) return listOf(cell)

        return blocks.map { blockLines ->
            val blockText = blockLines.joinToString("\n")
            val scopedFields = cell.fields.filter { f ->
                val v = f.v.trim()
                v.isNotEmpty() && blockLines.any { line -> line.contains(v) || v.contains(line.trim()) }
            }
            cell.copy(
                text = blockText,
                fields = scopedFields
            )
        }
    }

    // ---------------------------------------------------------------------
    // 单格解析
    // ---------------------------------------------------------------------

    private val nameTitleKeys = listOf("课程名称", "课程名", "课名")
    private val teacherTitleKeys = listOf("老师", "教师", "任课", "授课", "主讲")
    private val roomTitleKeys = listOf("教室", "地点", "场地", "位置", "教学楼", "上课地点", "校区")
    private val timeTitleKeys = listOf("周次", "节次", "节/周", "上课时间", "时间")
    private val nonNameTitleKeys = teacherTitleKeys + roomTitleKeys + timeTitleKeys + listOf(
        "教学班", "班级", "学分", "学时", "考核", "属性", "性质", "类别", "周学时", "选课", "备注", "人数", "分组"
    )

    private val weekExprRegex = Regex("\\d{1,2}(?:\\s*[-~～,，、]\\s*\\d{1,2})*\\s*(?:\\([单双]?周\\)|（[单双]?周）|[单双]?周)")
    private val sectionHintRegex = Regex(
        "\\d{1,2}\\s*(?:[-~～,，、]\\s*\\d{1,2})*\\s*(?:小)?节" +
            "|\\[\\s*\\d{1,2}(?:\\s*[-~～,，]\\s*\\d{1,2})+\\s*\\]" +
            "|\\[\\s*(?:0[1-9]|1[0-8]){2,6}\\s*(?:小?节)?\\s*\\]"
    )
    private val leadingSymbolRegex = Regex("^[◆★☆●○■□▪▲△◇※#*\\s]+")
    private val trailingSymbolRegex = Regex("[◆★☆●○■□▪▲△◇※*\\s]+$")
    private val redundantTrailingTagRegex = Regex("(?:[\\(（\\[【](?:必修|选修|公选|校选|通选|专选|限选|任选|考查|考试|线上|线下)[\\)）\\]】])+$")
    private val roomLikeRegex = Regex("(?:楼|教|室|馆|场|区|中心|机房|实验|厅|院|看台|[A-Za-z]\\d|\\d{3,})")

    fun parseCell(cell: RawTimetableCell): CourseItem? {
        val day = cell.day
        if (day !in 1..7) return null

        val lines = cell.text.lines()
            .map { it.replace('\u00a0', ' ').replace(Regex("\\s+"), " ").trim() }
            .filter { it.isNotEmpty() && !it.matches(Regex("[-=_*\\s]+")) }
        if (lines.isEmpty()) return null

        val nameField = fieldValue(cell.fields, nameTitleKeys)
        val teacherField = fieldValue(cell.fields, teacherTitleKeys)
        val roomField = fieldValue(cell.fields, roomTitleKeys)
        val timeField = fieldValue(cell.fields, timeTitleKeys)
        val nonNameFieldValues = cell.fields
            .filter { f -> nonNameTitleKeys.any { k -> f.t.contains(k) } }
            .map { it.v.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        // 1. 课程名：优先读取显式课程名字段，否则取首个既不是非课名字段值、也不像周次/节次/元数据的行
        val nameIdx = if (!nameField.isNullOrBlank()) {
            lines.indexOfFirst { it == nameField || it.contains(nameField) }.takeIf { it >= 0 } ?: 0
        } else {
            lines.indexOfFirst { line ->
                line !in nonNameFieldValues && !isTimeLine(line) && !isMetaNoiseLine(line) && line.length >= 2
            }
        }
        if (nameIdx < 0) return null
        val rawName = if (!nameField.isNullOrBlank() && lines.getOrNull(nameIdx)?.contains(nameField) != true) {
            nameField
        } else {
            lines[nameIdx]
        }
        val name = cleanDisplayCourseName(rawName)
        if (name.length < 2) return null

        // 2. 时间文本（周次 + 节次）
        val timeLines = lines.filterIndexed { i, l -> i != nameIdx && isTimeLine(l) }
        val timeText = listOfNotNull(timeField, timeLines.joinToString(" ").takeIf { it.isNotBlank() })
            .distinct()
            .joinToString(" ")

        // 3. 教师与教室：字段优先，其次启发式
        var teacher = teacherField.orEmpty()
        var room = roomField.orEmpty()
        if (teacher.isEmpty() || room.isEmpty()) {
            lines.forEachIndexed { i, line ->
                if (i == nameIdx || line in nonNameFieldValues || isTimeLine(line) || isMetaNoiseLine(line)) return@forEachIndexed
                val roomLike = roomLikeRegex.containsMatchIn(line)
                if (room.isEmpty() && roomLike) {
                    room = line
                } else if (teacher.isEmpty() && !roomLike && line.length in 2..12 && !line.any { it.isDigit() }) {
                    teacher = line
                }
            }
        }

        // 4. 周次（直接传入完整 timeText，由 CourseImporter.parseWeekInfo 剥离节次并保留后置 (单)/(双) 标记）
        val hasWeekInfo = timeText.contains("周") || timeText.contains("单") || timeText.contains("双")
        val weeks = if (hasWeekInfo) CourseImporter.parseWeekInfo(timeText) else null

        // 5. 节次
        val rowRange = resolveRowSections(cell)
        val explicitRange = extractExplicitSections(timeText, lines.filterIndexed { i, _ -> i != nameIdx })
        val sections = when {
            explicitRange != null && rowRange == null -> explicitRange
            explicitRange != null && rowRange != null && rangesOverlap(explicitRange, rowRange) -> explicitRange
            rowRange != null -> rowRange
            else -> return null
        }

        return CourseItem(
            name = name,
            room = room.removePrefix("@").trim().ifBlank { "待定教室" },
            teacher = teacher.trim(),
            dayOfWeek = day,
            startSection = sections.first.coerceIn(1, 18),
            endSection = sections.second.coerceIn(sections.first.coerceIn(1, 18), 18),
            startWeek = weeks?.startWeek ?: 1,
            endWeek = weeks?.endWeek ?: 18,
            weeksSpec = weeks?.weeksSpec.orEmpty(),
            weekParity = weeks?.parity ?: WeekParity.ALL
        )
    }

    private fun cleanDisplayCourseName(raw: String): String {
        val base = raw
            .replace(leadingSymbolRegex, "")
            .replace(trailingSymbolRegex, "")
            .replace("（", "(")
            .replace("）", ")")
            .trim()
        val stripped = base.replace(redundantTrailingTagRegex, "").trim()
        return if (stripped.length >= 2) stripped else base
    }

    private fun fieldValue(fields: List<RawField>, keys: List<String>): String? =
        fields.firstOrNull { f -> keys.any { k -> f.t.contains(k) } && f.v.isNotBlank() }?.v?.trim()

    private fun isTimeLine(line: String): Boolean =
        weekExprRegex.containsMatchIn(line) || sectionHintRegex.containsMatchIn(line) ||
            line.contains("单周") || line.contains("双周")

    private fun isMetaNoiseLine(line: String): Boolean =
        line.contains("学分") || line.contains("学时") || line.startsWith("教学班") || line.startsWith("选课备注")

    private fun rangesOverlap(a: Pair<Int, Int>, b: Pair<Int, Int>): Boolean =
        a.first <= b.second && b.first <= a.second

    /** 只接受带「节」字或明确方括号节次（如 [01-02]、[0102]）的表达，避免把教室号、周次误当节次 */
    private fun extractExplicitSections(timeText: String, otherLines: List<String>): Pair<Int, Int>? {
        val candidates = buildList {
            if (timeText.isNotBlank()) add(timeText)
            addAll(otherLines.filter { it.contains("节") || sectionHintRegex.containsMatchIn(it) })
        }
        for (c in candidates) {
            if (!c.contains("节") && !sectionHintRegex.containsMatchIn(c)) continue
            val range = CourseImporter.parseSectionRange(c) ?: continue
            if (range.first in 1..18 && range.second in range.first..18) return range
        }
        return null
    }

    // ---------------------------------------------------------------------
    // 行 → 节次
    // ---------------------------------------------------------------------

    fun resolveRowSections(cell: RawTimetableCell): Pair<Int, Int>? {
        if (cell.rowStart in 1..18) {
            return cell.rowStart to cell.rowEnd.coerceIn(cell.rowStart, 18)
        }
        parseRowHeader(cell.rowHeader, cell.rowOrdinal)?.let { return it }
        if (cell.rowOrdinal >= 0) {
            val start = cell.rowOrdinal * 2 + 1
            if (start in 1..17) return start to start + 1
        }
        return null
    }

    private val chineseNumerals = mapOf(
        "一" to 1, "二" to 2, "三" to 3, "四" to 4, "五" to 5, "六" to 6,
        "七" to 7, "八" to 8, "九" to 9, "十" to 10, "十一" to 11, "十二" to 12
    )

    /**
     * 解析行头文本为节次范围（深度兼容北京林业大学等强智教务 "12节 / 34节 / 5节 / 67节 / 89节 / 1011节 / 12节" 非补零连写格式）：
     * - "12节" (rowOrdinal=0/1) → 1..2；"12节" (rowOrdinal>=4) → 12..12
     * - "34节" / "67节" / "89节" → 3..4 / 6..7 / 8..9
     * - "5节" → 5..5
     * - "1011节" / "0102" / "010203" / "091011" / "910节" → 10..11 / 1..2 / 1..3 / 9..11 / 9..10
     * - "第一大节 (01,02小节) 08:00-09:35" → 1..2
     * - "第三大节\n05,06,07" → 5..7
     * - "第2大节" → 3..4
     * - "第一大节 第二大节" (rowSpan 跨行合并) → 1..4
     * - "1-2节" / "第3节" / "第一节 第二节" / "3" → 1..2 / 3..3 / 1..2 / 3..3
     * - "上午"、"中午"、"午休"、"备注"、"实验实习安排" → null
     */
    fun parseRowHeader(raw: String, rowOrdinal: Int = -1): Pair<Int, Int>? {
        if (raw.isBlank()) return null
        // 去除时间（08:00、8：50）
        var s = raw.replace(Regex("\\d{1,2}\\s*[:：]\\s*\\d{2}"), " ")
        if (s.contains("备注") || s.contains("说明") || s.contains("合计") || s.contains("午休") || s.contains("安排")) {
            return null
        }

        // 1. 大节：优先用其后附带的小节编号，否则按「第 n 大节 = 2n-1、2n 节」换算（支持跨行合并的多个大节）
        val bigRegex = Regex("第?\\s*([0-9]{1,2}|十[一二]?|[一二三四五六七八九])\\s*大节")
        val bigMatches = bigRegex.findAll(s).toList()
        if (bigMatches.isNotEmpty()) {
            val rest = s.replace(bigRegex, " ")
            numbersRange(rest, rowOrdinal)?.let { return it }
            val bigNums = bigMatches.mapNotNull { m ->
                val token = m.groupValues[1]
                token.toIntOrNull() ?: chineseNumerals[token]
            }.filter { it in 1..9 }
            if (bigNums.isNotEmpty()) {
                val start = (bigNums.min() - 1) * 2 + 1
                val end = bigNums.max() * 2
                return start to end
            }
            return null
        }

        // 2. 中文单节 "第一节"、"第一节 第二节"
        val cnMatches = Regex("第\\s*(十[一二]?|[一二三四五六七八九])\\s*节").findAll(s)
            .mapNotNull { m -> chineseNumerals[m.groupValues[1]] }
            .filter { it in 1..18 }
            .toList()
        if (cnMatches.isNotEmpty()) {
            return cnMatches.min() to cnMatches.max()
        }

        s = s.replace("小节", " ")
        return numbersRange(s, rowOrdinal)
    }

    /**
     * 将行头中的数字 Token 解码为节次列表：
     * - 支持 "12" 消歧：首行/上午（rowOrdinal in 0..1）解码为 [1, 2]，末行/晚上（rowOrdinal >= 4 或含 "第12"）解码为 [12]
     * - 支持非补零双位连写："23"->[2,3], "34"->[3,4], "45"->[4,5], "56"->[5,6], "67"->[6,7], "78"->[7,8], "89"->[8,9]
     * - 支持多位连续连写："0102"->[1,2], "1011"->[10,11], "091011"->[9,10,11], "910"->[9,10], "123"->[1,2,3], "101112"->[10,11,12]
     */
    internal fun decodeDigitTokenToSections(tok: String, rowOrdinal: Int = -1, fullHeader: String = ""): List<Int> {
        if (tok.isEmpty()) return emptyList()
        if (tok == "12") {
            return when {
                rowOrdinal in 0..1 -> listOf(1, 2)
                rowOrdinal >= 4 -> listOf(12)
                fullHeader.contains("第12") || fullHeader.contains("晚") -> listOf(12)
                fullHeader.contains("上午") || fullHeader.contains("早") -> listOf(1, 2)
                // 无行号上下文时："12节" 默认按首行 "1、2节" 处理；纯数字 "12"（如 "11-12" 的右端点）按 12 处理
                fullHeader.contains("12节") && !fullHeader.contains("-") && !fullHeader.contains(",") -> listOf(1, 2)
                else -> listOf(12)
            }
        }
        if (tok.length == 1) {
            val v = tok.toIntOrNull() ?: return emptyList()
            return if (v in 1..9) listOf(v) else emptyList()
        }
        if (tok.length == 2) {
            val a = tok[0].digitToInt()
            val b = tok[1].digitToInt()
            if (a in 1..8 && b == a + 1) {
                return listOf(a, b)
            }
            val v = tok.toIntOrNull() ?: return emptyList()
            return if (v in 1..18) listOf(v) else emptyList()
        }
        // 偶数位补零紧凑格式："0102"、"0304"、"1011"、"050607"、"091011"、"101112"
        if (tok.length % 2 == 0) {
            val pairs = tok.chunked(2).mapNotNull { it.toIntOrNull() }
            if (pairs.size >= 2 && pairs.all { it in 1..18 } && pairs.zipWithNext().all { (x, y) -> y == x + 1 }) {
                return pairs
            }
        }
        // 非补零多位连续节次："123"、"234"、"345"、"567"、"678"、"789"、"910"、"8910"、"91011"
        for (start in 1..17) {
            for (end in (start + 1)..minOf(18, start + 5)) {
                if ((start..end).joinToString("") == tok) {
                    return (start..end).toList()
                }
            }
        }
        return emptyList()
    }

    private fun numbersRange(text: String, rowOrdinal: Int = -1): Pair<Int, Int>? {
        val nums = Regex("\\d+").findAll(text)
            .flatMap { m -> decodeDigitTokenToSections(m.value, rowOrdinal, text) }
            .filter { it in 1..18 }
            .toList()
        if (nums.isEmpty()) return null
        val min = nums.min()
        val max = nums.max()
        // 行头的节次跨度不应超过 6 节，否则很可能混入了无关数字
        return if (max - min <= 5) min to max else null
    }
}
