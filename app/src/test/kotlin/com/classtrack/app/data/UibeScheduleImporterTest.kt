package com.classtrack.app.data

import com.classtrack.app.data.model.DefaultPresets
import com.classtrack.app.data.model.WeekParity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 对外经济贸易大学（UIBE：bkjw.uibe.edu.cn / zhjw.uibe.edu.cn）多渠道导课专项回归测试
 */
class UibeScheduleImporterTest {

    private fun flexCell(day: Int, text: String) = TimetableCellParser.RawTimetableCell(
        kind = "cell",
        source = "flexgrid",
        day = day,
        text = text
    )

    @Test
    fun universityPresets_containsBothZhjwAndBkjwForUibe() {
        val uibePortals = DefaultPresets.universityPortals.filter {
            it.schoolName.contains("对外经济贸易大学")
        }
        assertTrue("UIBE 应同时预置 zhjw 与 bkjw 两个教务入口", uibePortals.size >= 2)
        assertTrue(uibePortals.any { it.url.contains("zhjw.uibe.edu.cn") })
        assertTrue(uibePortals.any { it.url.contains("bkjw.uibe.edu.cn") })
    }

    @Test
    fun parseSectionRange_handlesUibeDoubleUnitSectionFormat() {
        assertEquals(1 to 2, CourseImporter.parseSectionRange("4-16周李怀征 1节-2节 求103"))
        assertEquals(3 to 5, CourseImporter.parseSectionRange("4-16周叶文锐 3节-5节 宁726"))
        assertEquals(6 to 7, CourseImporter.parseSectionRange("4-12周,15-16周陈俊彰 6节-7节 求319"))
        assertEquals(8 to 9, CourseImporter.parseSectionRange("4-16周吴超 8节-9节 田径场"))
        assertEquals(10 to 12, CourseImporter.parseSectionRange("1-16周 张某 10节-12节 求101"))
    }

    @Test
    fun parseWeekInfo_ignoresClassNumberAndEnrollmentCountAndPreservesSurnameDan() {
        val seminarWeeks = CourseImporter.parseWeekInfo(
            "“名师启航”新生研讨课[ACA102] 75\n10-16周叶鹏 8节-9节 宁331\n选课人数：39"
        )
        assertEquals(10, seminarWeeks.startWeek)
        assertEquals(16, seminarWeeks.endWeek)
        assertEquals("", seminarWeeks.weeksSpec)

        val splitWeeks = CourseImporter.parseWeekInfo(
            "人工智能素养与实践[AIT105] 9\n4-12周,15-16周陈俊彰 6节-7节 求319\n选课人数：129"
        )
        assertEquals(4, splitWeeks.startWeek)
        assertEquals(16, splitWeeks.endWeek)
        assertEquals("4-12,15-16", splitWeeks.weeksSpec)

        val surnameDanWeeks = CourseImporter.parseWeekInfo("5-16周单萧语 6节-7节 求321")
        assertEquals(5, surnameDanWeeks.startWeek)
        assertEquals(16, surnameDanWeeks.endWeek)
        assertEquals(WeekParity.ALL, surnameDanWeeks.parity)
    }

