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

        // 北京林业大学强智教务 7 行真实行头（非补零连写 + 首尾同为“12节”消歧）
        assertEquals(1 to 2, TimetableCellParser.parseRowHeader("12节", rowOrdinal = 0))
        assertEquals(3 to 4, TimetableCellParser.parseRowHeader("34节", rowOrdinal = 1))
        assertEquals(5 to 5, TimetableCellParser.parseRowHeader("5节", rowOrdinal = 2))
        assertEquals(6 to 7, TimetableCellParser.parseRowHeader("67节", rowOrdinal = 3))
        assertEquals(8 to 9, TimetableCellParser.parseRowHeader("89节", rowOrdinal = 4))
        assertEquals(10 to 11, TimetableCellParser.parseRowHeader("1011节", rowOrdinal = 5))
        assertEquals(12 to 12, TimetableCellParser.parseRowHeader("12节", rowOrdinal = 6))
    }

    @Test
    fun bjfu_realTimetable_week5AndWeek10_matchesGroundTruthExactly() {
        // 完整复现北京林业大学真实课表（含第1行与第7行同为"12节"、第3行"5节"仅1节、3节长课与4节实验课）
        val cells = listOf(
            // 周一 12节(row 0)：综合英语 1-16周 + 思想道德与法治 17-18周
            qzCell(
                1, "12节", 0,
                "综合英语(必修)\n李老师\n1-16(周)\n一教211\n--\n思想道德与法治(必修)\n张老师\n17-18(周)\n一教406"
            ),
            // 周一 34节(row 1) + 5节(row 2)：思想道德与法治 3节长课（多周次分段）
            qzCell(
                1, "34节", 1,
                "高等数学(基础)(必修)\n王老师\n1-2,7,9(周)\n二教209\n--\n思想道德与法治(必修)\n陈老师\n3-4(周)\n一教406\n--\n思想道德与法治(必修)\n陈老师\n6(周)\n一教406"
            ),
            qzCell(
                1, "5节", 2,
                "思想道德与法治(必修)\n陈老师\n3-4(周)\n一教406\n--\n思想道德与法治(必修)\n陈老师\n6(周)\n一教406"
            ),
            // 周二 67节(row 3)：基础土壤学 9-16周 @A0420
            qzCell(2, "67节", 3, "基础土壤学(必修)\n赵老师\n9-16(周)\nA0420"),
            // 周二 1011节(row 5)：无机及分析化学 9-16周 @二教208
            qzCell(2, "1011节", 5, "无机及分析化学(必修)\n孙老师\n9-16(周)\n二教208"),
            // 周三 67节(row 3)：改革开放史 1-8周
            qzCell(3, "67节", 3, "改革开放史(必修)\n周老师\n1-8(周)"),
            // 周三 1011节(row 5) + 12节(row 6)：雅思英语(公选) 6-15周 @二教608（3节长课）
            qzCell(3, "1011节", 5, "雅思英语(公选)\n张燕\n6-15(周)\n二教608"),
            qzCell(3, "12节", 6, "雅思英语(公选)\n张燕\n6-15(周)\n二教608"),
            // 周四 12节(row 0)：基础土壤学 1-7周 + 思想道德与法治 17-18周
            qzCell(
                4, "12节", 0,
                "基础土壤学(必修)\n赵老师\n1-7(周)\nA0420\n--\n思想道德与法治(必修)\n陈老师\n17-18(周)\n一教406"
            ),
            // 周四 34节(row 1)：基础土壤学 8周 + 体育武术（男女） 9-16周 @看台306
            qzCell(
                4, "34节", 1,
                "基础土壤学(必修)\n赵老师\n8(周)\nA0420\n--\n体育武术（男女）(必修)\n吴老师\n9-16(周)\n看台306"
            ),
            // 周四 67节(row 3) + 89节(row 4)：植物学实验B 9-16周 @生物楼214（4节连堂实验）
            qzCell(4, "67节", 3, "植物学实验B(必修)\n张贵芳\n9-16(周)\n生物楼214"),
            qzCell(4, "89节", 4, "植物学实验B(必修)\n张贵芳\n9-16(周)\n生物楼214"),
            // 周四 1011节(row 5)：思想道德与法治 5周 @一教406 + 无机及分析化学 9-16周 @二教208
            qzCell(
                4, "1011节", 5,
                "思想道德与法治(必修)\n陈老师\n5(周)\n一教406\n--\n无机及分析化学(必修)\n孙老师\n9-16(周)\n二教208"
            ),
            // 周四 12节(row 6)：思想道德与法治 5周 @一教406（与1011节缝合为 10-12 节 3节长课）
            qzCell(4, "12节", 6, "思想道德与法治(必修)\n陈老师\n5(周)\n一教406"),
            // 周五 67节(row 3)：综合英语 1-16周 @一教211
            qzCell(5, "67节", 3, "综合英语(必修)\n李老师\n1-16(周)\n一教211")
        )

        val rawParsed = TimetableCellParser.parseCells(cells)
        val merged = CourseImporter.mergeAndSanitizeCourses(rawParsed)

        // 验证第 5 周课表
        val week5 = merged.filter { it.isActiveInWeek(5) }
        // 周四早八 1-2 节：基础土壤学
        val thuMorningW5 = week5.single { it.dayOfWeek == 4 && it.startSection == 1 }
        assertEquals("基础土壤学", thuMorningW5.name)
        assertEquals(2, thuMorningW5.endSection)
        // 周四晚上 10-12 节：思想道德与法治（必须合并为单个 10..12 的 3 节长课，绝不分裂为 10-11 和 12！）
        val thuEveningW5 = week5.filter { it.dayOfWeek == 4 && it.startSection >= 10 }
        assertEquals(1, thuEveningW5.size)
        assertEquals("思想道德与法治", thuEveningW5.single().name)
        assertEquals(10, thuEveningW5.single().startSection)
        assertEquals(12, thuEveningW5.single().endSection)
        // 周三与周五下午课必须在 6-7 节（绝不偏移到 7-8 节）
        assertEquals(6 to 7, week5.single { it.dayOfWeek == 3 }.let { it.startSection to it.endSection })
        assertEquals(6 to 7, week5.single { it.dayOfWeek == 5 }.let { it.startSection to it.endSection })

        // 验证第 10 周课表（严格对齐 WakeUp 基准图）
        val week10 = merged.filter { it.isActiveInWeek(10) }
        // 1. 周一 1-2 节：综合英语（绝不跑到第 12 节）
        val monW10 = week10.single { it.dayOfWeek == 1 }
        assertEquals("综合英语", monW10.name)
        assertEquals(1 to 2, monW10.startSection to monW10.endSection)
        // 2. 周二 6-7 节 基础土壤学 + 10-11 节 无机及分析化学
        val tueW10 = week10.filter { it.dayOfWeek == 2 }.sortedBy { it.startSection }
        assertEquals(listOf(6 to 7, 10 to 11), tueW10.map { it.startSection to it.endSection })
        // 3. 周三 10-12 节 雅思英语（公选后缀已剥离，3节长课已合并）
        val wedW10 = week10.single { it.dayOfWeek == 3 }
        assertEquals("雅思英语", wedW10.name)
        assertEquals(10 to 12, wedW10.startSection to wedW10.endSection)
        // 4. 周四 3-4 节 体育武术 + 6-9 节 植物学实验B + 10-11 节 无机及分析化学（绝不互相吞噬）
        val thuW10 = week10.filter { it.dayOfWeek == 4 }.sortedBy { it.startSection }
        assertEquals(
            listOf("体育武术(男女)" to (3 to 4), "植物学实验B" to (6 to 9), "无机及分析化学" to (10 to 11)),
            thuW10.map { it.name to (it.startSection to it.endSection) }
        )
        // 5. 周五 6-7 节 综合英语
        val friW10 = week10.single { it.dayOfWeek == 5 }
        assertEquals(6 to 7, friW10.startSection to friW10.endSection)

        // 验证周一 3-5 节《思想道德与法治》在第 3, 4, 6 周均为 3..5 节 3 节长课
        val monPolW4 = merged.single { it.dayOfWeek == 1 && it.isActiveInWeek(4) && it.startSection >= 3 }
        assertEquals(3 to 5, monPolW4.startSection to monPolW4.endSection)
        val monPolW6 = merged.single { it.dayOfWeek == 1 && it.isActiveInWeek(6) && it.startSection >= 3 }
        assertEquals(3 to 5, monPolW6.startSection to monPolW6.endSection)
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
