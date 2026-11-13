package com.classtrack.app.data.model

import kotlinx.serialization.Serializable

/**
 * 音乐主题流派分类枚举
 */
@Serializable
enum class MusicGenre(val label: String, val shortLabel: String) {
    ALL("全部专辑", "全部"),
    MANDOPOP("华语流行", "华语"),
    ENGLISH("欧美流行", "欧美"),
    KPOP("K-Pop", "K-Pop"),
    JPOP("日音精选", "日音"),
    CUSTOM("自定义", "自定义");

    val displayName: String get() = shortLabel
}

/**
 * 内置多元流派音乐人专属主题枚举（涵盖华语、欧美、K-Pop、日音，同时支持 JSON 自定义主题）
 */
@Serializable
enum class ArtistTheme(
    val displayName: String,
    val artistName: String,
    val albumConcept: String,
    val subtitle: String,
    val badgeText: String,
    val genre: MusicGenre = MusicGenre.JPOP,
    val isDarkPreset: Boolean = true
) {
    ZTMY(
        displayName = "真夜霓虹",
        artistName = "ずっと真夜中でいいのに。",
        albumConcept = "ACAね · 深夜电台磁带",
        subtitle = "暗夜录音室 × 荧光薄荷/珊瑚粉 · 赛博律动",
        badgeText = "ZTMY · MIDNIGHT",
        genre = MusicGenre.JPOP,
        isDarkPreset = true
    ),
    YORUSHIKA(
        displayName = "盗作之夏",
        artistName = "ヨルシカ (Yorushika)",
        albumConcept = "n-buna & suis · 夏日文学信封",
        subtitle = "通透海蓝 × 纯白留白 · 诗意手帐",
        badgeText = "YORUSHIKA · SUMMER",
        genre = MusicGenre.JPOP,
        isDarkPreset = false
    ),
    FUJII_KAZE(
        displayName = "落日微风",
        artistName = "藤井 風 (Fujii Kaze)",
        albumConcept = "HEHN RECORDS · 复古黑胶唱盘",
        subtitle = "落日赤陶橘 × 旅路松针绿 · 胶片松弛感",
        badgeText = "KAZE · GROOVE",
        genre = MusicGenre.JPOP,
        isDarkPreset = false
    ),
    JAY_CHOU(
        displayName = "叶惠美·范特西",
        artistName = "周杰伦 (Jay Chou)",
        albumConcept = "JVR Music · 古典羊皮纸与复古摩卡金",
        subtitle = "古典暖米羊皮纸 × 琥珀流金/橄榄绿 · 华语经典",
        badgeText = "JAY · FANTASY",
        genre = MusicGenre.MANDOPOP,
        isDarkPreset = false
    ),
    DAVID_TAO(
        displayName = "黑色柳丁",
        artistName = "陶喆 (David Tao)",
        albumConcept = "Shock Records · 鲜明柳丁橙与曜石黑",
        subtitle = "曜石纯黑 × 柳丁暖橙/蓝调琥珀 · R&B 律动",
        badgeText = "TAO · SOUL R&B",
        genre = MusicGenre.MANDOPOP,
        isDarkPreset = true
    ),
    TAYLOR_SWIFT(
        displayName = "午夜星辰",
        artistName = "Taylor Swift",
        albumConcept = "Midnights & Lover · 星夜靛蓝与霞粉",
        subtitle = "午夜薰衣草深蓝 × 浪漫霞粉/星光紫 · 欧美流行",
        badgeText = "SWIFT · MIDNIGHTS",
        genre = MusicGenre.ENGLISH,
        isDarkPreset = true
    ),
    AESPA(
        displayName = "超新星金属",
        artistName = "aespa (에스파)",
        albumConcept = "Armageddon & Whiplash · 液态银铬电光紫",
        subtitle = "金属银铬深空 × 赛博电光紫/激光冰青 · K-Pop",
        badgeText = "AESPA · SUPERNOVA",
        genre = MusicGenre.KPOP,
        isDarkPreset = true
    ),
    CUSTOM_JSON(
        displayName = "自定义主题",
        artistName = "Custom JSON Theme",
        albumConcept = "AI 定制概念主题",
        subtitle = "通过主题 JSON 文件导入的个性化皮肤",
        badgeText = "CUSTOM · THEME",
        genre = MusicGenre.CUSTOM,
        isDarkPreset = true
    );

    companion object {
        val builtInThemes: List<ArtistTheme> = listOf(
            JAY_CHOU,
            DAVID_TAO,
            TAYLOR_SWIFT,
            AESPA,
            ZTMY,
            YORUSHIKA,
            FUJII_KAZE
        )
    }
}