    @Test
    fun uibe_bkjw_allSampleCards_extractAccurateRoomsTeachersSectionsAndWeeks() {
        val cells = listOf(
            // 周一 6-7节
            flexCell(1, "大学商务英语\n（一）[ENG653] 6\n4-16周时晓英\n6节-7节 求217\n选课人数：46"),
            // 周二 3-5节 + 8-9节（含折行）
            flexCell(2, "数学分析\n（一）[STA102] 1\n4-16周叶文锐\n3节-5节 宁726\n选课人数：150"),
            flexCell(2, "“名师启航”新生研讨课[ACA102] 75\n10-16周叶鹏 8\n节-9节 宁331\n选课人数：39"),
            // 周三 1-2节 + 3-4节 + 6-7节（一卡双教师不同周次）
            flexCell(3, "思想道德与法治[ETH105] 19\n4-16周李怀征 1节-2节 求103\n选课人数：99"),
            flexCell(3, "中国文学与文化[GED111] 13\n4-16周黄海飞 3节-4节 博305\n选课人数：60"),
            flexCell(
                3,
                "心理健康与自我成长[ETH106] 25\n5-16周单萧语 6节-7节 求321\n4周刘菁 6节-7节 求321\n选课人数：92"
            ),
            // 周四 1-2节
            flexCell(4, "形势与政策[HUM999] 104\n13-16周于靖靖 1节-2节 求305\n选课人数：100"),
            // 周五 1-2节 + 3-5节 + 6-7节（断点周） + 8-9节（田径场）
            flexCell(5, "大学商务英语\n（一）[ENG653] 6\n4-16周时晓英 1节-2节 求507\n选课人数：46"),
            flexCell(5, "高等代数\n（一）[STA107] 1\n4-16周张宇卓 3节-5节 求323\n选课人数：153"),
            flexCell(
                5,
                "人工智能素养与实践[AIT105] 9\n4-12周,15-16\n周陈俊彰 6节-7节 求319\n选课人数：129"
            ),
            flexCell(5, "体育基础[PED001] 52\n4-16周吴超 8节-9节 田径场\n选课人数：36")
        )

        val courses = TimetableCellParser.parseCells(cells)
        // 共 11 张卡片，其中周三心理健康拆成 2 条不同教师/周次排课，合计 12 条课轨
        assertEquals(
            "实际解析: ${courses.joinToString { "${it.dayOfWeek}:${it.name}(${it.teacher}/${it.room}/${it.startSection}-${it.endSection})" }}",
            12,
            courses.size
        )
        // 所有课程均必须精准解析出真实教室，绝不可出现 待定教室
        assertTrue(courses.none { it.room == "待定教室" })

        val ideology = courses.first { it.name == "思想道德与法治" }
        assertEquals(3, ideology.dayOfWeek)
        assertEquals(1, ideology.startSection)
        assertEquals(2, ideology.endSection)
        assertEquals("求103", ideology.room)
        assertEquals("李怀征", ideology.teacher)
        assertEquals(4, ideology.startWeek)
        assertEquals(16, ideology.endWeek)

        val mathAnalysis = courses.first { it.name == "数学分析(一)" }
        assertEquals(2, mathAnalysis.dayOfWeek)
        assertEquals(3, mathAnalysis.startSection)
        assertEquals(5, mathAnalysis.endSection)
        assertEquals("宁726", mathAnalysis.room)
        assertEquals("叶文锐", mathAnalysis.teacher)

        val seminar = courses.first { it.name.contains("名师启航") }
        assertEquals(2, seminar.dayOfWeek)
        assertEquals(8, seminar.startSection)
        assertEquals(9, seminar.endSection)
        assertEquals("宁331", seminar.room)
        assertEquals("叶鹏", seminar.teacher)
        assertEquals(10, seminar.startWeek)
        assertEquals(16, seminar.endWeek)
        assertEquals("", seminar.weeksSpec)

        val psychList = courses.filter { it.name == "心理健康与自我成长" }
        assertEquals(2, psychList.size)
        val psychDan = psychList.first { it.teacher == "单萧语" }
        assertEquals("求321", psychDan.room)
        assertEquals(6, psychDan.startSection)
        assertEquals(7, psychDan.endSection)
        assertEquals(5, psychDan.startWeek)
        assertEquals(16, psychDan.endWeek)
        assertEquals(WeekParity.ALL, psychDan.weekParity)

        val psychLiu = psychList.first { it.teacher == "刘菁" }
        assertEquals("求321", psychLiu.room)
        assertEquals(4, psychLiu.startWeek)
        assertEquals(4, psychLiu.endWeek)

        val aiCourse = courses.first { it.name == "人工智能素养与实践" }
        assertEquals(5, aiCourse.dayOfWeek)
        assertEquals(6, aiCourse.startSection)
        assertEquals(7, aiCourse.endSection)
        assertEquals("求319", aiCourse.room)
        assertEquals("陈俊彰", aiCourse.teacher)
        assertEquals("4-12,15-16", aiCourse.weeksSpec)

        val peCourse = courses.first { it.name == "体育基础" }
        assertEquals(5, peCourse.dayOfWeek)
        assertEquals(8, peCourse.startSection)
        assertEquals(9, peCourse.endSection)
        assertEquals("田径场", peCourse.room)
        assertEquals("吴超", peCourse.teacher)
    }

    @Test
    fun parseAuto_copiedUibeTextAndAiJson_extractsRoomsAndStripsCourseCodes() {
        val copiedUibeText = """
            星期三
            思想道德与法治[ETH105] 19
            4-16周李怀征 1节-2节 求103
            选课人数：99
            心理健康与自我成长[ETH106] 25
            5-16周单萧语 6节-7节 求321
            4周刘菁 6节-7节 求321
            选课人数：92
            星期五
            人工智能素养与实践[AIT105] 9
            4-12周,15-16周陈俊彰 6节-7节 求319
            选课人数：129
            体育基础[PED001] 52
            4-16周吴超 8节-9节 田径场
            选课人数：36
        """.trimIndent()

        val textResult = CourseImporter.parseAuto(copiedUibeText)
        assertTrue(textResult.success)
        assertEquals(5, textResult.courses.size)
        assertTrue(textResult.courses.none { it.room == "待定教室" })

        val wedIdeology = textResult.courses.firstOrNull { it.name == "思想道德与法治" }
        assertNotNull(wedIdeology)
        assertEquals(3, wedIdeology!!.dayOfWeek)
        assertEquals("求103", wedIdeology.room)
        assertEquals("李怀征", wedIdeology.teacher)
        assertEquals(1, wedIdeology.startSection)
        assertEquals(2, wedIdeology.endSection)

        val friAi = textResult.courses.first { it.name == "人工智能素养与实践" }
        assertEquals(5, friAi.dayOfWeek)
        assertEquals("求319", friAi.room)
        assertEquals("4-12,15-16", friAi.weeksSpec)

        // 同时验证 AI 结构化输出带课程代码后缀与非标准 weeksSpec 的二次清洗
        val aiJson = """
            [
              {
                "name": "数学分析（一）[STA102] 1",
                "room": "宁726",
                "teacher": "叶文锐",
                "dayOfWeek": 2,
                "startSection": 3,
                "endSection": 5,
                "startWeek": 4,
                "endWeek": 16,
                "weeksSpec": "4-12周,15-16周",
                "parity": "ALL"
              }
            ]
        """.trimIndent()
        val aiResult = CourseImporter.parseAuto(aiJson)
        assertTrue(aiResult.success)
        val parsed = aiResult.courses.single()
        assertEquals("数学分析(一)", parsed.name)
        assertEquals("宁726", parsed.room)
        assertEquals("4-12,15-16", parsed.weeksSpec)
    }
}
