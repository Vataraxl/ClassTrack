package com.classtrack.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 教务网页原始格子解析回归测试（以北京林业大学强智教务为样本）
 */
class TimetableCellParserTest {

    private fun qzCell(
        day: Int,
        rowHeader: String,
        rowOrdinal: Int,
        text: String,
        fields: List<TimetableCellParser.RawField> = emptyList()
    ) = TimetableCellParser.RawTimetableCell(
        kind = "cell",
        source = "qiangzhi",
        day = day,
        rowHeader = rowHeader,
        rowOrdinal = rowOrdinal,
        text = text,
        fields = fields
    )

    @Test
    fun rowHeader_parsesCommonFormats() {
        assertEquals(1 to 2, TimetableCellParser.parseRowHeader("第一大节 (01,02小节) 08:00-09:35"))
        assertEquals(3 to 4, TimetableCellParser.parseRowHeader("第二大节"))
        assertEquals(3 to 4, TimetableCellParser.parseRowHeader("第2大节"))
        assertEquals(1 to 4, TimetableCellParser.parseRowHeader("第一大节 第二大节"))
        assertEquals(5 to 7, TimetableCellParser.parseRowHeader("第三大节\n05,06,07小节\n13:30-16:05"))
        assertEquals(1 to 2, TimetableCellParser.parseRowHeader("0102"))
        assertEquals(9 to 11, TimetableCellParser.parseRowHeader("091011"))
        assertEquals(3 to 3, TimetableCellParser.parseRowHeader("第三节"))
        assertEquals(1 to 2, TimetableCellParser.parseRowHeader("第一节 第二节"))
        assertEquals(1 to 2, TimetableCellParser.parseRowHeader("上午 1-2节"))
        assertNull(TimetableCellParser.parseRowHeader("上午"))
        assertNull(TimetableCellParser.parseRowHeader("午休"))
        assertNull(TimetableCellParser.parseRowHeader("备注：无"))
    }

    @Test
    fun qiangzhi_coursesWithoutSectionText_useRowHeaderInsteadOfIdSuffix() {
        // 旧版 bug：强智 div id "xxx-星期-2" 的末段 2 被当作第 2 大节，导致所有课被挤到 3-4 节
        val cells = listOf(
            qzCell(
                1, "第一大节 (01,02小节)", 0,
                "综合英语(必修)\n李老师\n1-16(周)\n一教211",
                listOf(
                    TimetableCellParser.RawField("老师", "李老师"),
                    TimetableCellParser.RawField("周次(节次)", "1-16(周)"),
                    TimetableCellParser.RawField("教室", "一教211")
                )
            ),
            qzCell(2, "第二大节 (03,04小节)", 1, "高等数学(基础)\n2-16(周)\n二教209"),
            qzCell(3, "第四大节 (07,08小节)", 3, "改革开放史\n1-8(周)"),
            qzCell(4, "第五大节 (09,10小节)", 4, "基础土壤学(必修)\n1-16(周)\nA0420")
        )
        val courses = TimetableCellParser.parseCells(cells).associateBy { it.dayOfWeek }

        assertEquals("综合英语", courses[1]!!.name)
        assertEquals(1, courses[1]!!.startSection)
        assertEquals(2, courses[1]!!.endSection)
        assertEquals("李老师", courses[1]!!.teacher)
        assertEquals("一教211", courses[1]!!.room)
        assertEquals(1, courses[1]!!.startWeek)
        assertEquals(16, courses[1]!!.endWeek)

        assertEquals("高等数学(基础)", courses[2]!!.name)
        assertEquals(3, courses[2]!!.startSection)
        assertEquals("二教209", courses[2]!!.room)
        assertEquals(2, courses[2]!!.startWeek)

        assertEquals(7, courses[3]!!.startSection)
        assertEquals(8, courses[3]!!.endWeek)

        assertEquals("基础土壤学", courses[4]!!.name)
        assertEquals(9, courses[4]!!.startSection)
        assertEquals("A0420", courses[4]!!.room)
    }

    @Test
    fun titledCourseNameAndCompactSectionsAndTrailingParity_areHandledCorrectly() {
        val cells = listOf(
            qzCell(
                1, "第一大节", 0,
                "水土保持学（选修）\n王教授\n1-15周(单)[0102节]\n学研A0302",
                listOf(
                    TimetableCellParser.RawField("课程名称", "水土保持学（选修）"),
                    TimetableCellParser.RawField("教师", "王教授"),
                    TimetableCellParser.RawField("周次(节次)", "1-15周(单)[0102节]"),
                    TimetableCellParser.RawField("教室", "学研A0302")
                )
            ),
            qzCell(
                2, "第二大节", 1,
                "树木学\n赵老师\n2-16(周)(双)[03-04]\n林业楼202"
            )
        )
        val courses = TimetableCellParser.parseCells(cells).associateBy { it.dayOfWeek }
        assertEquals("水土保持学", courses[1]!!.name)
        assertEquals(1, courses[1]!!.startSection)
        assertEquals(2, courses[1]!!.endSection)
        assertEquals(com.classtrack.app.data.model.WeekParity.ODD, courses[1]!!.weekParity)
        assertEquals(1, courses[1]!!.startWeek)
        assertEquals(15, courses[1]!!.endWeek)

        assertEquals("树木学", courses[2]!!.name)
        assertEquals(3, courses[2]!!.startSection)
        assertEquals(4, courses[2]!!.endSection)
        assertEquals(com.classtrack.app.data.model.WeekParity.EVEN, courses[2]!!.weekParity)
    }