/**
 * 界面质感风格枚举：
 * CLASSIC: 经典质感（专辑拟物、实体边框、原生极简）
 * GLASS: 液态玻璃（类 iOS 18 拟态磨砂玻璃、晶莹通透、高光倒角折射、悬浮毛玻璃 Dock 栏与环境光晕）
 */
@Serializable
enum class UiStyle(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val badge: String
) {
    CLASSIC(
        id = "classic",
        displayName = "经典质感",
        subtitle = "专辑拟物 · 原生极简",
        badge = "CLASSIC"
    ),
    GLASS(
        id = "glass",
        displayName = "液态玻璃",
        subtitle = "类 iOS 18 · 晶莹通透 · 高光折射",
        badge = "LIQUID GLASS"
    )
}

/**
 * 自定义主题中的歌词条目（方便用户让 DeepSeek 等 AI 一并生成代表作歌词）
 */
@Serializable
data class CustomLyricItem(
    val songTitle: String,
    val japaneseLyric: String,
    val chineseTranslation: String = ""
)

/**
 * 可通过 JSON 文件导入/导出的自定义主题完整配置模型
 * 用户可直接复制模板发给 DeepSeek 等免费 AI 修改配色与歌词后导入《课轨》
 */
@Serializable
data class CustomThemeConfig(
    // 主题唯一标识（由 App 在新建时自动分配，AI 无需填写）
    val id: String = "",
    val themeName: String = "群青夜游",
    val artistName: String = "YOASOBI / 自定义音乐人",
    val subtitle: String = "夜色群青 × 星光霓虹 · AI 定制主题",
    val isDark: Boolean = true,
    val fontStyle: String = "SANS", // "SANS" | "SERIF" | "MONO"
    val cardCornerDp: Int = 10,
    val backgroundHex: String = "#0B1021",
    val surfaceHex: String = "#151C36",
    val surfaceVariantHex: String = "#1F294D",
    val primaryHex: String = "#38BDF8",
    val secondaryHex: String = "#F43F5E",
    val tertiaryHex: String = "#A855F7",
    val onBackgroundHex: String = "#F1F5F9",
    val onSurfaceHex: String = "#E2E8F0",
    val mutedTextHex: String = "#94A3B8",
    val borderHex: String = "#2E3B66",
    val trackBackgroundHexList: List<String> = listOf(
        "#172554",
        "#3B0764",
        "#4C0519",
        "#064E3B",
        "#312E81",
        "#451A03"
    ),
    val trackAccentHexList: List<String> = listOf(
        "#38BDF8",
        "#C084FC",
        "#FB7185",
        "#34D399",
        "#818CF8",
        "#FBBF24"
    ),
    val customLyrics: List<CustomLyricItem> = listOf(
        CustomLyricItem(
            songTitle = "群青",
            japaneseLyric = "知らず知らず隠してた、本当の声を響かせてよ、ほら。",
            chineseTranslation = "将不知不觉间隐藏起来的、真正的声音响彻出来吧。"
        ),
        CustomLyricItem(
            songTitle = "夜に駆ける",
            japaneseLyric = "騒がしい日々に笑えない君に、思いつく限り眩しい明日を。",
            chineseTranslation = "致在喧嚣日常中难以展露笑颜的你，献上我所能想到最耀眼的明天。"
        )
    )
)

/**
 * 单双周枚举
 */
@Serializable
enum class WeekParity(val label: String) {
    ALL("每周"),
    ODD("单周"),
    EVEN("双周");

    fun matchesWeek(week: Int): Boolean = when (this) {
        ALL -> true
        ODD -> week % 2 == 1
        EVEN -> week % 2 == 0
    }
}

/**
 * 课程实体模型
 */
