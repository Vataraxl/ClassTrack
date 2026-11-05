package com.classtrack.app.ui.main

import com.classtrack.app.data.CourseImporter
import com.classtrack.app.data.model.ArtistTheme
import com.classtrack.app.data.model.CourseItem
import com.classtrack.app.data.model.DefaultPresets
import com.classtrack.app.data.model.ScheduleBackup
import com.classtrack.app.data.model.WeekParity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class MainScreenViewModelTest {

    @Test
    fun courseItem_weekParity_matchesCorrectly() {
        val oddCourse = CourseItem(
            name = "操作系统实验",
            room = "405",
            dayOfWeek = 3,
            startSection = 1,
            endSection = 2,
            startWeek = 1,
            endWeek = 15,
            weekParity = WeekParity.ODD
        )
        assertTrue(oddCourse.isActiveInWeek(1))
        assertFalse(oddCourse.isActiveInWeek(2))
        assertTrue(oddCourse.isActiveInWeek(9))
        assertFalse(oddCourse.isActiveInWeek(16))
    }

    @Test
    fun courseImporter_parsesCsvAndBackupJson() {
        val csvInput = """
            课程名称,星期,开始节次,结束节次,教室,教师,开始周,结束周
            数据结构,1,1,2,A302,林教授,1,16
            日本音乐赏析,5,6,7,B204,夏目,1,16
        """.trimIndent()

        val csvResult = CourseImporter.parseAuto(csvInput)
        assertTrue(csvResult.success)
        assertEquals(2, csvResult.courses.size)
        assertEquals("数据结构", csvResult.courses[0].name)

        val dummyTestCourses = listOf(
            CourseItem(id = 1L, name = "计算机网络", room = "301", teacher = "张老师", dayOfWeek = 1, startSection = 1, endSection = 2, startWeek = 1, endWeek = 16),
            CourseItem(id = 2L, name = "操作系统", room = "402", teacher = "李老师", dayOfWeek = 2, startSection = 3, endSection = 4, startWeek = 1, endWeek = 16)
        )
        val backup = ScheduleBackup(
            scheduleName = "测试课表",
            currentWeek = 5,
            totalWeeks = 16,
            artistTheme = ArtistTheme.YORUSHIKA,
            courses = dummyTestCourses
        )
        val jsonStr = CourseImporter.exportToJson(backup)
        val jsonResult = CourseImporter.parseAuto(jsonStr)
        assertTrue(jsonResult.success)
        assertEquals(dummyTestCourses.size, jsonResult.courses.size)
        assertEquals(ArtistTheme.YORUSHIKA, jsonResult.backupMetadata?.artistTheme)

        // 验证主题 JSON 解析（含 DeepSeek 返回的 ```json 代码块包裹）
        val aiWrappedThemeJson = """
            ```json
            {
              "themeName": "结束乐队 · 星光",
              "artistName": "結束バンド",
              "subtitle": "孤独摇滚四原色",
              "isDark": true,
              "backgroundHex": "#12131A",
              "surfaceHex": "#1C1E28",
              "primaryHex": "#FF4D8D"
            }
            ```
        """.trimIndent()
        val parsedTheme = CourseImporter.parseThemeJson(aiWrappedThemeJson)
        assertTrue(parsedTheme.isSuccess)
        assertEquals("结束乐队 · 星光", parsedTheme.getOrNull()?.themeName)
        assertEquals(6, parsedTheme.getOrNull()?.trackBackgroundHexList?.size)
        assertEquals(6, parsedTheme.getOrNull()?.trackAccentHexList?.size)
    }

    @Test
    fun universityPresets_containsOnlySingleBjfuNewjwxt() {
        val bjfuList = DefaultPresets.universityPortals.filter { it.schoolName == "北京林业大学" }
        assertEquals(1, bjfuList.size)
        assertEquals("http://newjwxt.bjfu.edu.cn", bjfuList.first().url)
    }

    @Test
    fun emptyThemeTemplate_andDetailedPrompt_containAllRequiredFields() {
        val emptyJson = CourseImporter.buildEmptyThemeTemplateJson()
        assertTrue(emptyJson.contains("\"themeName\": \"\""))
        assertTrue(emptyJson.contains("\"trackBackgroundHexList\""))
        assertTrue(emptyJson.contains("\"customLyrics\""))

        val prompt = CourseImporter.buildDeepSeekThemePrompt("孤独摇滚结束乐队")
        assertTrue(prompt.contains("孤独摇滚结束乐队"))
        assertTrue(prompt.contains("【字段规范】"))
        assertTrue(prompt.contains("trackBackgroundHexList"))
    }

    @Test
    fun parseGenericUniversityApiJson_parsesQiangZhiAndZhengFangJsonCorrectly() {
        // 1. 模拟强智系统/通用教务 API 返回的 JSON (含 kcmc, jxcdmc, teName, dayOfWeek, startUnit 等)
        val qiangZhiApiJson = """
            {
              "code": 200,
              "data": [
                {
                  "kcmc": "微积分(经管类)",
                  "jxcdmc": "博学楼402",
                  "teName": "王教授",
                  "dayOfWeek": 2,
                  "startUnit": 3,
                  "endUnit": 4,
                  "weeks": "1-16周"
                },
                {
                  "kcmc": "国际贸易实务",
                  "jxcdmc": "诚信楼101",
                  "teName": "李老师",
                  "dayOfWeek": 4,
                  "startUnit": 5,
                  "endUnit": 6,
                  "weeks": "1-16周(双)"
                }
              ]
            }
        """.trimIndent()

        val parsedQz = CourseImporter.parseGenericUniversityApiJson(qiangZhiApiJson)
        assertEquals(2, parsedQz.size)
        assertEquals("微积分(经管类)", parsedQz[0].name)
        assertEquals("博学楼402", parsedQz[0].room)
        assertEquals(2, parsedQz[0].dayOfWeek)
        assertEquals(3, parsedQz[0].startSection)
        assertEquals(4, parsedQz[0].endSection)
        assertEquals(1, parsedQz[0].startWeek)
        assertEquals(16, parsedQz[0].endWeek)
        assertEquals(WeekParity.ALL, parsedQz[0].weekParity)

        assertEquals("国际贸易实务", parsedQz[1].name)
        assertEquals(WeekParity.EVEN, parsedQz[1].weekParity)

        // 2. 模拟新正方教务系统 kbList 数组格式
        val zfApiJson = """
            {
              "kbList": [
                {
                  "kcmc": "宏观经济学",
                  "cdmc": "宁远楼328",
                  "xm": "赵教授",
                  "xqj": 1,
                  "jcs": "1-2",
                  "zcd": "1-16周"
                }
              ]
            }
        """.trimIndent()
        val parsedZf = CourseImporter.parseGenericUniversityApiJson(zfApiJson)
        assertEquals(1, parsedZf.size)
        assertEquals("宏观经济学", parsedZf[0].name)
        assertEquals("宁远楼328", parsedZf[0].room)
        assertEquals(1, parsedZf[0].dayOfWeek)
        assertEquals(1, parsedZf[0].startSection)
        assertEquals(2, parsedZf[0].endSection)

        // 3. 测试通过 CourseImporter.parseAuto 自动分流解析 API JSON
        val autoResult = CourseImporter.parseAuto(qiangZhiApiJson)
        assertTrue(autoResult.success)
        assertEquals("教务系统原生数据接口", autoResult.sourceType)
        assertEquals(2, autoResult.courses.size)
    }

    @Test
    fun universityPresets_containsUibeAndMajorUniversities() {
        val uibe = DefaultPresets.universityPortals.firstOrNull { it.schoolName.contains("对外经济贸易大学") }
        assertTrue("预置中必须包含对外经济贸易大学 (UIBE)", uibe != null)
        assertEquals("http://zhjw.uibe.edu.cn", uibe?.url)
        assertEquals("强智教务", uibe?.systemType)
    }

    @Test
    fun fluidCloud_semanticFrameCarouselSteps_verifySequence() {
        val steps = (0..5).map { step ->
            when (step % 3) {
                0 -> "课程"
                1 -> "地点"
                else -> "时间"
            }
        }
        assertEquals(listOf("课程", "地点", "时间", "课程", "地点", "时间"), steps)
    }

    @Test
    fun watchBridge_channelAndActionConstants_areValid() {
        assertEquals("classtrack_watch_bridge_v1", com.classtrack.app.system.ColorOsFluidCloudHelper.CHANNEL_ID_WATCH_BRIDGE)
        assertEquals(202603, com.classtrack.app.system.ColorOsFluidCloudHelper.NOTIFICATION_ID_WATCH_BRIDGE)
    }

    @Test
    fun uiStyle_enumProperties_andTransitionEvent_areValid() {
        val classic = com.classtrack.app.data.model.UiStyle.CLASSIC
        val glass = com.classtrack.app.data.model.UiStyle.GLASS

        assertEquals("classic", classic.id)
        assertEquals("经典质感", classic.displayName)
        assertEquals("glass", glass.id)
        assertEquals("液态玻璃", glass.displayName)

        val event = UiStyleTransitionEvent(
            fromStyle = classic,
            toStyle = glass,
            timestamp = 1000L
        )
        assertEquals(classic, event.fromStyle)
        assertEquals(glass, event.toStyle)
        assertEquals(1000L, event.timestamp)
    }

    @Test
    fun mergeAndSanitizeCourses_merges3SectionConsecutiveCoursesCorrectly() {
        // 场景 1：教务系统将一门 3 节的大课拆分成 [1-2节] 和 [3-3节]，必须无缝合并为 1-3 节！
        val split3SectionCourses = listOf(
            CourseItem(
                name = "大学物理实验(甲)[01]",
                room = "物电楼204",
                teacher = "张老师",
                dayOfWeek = 1,
                startSection = 1,
                endSection = 2,
                startWeek = 1,
                endWeek = 16
            ),
            CourseItem(
                name = "大学物理实验(甲)",
                room = "物电楼204",
                teacher = "张老师",
                dayOfWeek = 1,
                startSection = 3,
                endSection = 3,
                startWeek = 1,
                endWeek = 16
            )
        )
        val merged3 = CourseImporter.mergeAndSanitizeCourses(split3SectionCourses)
        assertEquals(1, merged3.size)
        assertEquals(1, merged3[0].startSection)
        assertEquals(3, merged3[0].endSection)
        assertEquals("大学物理实验(甲)[01]", merged3[0].name)
        assertEquals("物电楼204", merged3[0].room)

        // 场景 2：连续排课的 4 节连堂实验课 [1-2节] + [3-4节]，必须合并为 1-4 节！
        val split4SectionCourses = listOf(
            CourseItem(
                name = "电路与模拟电子技术",
                room = "电子楼302",
                teacher = "李教授",
                dayOfWeek = 3,
                startSection = 1,
                endSection = 2,
                startWeek = 1,
                endWeek = 16
            ),
            CourseItem(
                name = "电路与模拟电子技术",
                room = "电子楼302",
                teacher = "李教授",
                dayOfWeek = 3,
                startSection = 3,
                endSection = 4,
                startWeek = 1,
                endWeek = 16
            )
        )
        val merged4 = CourseImporter.mergeAndSanitizeCourses(split4SectionCourses)
        assertEquals(1, merged4.size)
        assertEquals(1, merged4[0].startSection)
        assertEquals(4, merged4[0].endSection)

        // 场景 3：重叠重复抓取课块 [1-3节] 与 [2-3节]，必须合并为完整的 [1-3节]！
        val overlapCourses = listOf(
            CourseItem(
                name = "软件工程导论",
                room = "主楼101",
                dayOfWeek = 4,
                startSection = 1,
                endSection = 3,
                startWeek = 1,
                endWeek = 16
            ),
            CourseItem(
                name = "软件工程导论",
                room = "主楼101",
                dayOfWeek = 4,
                startSection = 2,
                endSection = 3,
                startWeek = 1,
                endWeek = 16
            )
        )
        val mergedOverlap = CourseImporter.mergeAndSanitizeCourses(overlapCourses)
        assertEquals(1, mergedOverlap.size)
        assertEquals(1, mergedOverlap[0].startSection)
        assertEquals(3, mergedOverlap[0].endSection)

        // 场景 4：不同周次/不同星期的课独立保留
        val differentDayCourses = listOf(
            CourseItem(name = "高数", room = "101", dayOfWeek = 1, startSection = 1, endSection = 2, startWeek = 1, endWeek = 16),
            CourseItem(name = "高数", room = "101", dayOfWeek = 3, startSection = 1, endSection = 2, startWeek = 1, endWeek = 16)
        )
        val mergedDiff = CourseImporter.mergeAndSanitizeCourses(differentDayCourses)
        assertEquals(2, mergedDiff.size)
    }

    @Test
    fun parseSectionRange_andWeekInfo_handlesVariousCampusFormats() {
        // 验证各种教务节次格式
        val sec1to3 = CourseImporter.parseSectionRange("[01-03节]")
        assertEquals(Pair(1, 3), sec1to3)

        val sec1to3Tilde = CourseImporter.parseSectionRange("1~3节")
        assertEquals(Pair(1, 3), sec1to3Tilde)

        val sec123 = CourseImporter.parseSectionRange("第1,2,3节")
        assertEquals(Pair(1, 3), sec123)

        val singleSec3 = CourseImporter.parseSectionRange("[03节]")
        assertEquals(Pair(3, 3), singleSec3)

        // 验证周次格式
        val weekInfo = CourseImporter.parseWeekInfo("1-16周(单)")
        assertEquals(1, weekInfo.startWeek)
        assertEquals(16, weekInfo.endWeek)
        assertEquals(WeekParity.ODD, weekInfo.parity)

        val discreteWeek = CourseImporter.parseWeekInfo("2,6,8-9,12-18(周)")
        assertEquals(2, discreteWeek.startWeek)
        assertEquals(18, discreteWeek.endWeek)
        assertEquals("2,6,8-9,12-18", discreteWeek.weeksSpec)
    }

    @Test
    fun mergeAndSanitizeCourses_threeSectionHint_mergesTo1to3Not4() {
        val split3SectionHintCourses = listOf(
            CourseItem(
                name = "大学物理(3节)",
                room = "理学楼102",
                teacher = "韩老师",
                dayOfWeek = 2,
                startSection = 1,
                endSection = 2,
                startWeek = 1,
                endWeek = 16
            ),
            CourseItem(
                name = "大学物理",
                room = "理学楼102",
                teacher = "韩老师",
                dayOfWeek = 2,
                startSection = 3,
                endSection = 4,
                startWeek = 1,
                endWeek = 16
            )
        )
        val merged = CourseImporter.mergeAndSanitizeCourses(split3SectionHintCourses)
        assertEquals(1, merged.size)
        assertEquals(1, merged[0].startSection)
        assertEquals(3, merged[0].endSection)
    }

    @Test
    fun mergeAndSanitizeCourses_threeIndividualOneSectionCourses_mergesTo1to3() {
        val threePieceCourses = listOf(
            CourseItem(
                name = "数据结构实验",
                room = "机房301",
                dayOfWeek = 4,
                startSection = 1,
                endSection = 1,
                startWeek = 1,
                endWeek = 16
            ),
            CourseItem(
                name = "数据结构实验",
                room = "机房301",
                dayOfWeek = 4,
                startSection = 2,
                endSection = 2,
                startWeek = 1,
                endWeek = 16
            ),
            CourseItem(
                name = "数据结构实验",
                room = "机房301",
                dayOfWeek = 4,
                startSection = 3,
                endSection = 3,
                startWeek = 1,
                endWeek = 16
            )
        )
        val merged = CourseImporter.mergeAndSanitizeCourses(threePieceCourses)
        assertEquals(1, merged.size)
        assertEquals(1, merged[0].startSection)
        assertEquals(3, merged[0].endSection)
    }

    @Test
    fun mergeAndSanitizeCourses_disjointHalfSemesterCourses_areNotMerged() {
        // 前半学期 1-8 周与后半学期 9-16 周的错时课程，虽然节次相邻，但活跃周完全不重合，绝不能合并为 1-4 节！
        val splitTermCourses = listOf(
            CourseItem(
                name = "计算机网络",
                room = "网络楼101",
                dayOfWeek = 1,
                startSection = 1,
                endSection = 2,
                startWeek = 1,
                endWeek = 8
            ),
            CourseItem(
                name = "计算机网络",
                room = "网络楼101",
                dayOfWeek = 1,
                startSection = 3,
                endSection = 4,
                startWeek = 9,
                endWeek = 16
            )
        )
        val result = CourseImporter.mergeAndSanitizeCourses(splitTermCourses)
        assertEquals(2, result.size)
    }

    @Test
    fun parseSectionRange_strictlyRejectsWeekStrings() {
        assertNull(CourseImporter.parseSectionRange("1-16周"))
        assertNull(CourseImporter.parseSectionRange("第1-16周"))
        assertNull(CourseImporter.parseSectionRange("1-18周(单)"))
        assertEquals(Pair(1, 2), CourseImporter.parseSectionRange("第1大节"))
        assertEquals(Pair(3, 4), CourseImporter.parseSectionRange("第2大节"))
        assertEquals(Pair(1, 2), CourseImporter.parseSectionRange("1-2节"))
    }

    @Test
    fun parseWeekInfo_multiIntervalsAndParity() {
        val multiInterval = CourseImporter.parseWeekInfo("1-9周, 11-17周")
        assertEquals(1, multiInterval.startWeek)
        assertEquals(17, multiInterval.endWeek)
        assertEquals("1-9,11-17", multiInterval.weeksSpec)
        assertEquals(WeekParity.ALL, multiInterval.parity)

        val oddInterval = CourseImporter.parseWeekInfo("1-8周，10-18周(单)")
        assertEquals(1, oddInterval.startWeek)
        assertEquals(18, oddInterval.endWeek)
        assertEquals("1-8,10-18", oddInterval.weeksSpec)
        assertEquals(WeekParity.ODD, oddInterval.parity)
    }

    @Test
    fun extractDayOfWeek_handlesVariousFormats() {
        val obj1 = buildJsonObject { put("xqj", "周三") }
        assertEquals(3, CourseImporter.extractDayOfWeek(obj1, "xqj"))

        val obj2 = buildJsonObject { put("weekday", "星期五") }
        assertEquals(5, CourseImporter.extractDayOfWeek(obj2, "weekday"))

        val obj3 = buildJsonObject { put("day", "3") }
        assertEquals(3, CourseImporter.extractDayOfWeek(obj3, "day"))

        val obj4 = buildJsonObject { put("xq", 7) }
        assertEquals(7, CourseImporter.extractDayOfWeek(obj4, "xq"))

        val obj5 = buildJsonObject { put("skxq", "礼拜天") }
        assertEquals(7, CourseImporter.extractDayOfWeek(obj5, "skxq"))

        val obj6 = buildJsonObject { put("day", "周1") }
        assertEquals(1, CourseImporter.extractDayOfWeek(obj6, "day"))

        val obj7 = buildJsonObject { put("day", "星期七") }
        assertEquals(7, CourseImporter.extractDayOfWeek(obj7, "day"))
    }

    @Test
    fun parseSectionRange_andWeekInfo_strictlySeparated() {
        // [1-16周] 不得被误识别为 1-16 节
        assertNull(CourseImporter.parseSectionRange("[1-16周]"))
        assertNull(CourseImporter.parseSectionRange("[01-16周]"))

        // 周次与节次共存文本提取
        assertEquals(Pair(1, 2), CourseImporter.parseSectionRange("1-16周 1-2节"))
        assertEquals(Pair(3, 4), CourseImporter.parseSectionRange("1-16周 第3-4节"))

        // 周次提取时节次数字 9-10 不得混入周次集合
        val weekInfo = CourseImporter.parseWeekInfo("9-10节 1-8周")
        assertEquals(1, weekInfo.startWeek)
        assertEquals(8, weekInfo.endWeek)
    }

    @Test
    fun mergeAndSanitizeCourses_robustnessEdgeCases() {
        // 场景 A: 4 节连堂实验课，备注有 "第3节去机房"，绝不能被误截断为 1-3 节！
        val real4Section = listOf(
            CourseItem(name = "电子线路设计", room = "实验楼101", dayOfWeek = 2, startSection = 1, endSection = 2, startWeek = 1, endWeek = 16, note = "第3节随堂测验"),
            CourseItem(name = "电子线路设计", room = "实验楼101", dayOfWeek = 2, startSection = 3, endSection = 4, startWeek = 1, endWeek = 16)
        )
        val merged4 = CourseImporter.mergeAndSanitizeCourses(real4Section)
        assertEquals(1, merged4.size)
        assertEquals(1, merged4[0].startSection)
        assertEquals(4, merged4[0].endSection)

        // 场景 B: 下午 3 节长课 (5-7节)，排成 5-6 与 7-8，课名带 (3节)
        val afternoon3 = listOf(
            CourseItem(name = "物理化学实验(3节)", room = "化院102", dayOfWeek = 3, startSection = 5, endSection = 6, startWeek = 1, endWeek = 16),
            CourseItem(name = "物理化学实验", room = "化院102", dayOfWeek = 3, startSection = 7, endSection = 8, startWeek = 1, endWeek = 16)
        )
        val mergedAfternoon3 = CourseImporter.mergeAndSanitizeCourses(afternoon3)
        assertEquals(1, mergedAfternoon3.size)
        assertEquals(5, mergedAfternoon3[0].startSection)
        assertEquals(7, mergedAfternoon3[0].endSection)

        // 场景 C: 1-16 周 1-2 节与 1-8 周 3-4 节，周次不完全相同，绝不能合并为全学期 1-4 节长课！
        val unequalWeeks = listOf(
            CourseItem(name = "大学英语", room = "一教101", dayOfWeek = 1, startSection = 1, endSection = 2, startWeek = 1, endWeek = 16),
            CourseItem(name = "大学英语", room = "一教101", dayOfWeek = 1, startSection = 3, endSection = 4, startWeek = 1, endWeek = 8)
        )
        val mergedUnequal = CourseImporter.mergeAndSanitizeCourses(unequalWeeks)
        assertEquals(2, mergedUnequal.size)

        // 场景 D: 同门课同节次因考试周跳周 (1-8周 + 10-16周)，应合并为一个离散周次条目 (1-8,10-16)
        val skipWeekCourses = listOf(
            CourseItem(name = "线性代数", room = "主楼201", dayOfWeek = 4, startSection = 1, endSection = 2, startWeek = 1, endWeek = 8),
            CourseItem(name = "线性代数", room = "主楼201", dayOfWeek = 4, startSection = 1, endSection = 2, startWeek = 10, endWeek = 16)
        )
        val mergedSkip = CourseImporter.mergeAndSanitizeCourses(skipWeekCourses)
        assertEquals(1, mergedSkip.size)
        assertEquals(1, mergedSkip[0].startWeek)
        assertEquals(16, mergedSkip[0].endWeek)
        assertEquals("1-8,10-16", mergedSkip[0].weeksSpec)
    }

    @Test
    fun courseImporter_handlesBomAndAdvancedNormalization() {
        // UTF-8 BOM 前缀识别
        val jsonWithBom = "\uFEFF" + """
            {
              "scheduleName": "BOM测试课表",
              "currentWeek": 1,
              "totalWeeks": 16,
              "artistTheme": "ZTMY",
              "courses": [
                {"name": "编译原理", "room": "A101", "dayOfWeek": 1, "startSection": 1, "endSection": 2, "startWeek": 1, "endWeek": 16}
              ]
            }
        """.trimIndent()
        val result = CourseImporter.parseAuto(jsonWithBom)
        assertTrue(result.success)
        assertEquals(1, result.courses.size)
        assertEquals("编译原理", result.courses[0].name)

        // 课程名去标签净化
        assertEquals("操作系统", CourseImporter.normalizeCourseName("【必修】操作系统[01]"))
        assertEquals("软件测试", CourseImporter.normalizeCourseName("软件测试(3.0学时)"))
        assertEquals("人工智能导论", CourseImporter.normalizeCourseName("人工智能导论[3学分]"))
        assertEquals("计算机图形学", CourseImporter.normalizeCourseName("计算机图形学(双语)"))
        assertEquals("中西文化比较", CourseImporter.normalizeCourseName("[公选]中西文化比较"))
        assertEquals("摄影基础", CourseImporter.normalizeCourseName("【校选】摄影基础"))
        assertEquals("心理学", CourseImporter.normalizeCourseName("[通选]心理学"))
        assertEquals("数据挖掘", CourseImporter.normalizeCourseName("[专选]数据挖掘"))
        assertEquals("操作系统", CourseImporter.normalizeCourseName("[核心]操作系统"))
        assertEquals("批判性思维", CourseImporter.normalizeCourseName("[网课]批判性思维"))
        assertEquals("大学物理", CourseImporter.normalizeCourseName("大学物理(3节连上)"))
        assertEquals("高等数学A(1)", CourseImporter.normalizeCourseName("高等数学A(1)"))
    }

    @Test
    fun courseItem_isActiveInWeek_respectsParityEvenWhenWeeksSpecIsPresent() {
        val oddDiscreteCourse = CourseItem(
            name = "离散单周实验",
            room = "302",
            dayOfWeek = 1,
            startSection = 1,
            endSection = 2,
            startWeek = 1,
            endWeek = 16,
            weeksSpec = "1-8,10-16",
            weekParity = WeekParity.ODD
        )
        assertTrue(oddDiscreteCourse.isActiveInWeek(1))
        assertFalse("单周课程在第2周(双周)严禁处于活跃状态！", oddDiscreteCourse.isActiveInWeek(2))
        assertTrue(oddDiscreteCourse.isActiveInWeek(3))
        assertFalse(oddDiscreteCourse.isActiveInWeek(4))
        assertFalse("第9周不在 weeksSpec 中，严禁处于活跃状态！", oddDiscreteCourse.isActiveInWeek(9))
        assertTrue(oddDiscreteCourse.isActiveInWeek(11))
        assertFalse(oddDiscreteCourse.isActiveInWeek(12))

        val evenDiscreteCourse = CourseItem(
            name = "离散双周实验",
            room = "302",
            dayOfWeek = 1,
            startSection = 1,
            endSection = 2,
            startWeek = 1,
            endWeek = 16,
            weeksSpec = "1-8,10-16",
            weekParity = WeekParity.EVEN
        )
        assertFalse(evenDiscreteCourse.isActiveInWeek(1))
        assertTrue(evenDiscreteCourse.isActiveInWeek(2))
        assertFalse(evenDiscreteCourse.isActiveInWeek(3))
        assertTrue(evenDiscreteCourse.isActiveInWeek(4))
    }

    @Test
    fun threeSectionHint_doesNotMisidentifyWeekRangesAsThreeSectionCourses() {
        // "7-9周"、"1-3周" 为纯周次信息，严禁误触发 3 节长课判定！
        assertFalse("7-9周在科技楼 不能触发 3 节长课提示！", CourseImporter.hasThreeSectionHint("7-9周在科技楼"))
        assertFalse("1-3周 不能触发 3 节长课提示！", CourseImporter.hasThreeSectionHint("1-3周"))
        assertFalse("5-7周 不能触发 3 节长课提示！", CourseImporter.hasThreeSectionHint("5-7周"))

        // 真实 3 节课标识校验
        assertTrue(CourseImporter.hasThreeSectionHint("大学物理(3.0学时)"))
        assertTrue(CourseImporter.hasThreeSectionHint("物理实验 3学时"))
        assertTrue(CourseImporter.hasThreeSectionHint("(3节连上)"))
        assertTrue(CourseImporter.hasThreeSectionHint("连上3节"))
        assertTrue(CourseImporter.hasThreeSectionHint("1-3节"))
        assertTrue(CourseImporter.hasThreeSectionHint("(1-3)"))

        // 验证排在 7-9 周的真实 4 节连堂实验课，绝不能因包含 7-9 周而被误截断为 1-3 节！
        val real4SectionInWeeks7to9 = listOf(
            CourseItem(name = "电子实验", room = "科技楼101", dayOfWeek = 2, startSection = 1, endSection = 2, startWeek = 7, endWeek = 9, note = "7-9周在科技楼"),
            CourseItem(name = "电子实验", room = "科技楼101", dayOfWeek = 2, startSection = 3, endSection = 4, startWeek = 7, endWeek = 9, note = "7-9周在科技楼")
        )
        val merged4 = CourseImporter.mergeAndSanitizeCourses(real4SectionInWeeks7to9)
        assertEquals(1, merged4.size)
        assertEquals(1, merged4[0].startSection)
        assertEquals("4 节课严禁被误截断为 3 节！", 4, merged4[0].endSection)
    }

    @Test
    fun parseSectionRange_handlesParenthesesAndWeeksCoexistingWithoutSectionKeyword() {
        // 支持无 "节" 关键字的周节混排
        assertEquals(Pair(3, 4), CourseImporter.parseSectionRange("1-16周 3-4"))
        assertEquals(Pair(1, 2), CourseImporter.parseSectionRange("[1-16周] 1-2"))

        // 支持括号包裹的节次区间
        assertEquals(Pair(1, 2), CourseImporter.parseSectionRange("(1-2)"))
        assertEquals(Pair(3, 4), CourseImporter.parseSectionRange("(03-04)"))

        // 纯周次仍然严格返回 null
        assertNull(CourseImporter.parseSectionRange("1-16周"))
        assertNull(CourseImporter.parseSectionRange("[1-16周]"))
        assertNull(CourseImporter.parseSectionRange("[01-16周]"))
    }

    @Test
    fun parseGenericUniversityApiJson_handlesStepAndNotes() {
        val jsonWithStepAndNote = """
            {
              "data": [
                {
                  "kcmc": "机械设计基础",
                  "cdmc": "工科楼201",
                  "teName": "陈老师",
                  "day_of_week": 3,
                  "startUnit": 5,
                  "step": 3,
                  "bz": "3学时连上",
                  "zcd": "1-16周"
                }
              ]
            }
        """.trimIndent()

        val parsed = CourseImporter.parseGenericUniversityApiJson(jsonWithStepAndNote)
        assertEquals(1, parsed.size)
        assertEquals("机械设计基础", parsed[0].name)
        assertEquals(3, parsed[0].dayOfWeek)
        assertEquals(5, parsed[0].startSection)
        assertEquals(7, parsed[0].endSection) // 5 + 3 - 1 = 7 (3 sections)
        assertEquals("3学时连上", parsed[0].note)
    }

    @Test
    fun parseAuto_handlesCopiedScheduleTextWithCompositeLine() {
        val compositeCopiedText = """
            周一 1-2节 高等数学
            理学楼101
            张教授
            1-16周
        """.trimIndent()

        val result = CourseImporter.parseAuto(compositeCopiedText)
        assertTrue(result.success)
        assertEquals(1, result.courses.size)
        assertEquals("高等数学", result.courses[0].name)
        assertEquals(1, result.courses[0].dayOfWeek)
        assertEquals(1, result.courses[0].startSection)
        assertEquals(2, result.courses[0].endSection)
        assertEquals(1, result.courses[0].startWeek)
        assertEquals(16, result.courses[0].endWeek)
    }
}