    @Test
    fun qiangzhi_explicitThreeSections_mergeIntoSingleBlock() {
        // 3 节加长课在强智中会同时出现在第三、第四大节两个格子里
        val cells = listOf(
            qzCell(5, "第三大节 (05,06小节)", 2, "林学概论\n张三\n1-16(周)[05-06-07节]\n林业楼301"),
            qzCell(5, "第四大节 (07,08小节)", 3, "林学概论\n张三\n1-16(周)[05-06-07节]\n林业楼301")
        )
        val courses = TimetableCellParser.parseCells(cells)
        assertEquals(1, courses.size)
        assertEquals(5, courses[0].startSection)
        assertEquals(7, courses[0].endSection)
        assertEquals("张三", courses[0].teacher)
        assertEquals("林业楼301", courses[0].room)
    }

    @Test
    fun weeksAreNeverMistakenForSections() {
        val cells = listOf(qzCell(2, "第一大节", 0, "体育武术(男女)\n2-12(周)\n田径场"))
        val c = TimetableCellParser.parseCells(cells).single()
        assertEquals(1, c.startSection)
        assertEquals(2, c.endSection)
        assertEquals(2, c.startWeek)
        assertEquals(12, c.endWeek)
    }

    @Test
    fun explicitSectionOutsideRow_fallsBackToRow() {
        // 显式节次与所在行完全不重叠时视为误识别，以行为准
        val cells = listOf(qzCell(1, "第一大节 (01,02小节)", 0, "线性代数\n1-16(周)[09-10节]\n三教101"))
        val c = TimetableCellParser.parseCells(cells).single()
        assertEquals(1, c.startSection)
        assertEquals(2, c.endSection)
    }

    @Test
    fun zhengfang_absoluteRowFromTdId() {
        val cell = TimetableCellParser.RawTimetableCell(
            kind = "cell",
            source = "zhengfang",
            day = 3,
            rowStart = 3,
            rowEnd = 5,
            text = "数据结构★\n(3-5节)1-17周\n校区 实验楼B204\n王五",
            fields = listOf(
                TimetableCellParser.RawField("节/周", "(3-5节)1-17周"),
                TimetableCellParser.RawField("上课地点", "实验楼B204"),
                TimetableCellParser.RawField("教师", "王五")
            )
        )
        val c = TimetableCellParser.parseCells(listOf(cell)).single()
        assertEquals("数据结构", c.name)
        assertEquals(3, c.startSection)
        assertEquals(5, c.endSection)
        assertEquals(17, c.endWeek)
        assertEquals("实验楼B204", c.room)
        assertEquals("王五", c.teacher)
    }

    @Test
    fun oddWeeksAndDiscreteWeeks() {
        val cells = listOf(
            qzCell(1, "0304", 1, "大学物理实验\n1-15(单周)\n物理楼"),
            qzCell(2, "0506", 2, "形势与政策\n2,6,8-9(周)\n一教101")
        )
        val courses = TimetableCellParser.parseCells(cells).associateBy { it.dayOfWeek }
        assertEquals(com.classtrack.app.data.model.WeekParity.ODD, courses[1]!!.weekParity)
        assertEquals(3, courses[1]!!.startSection)
        assertEquals("2,6,8-9", courses[2]!!.weeksSpec)
        assertEquals(5, courses[2]!!.startSection)
    }

    @Test
    fun parseAuto_routesCellPayload() {
        val payload = """
            [{"kind":"cell","source":"qiangzhi","day":1,"rowStart":0,"rowEnd":0,
              "rowHeader":"第一大节 (01,02小节) 08:00-09:35","rowOrdinal":0,
              "text":"综合英语\n1-16(周)\n一教211","fields":[]},
             {"kind":"cell","source":"qiangzhi","day":1,"rowStart":0,"rowEnd":0,
              "rowHeader":"第五大节 (09,10小节)","rowOrdinal":4,
              "text":"高等数学\n1-16(周)\n二教501","fields":[]}]
        """.trimIndent()
        val result = CourseImporter.parseAuto(payload)
        assertTrue(result.success)
        assertEquals(2, result.courses.size)
        assertEquals(listOf(1, 9), result.courses.map { it.startSection }.sorted())
    }
}