@Serializable
data class CourseItem(
    val id: Long = 0L,
    val name: String,
    val room: String = "",
    val teacher: String = "",
    val dayOfWeek: Int = 1, // 1 = 周一 ... 7 = 周日
    val startSection: Int = 1, // 1..maxSections
    val endSection: Int = 2,   // 1..maxSections
    val startWeek: Int = 1,
    val endWeek: Int = 18,
    val weekParity: WeekParity = WeekParity.ALL,
    val weeksSpec: String = "", // 支持离散周次，如 "2,6,8-9,12-18"
    val trackColorIndex: Int = 0,
    val note: String = ""
) {
    fun isActiveInWeek(week: Int): Boolean {
        val inWeek = if (weeksSpec.isNotBlank()) {
            parseWeeksSpec(weeksSpec).contains(week)
        } else {
            week in startWeek..endWeek
        }
        return inWeek && weekParity.matchesWeek(week)
    }

    fun sectionRangeText(): String {
        return if (startSection == endSection) {
            "%02d 节".format(startSection)
        } else {
            "%02d-%02d 节".format(startSection, endSection)
        }
    }

    fun weekRangeText(): String {
        val paritySuffix = if (weekParity == WeekParity.ALL) "" else "(${weekParity.label})"
        if (weeksSpec.isNotBlank()) {
            return if (weeksSpec.contains("单") || weeksSpec.contains("双")) {
                "${weeksSpec}周"
            } else {
                "${weeksSpec}周$paritySuffix"
            }
        }
        return "${startWeek}-${endWeek}周$paritySuffix"
    }

    companion object {
        fun parseWeeksSpec(spec: String): Set<Int> {
            val result = mutableSetOf<Int>()
            val cleaned = spec
                .replace("周", "")
                .replace("(", "")
                .replace(")", "")
                .replace("（", "")
                .replace("）", "")
                .replace("~", "-")
                .replace("～", "-")
                .replace("—", "-")
                .trim()
            cleaned.split(",", "，").forEach { part ->
                val p = part.trim()
                if (p.contains("-")) {
                    val bounds = p.split("-")
                    val s = bounds.getOrNull(0)?.trim()?.toIntOrNull()
                    val e = bounds.getOrNull(1)?.trim()?.toIntOrNull()
                    if (s != null && e != null && s <= e) {
                        for (w in s..e) result.add(w)
                    }
                } else {
                    p.toIntOrNull()?.let { result.add(it) }
                }
            }
            return result
        }
    }
}

/**
 * 作息时间节次配置
 */
@Serializable
data class TimeSlot(
    val section: Int,
    val startTime: String, // "HH:mm"
    val endTime: String    // "HH:mm"
)

/**
 * 日音歌词日签模型
 */
@Serializable
data class LyricQuote(
    val id: Int,
    val artistTheme: ArtistTheme,
    val songTitle: String,
    val japaneseLyric: String,
    val chineseTranslation: String,
    val moodTag: String
)

/**
 * 课程实时播放状态
 */
enum class TrackPlayState {
    UPCOMING,
    NOW_PLAYING,
    COMPLETED
}

/**
 * 今日课轨包装模型
 */
data class TodayTrackUiModel(
    val trackNumber: Int,
    val course: CourseItem,
    val startTime: String,
    val endTime: String,
    val playState: TrackPlayState,
    val progressFraction: Float,
    val minutesUntilStartOrEnd: Int
)

/**
 * 高校教务系统导航预设模型（支持扩展全国各大高校）
 */
@Serializable
data class UniversityPortalPreset(
    val id: String,
    val schoolName: String,
    val portalLabel: String,
    val systemType: String,
    val url: String
)

/**
 * 通知栏流体云 / 实况卡片呈现样式
 */
enum class NotificationCardStyle(
    val displayName: String,
    val description: String
) {
    DOT_CARD(
        displayName = "极简点阵流体卡",
        description = "高清点阵时间轴 · 时间地点首行聚焦 · 紧凑交互胶囊（推荐）"
    ),
    LIVE_CAPSULE(
        displayName = "状态栏实况胶囊",
        description = "Android 16 原生药丸胶囊（显示在状态栏左上角时钟旁）"
    )
}

/**
 * 完整课表备份与导出 JSON 模型（包含每日总节数、作息时间表、课程列表与可选自定义主题）
 */
@Serializable
data class ScheduleBackup(
    val version: Int = 2,
    val scheduleName: String = "我的课表",
    val currentWeek: Int = 8,
    val totalWeeks: Int = 18,
    val maxSections: Int = 12,
    val artistTheme: ArtistTheme = ArtistTheme.ZTMY,
    val courses: List<CourseItem> = emptyList(),
    val timeSlots: List<TimeSlot> = DefaultPresets.defaultTimeSlots,
    val customTheme: CustomThemeConfig? = null
)
