package com.classtrack.app.data.model

/**
 * 预置高校标准作息时间表、三大音乐人双语歌词签、高校教务入口目录及默认配置
 */
object DefaultPresets {

    /**
     * 全国代表性高校教务系统预置目录（支持强智、正方/新正方、青果、树维/URP、金智等主流厂商）
     */
    val universityPortals: List<UniversityPortalPreset> = listOf(
        UniversityPortalPreset(
            id = "uibe_jwxt",
            schoolName = "对外经济贸易大学",
            portalLabel = "UIBE综合教务管理系统",
            systemType = "强智教务",
            url = "http://zhjw.uibe.edu.cn"
        ),
        UniversityPortalPreset(
            id = "uibe_bkjw",
            schoolName = "对外经济贸易大学",
            portalLabel = "UIBE本科教务课表 (bkjw新端)",
            systemType = "UIBE本科教务",
            url = "https://bkjw.uibe.edu.cn"
        ),
        UniversityPortalPreset(
            id = "bjfu_jwxt",
            schoolName = "北京林业大学",
            portalLabel = "北林教务系统",
            systemType = "强智教务",
            url = "http://newjwxt.bjfu.edu.cn"
        ),
        UniversityPortalPreset(
            id = "cau_jwxt",
            schoolName = "中国农业大学",
            portalLabel = "中农大本科生教务系统",
            systemType = "强智/综合教务",
            url = "http://jwxt.cau.edu.cn"
        ),
        UniversityPortalPreset(
            id = "bupt_jwxt",
            schoolName = "北京邮电大学",
            portalLabel = "北邮教学管理信息系统",
            systemType = "正方/综合教务",
            url = "https://jwxt.bupt.edu.cn"
        ),
        UniversityPortalPreset(
            id = "buaa_jwxt",
            schoolName = "北京航空航天大学",
            portalLabel = "北航本科教务系统",
            systemType = "综合教务",
            url = "https://jwxt.buaa.edu.cn"
        ),
        UniversityPortalPreset(
            id = "bit_jwxt",
            schoolName = "北京理工大学",
            portalLabel = "北理工本硕博一体化教务",
            systemType = "金智/综合教务",
            url = "https://jxzxehallapp.bit.edu.cn"
        ),
        UniversityPortalPreset(
            id = "ruc_jwxt",
            schoolName = "中国人民大学",
            portalLabel = "人大微师生教务系统",
            systemType = "金智/综合教务",
            url = "https://jw.ruc.edu.cn"
        ),
        UniversityPortalPreset(
            id = "bnu_jwxt",
            schoolName = "北京师范大学",
            portalLabel = "北师大教务网络管理系统",
            systemType = "青果教务",
            url = "http://zyfw.bnu.edu.cn"
        ),
        UniversityPortalPreset(
            id = "ustb_jwxt",
            schoolName = "北京科技大学",
            portalLabel = "北科大本科教学网",
            systemType = "强智教务",
            url = "https://jwgl.ustb.edu.cn"
        ),
        UniversityPortalPreset(
            id = "bjtu_jwxt",
            schoolName = "北京交通大学",
            portalLabel = "北交大本研一体化教务",
            systemType = "树维/URP",
            url = "https://aa.bjtu.edu.cn"
        ),
        UniversityPortalPreset(
            id = "zju_jwxt",
            schoolName = "浙江大学",
            portalLabel = "浙大现代教务管理系统",
            systemType = "新正方教务",
            url = "http://jwbinfosys.zju.edu.cn"
        ),
        UniversityPortalPreset(
            id = "whu_jwxt",
            schoolName = "武汉大学",
            portalLabel = "武大本科生教务管理系统",
            systemType = "新正方/综合教务",
            url = "https://cas.whu.edu.cn/authserver/login?service=https%3A%2F%2Fjwgl.whu.edu.cn"
        ),
        UniversityPortalPreset(
            id = "hust_jwxt",
            schoolName = "华中科技大学",
            portalLabel = "华科大本科教务系统",
            systemType = "综合教务",
            url = "http://hub.m.hust.edu.cn"
        ),
        UniversityPortalPreset(
            id = "fudan_jwxt",
            schoolName = "复旦大学",
            portalLabel = "复旦教务管理系统",
            systemType = "复旦教务",
            url = "https://jwfw.fudan.edu.cn"
        ),
        UniversityPortalPreset(
            id = "sjtu_jwxt",
            schoolName = "上海交通大学",
            portalLabel = "上交大教学信息服务网",
            systemType = "上交综合教务",
            url = "https://i.sjtu.edu.cn"
        ),
        UniversityPortalPreset(
            id = "nju_jwxt",
            schoolName = "南京大学",
            portalLabel = "南大教务信息管理系统",
            systemType = "南京大学教务",
            url = "https://jw.nju.edu.cn"
        )
    )

    val defaultTimeSlots: List<TimeSlot> = listOf(
        TimeSlot(1, "08:00", "08:45"),
        TimeSlot(2, "08:50", "09:35"),
        TimeSlot(3, "09:50", "10:35"),
        TimeSlot(4, "10:40", "11:25"),
        TimeSlot(5, "11:30", "12:15"),
        TimeSlot(6, "13:30", "14:15"),
        TimeSlot(7, "14:20", "15:05"),
        TimeSlot(8, "15:20", "16:05"),
        TimeSlot(9, "16:10", "16:55"),
        TimeSlot(10, "18:30", "19:15"),
        TimeSlot(11, "19:20", "20:05"),
        TimeSlot(12, "20:10", "20:55")
    )

    val defaultLyrics: List<LyricQuote> = listOf(
        // ZTMY ずっと真夜中でいいのに。
        LyricQuote(
            id = 1,
            artistTheme = ArtistTheme.ZTMY,
            songTitle = "秒針を噛む",
            japaneseLyric = "このまま奪って隠して忘れたい、分かり合う○と×もなくて。",
            chineseTranslation = "真想就这样夺走、藏起然后忘却，连彼此理解的对错都不复存在。",
            moodTag = "深夜电台 · Track 01"
        ),
        LyricQuote(
            id = 2,
            artistTheme = ArtistTheme.ZTMY,
            songTitle = "お勉強しといてよ",
            japaneseLyric = "理性的な感情論が綺麗で、また迷ってしまうけど。",
            chineseTranslation = "理性的感情论太过漂亮，让人不知不觉又陷入迷惘（记得好好学习哦）。",
            moodTag = "早八觉醒 · Study Mode"
        ),
        LyricQuote(
            id = 3,
            artistTheme = ArtistTheme.ZTMY,
            songTitle = "あいつら全員同窓会",
            japaneseLyric = "どうでもいいから置いてった、どうでもいいから進んでった。",
            chineseTranslation = "因为怎样都无所谓所以抛在脑后，因为怎样都无所谓所以大步向前。",
            moodTag = "课间律动 · Slap Bass"
        ),
        LyricQuote(
            id = 4,
            artistTheme = ArtistTheme.ZTMY,
            songTitle = "綺羅キラー (feat. Mori Calliope)",
            japaneseLyric = "焦るなよ焦るな、自分だけのテンポで踊ろう。",
            chineseTranslation = "别焦躁啊别焦躁，按属于自己的节拍起舞吧。",
            moodTag = "考前冲刺 · Neon Night"
        ),

        // Yorushika ヨルシカ
        LyricQuote(
            id = 5,
            artistTheme = ArtistTheme.YORUSHIKA,
            songTitle = "晴る",
            japaneseLyric = "晴れに晴れ、花よ咲け、咲いて春のせい。",
            chineseTranslation = "晴空万里，繁花盛放吧，一切都怪这明媚的春天。",
            moodTag = "晴空放课 · Sunny Breeze"
        ),
        LyricQuote(
            id = 6,
            artistTheme = ArtistTheme.YORUSHIKA,
            songTitle = "藍二乗",
            japaneseLyric = "エルマ、君なんだよ、僕だけの芸術は。",
            chineseTranslation = "艾尔玛，唯独你，才是我仅有的艺术啊。",
            moodTag = "文学间奏 · Indigo²"
        ),
        LyricQuote(
            id = 7,
            artistTheme = ArtistTheme.YORUSHIKA,
            songTitle = "春泥棒",
            japaneseLyric = "花見をしよう、散る花びらを拾うように時間を数えよう。",
            chineseTranslation = "去赏花吧，像拾起飘落的花瓣一样，细数流逝的时间。",
            moodTag = "午后小憩 · Spring Thief"
        ),
        LyricQuote(
            id = 8,
            artistTheme = ArtistTheme.YORUSHIKA,
            songTitle = "ただ君に晴れ",
            japaneseLyric = "追いつけないまま大人になって、君のポケットに夜が咲く。",
            chineseTranslation = "在尚未追赶上之时便已长大成人，你的口袋里绽放出夜色。",
            moodTag = "夏日余晖 · Summer Memo"
        ),

        // Fujii Kaze 藤井 風
        LyricQuote(
            id = 9,
            artistTheme = ArtistTheme.FUJII_KAZE,
            songTitle = "満ちてゆく",
            japaneseLyric = "手を放す、軽くなる、満ちてゆく。",
            chineseTranslation = "放开双手，变得轻盈，内心便渐渐充盈满溢。",
            moodTag = "松弛时刻 · Overflowing"
        ),
        LyricQuote(
            id = 10,
            artistTheme = ArtistTheme.FUJII_KAZE,
            songTitle = "きらり",
            japaneseLyric = "新しい日々も拙い過去も、全部がきらり。",
            chineseTranslation = "无论是崭新的日常还是笨拙的过去，全都闪闪发光。",
            moodTag = "赶课路上 · Kirari Groove"
        ),
        LyricQuote(
            id = 11,
            artistTheme = ArtistTheme.FUJII_KAZE,
            songTitle = "旅路",
            japaneseLyric = "僕らはまだ先の長い旅の中で、何かを爱したり忘れたり。",
            chineseTranslation = "我们仍在漫长的旅途之中，去爱上些什么，又遗忘些什么。",
            moodTag = "傍晚散步 · Sunset Vinyl"
        ),
        LyricQuote(
            id = 12,
            artistTheme = ArtistTheme.FUJII_KAZE,
            songTitle = "花",
            japaneseLyric = "枯れていく今この瞬間も、咲いている。",
            chineseTranslation = "即便在看似凋零的此时此刻，生命也依然在绽放。",
            moodTag = "周末呼吸 · Inner Flower"
        ),

        // Jay Chou 周杰伦
        LyricQuote(
            id = 13,
            artistTheme = ArtistTheme.JAY_CHOU,
            songTitle = "晴天",
            japaneseLyric = "故事的小黄花，从出生那年就飘着，童年的荡秋千，随记忆一直晃到现在。",
            chineseTranslation = "前奏的木吉他分解和弦一响，整个青春与晴朗的校园午后便扑面而来。",
            moodTag = "校园晴空 · Sunny Guitar"
        ),
        LyricQuote(
            id = 14,
            artistTheme = ArtistTheme.JAY_CHOU,
            songTitle = "以父之名",
            japaneseLyric = "仁慈的父我已坠入，看不见罪的国度，请原谅我的自负。",
            chineseTranslation = "古典歌剧与复古节拍交织，黄金年代的华语流行殿堂之作。",
            moodTag = "复古序曲 · Ye Hui Mei"
        ),
        LyricQuote(
            id = 15,
            artistTheme = ArtistTheme.JAY_CHOU,
            songTitle = "七里香",
            japaneseLyric = "窗外的麻雀在电线杆上多嘴，你说这一句很有夏天的感觉。",
            chineseTranslation = "手中的铅笔在纸上来来回回，记录每一堂课窗外的微风与蝉鸣。",
            moodTag = "夏日诗篇 · Summer Breeze"
        ),
        LyricQuote(
            id = 16,
            artistTheme = ArtistTheme.JAY_CHOU,
            songTitle = "夜曲",
            japaneseLyric = "为你弹奏萧邦的夜曲，纪念我死去的爱情，而我为你隐姓埋名，在月光下弹琴。",
            chineseTranslation = "晚自习归途的月色与耳机里清冷古典的钢琴前奏。",
            moodTag = "月光夜曲 · Nocturne"
        ),

        // David Tao 陶喆
        LyricQuote(
            id = 17,
            artistTheme = ArtistTheme.DAVID_TAO,
            songTitle = "黑色柳丁",
            japaneseLyric = "今天我心情有一点怪怪的，可是说不出到底为什么，好像有一些悲伤的征兆。",
            chineseTranslation = "当失真吉他与鼓点炸裂，把所有课业焦虑都化作摇滚与 R&B 的能量。",
            moodTag = "摇滚觉醒 · Black Tangerine"
        ),
        LyricQuote(
            id = 18,
            artistTheme = ArtistTheme.DAVID_TAO,
            songTitle = "普通朋友",
            japaneseLyric = "等待，我随时随地在等待，做你感情上的依赖，我没有任何的疑问，这是爱。",
            chineseTranslation = "丝滑的木吉他切音与灵动转音，陪你度过慵懒惬意的午后课间。",
            moodTag = "灵动转音 · Acoustic R&B"
        ),
        LyricQuote(
            id = 19,
            artistTheme = ArtistTheme.DAVID_TAO,
            songTitle = "流沙",
            japaneseLyric = "爱情好像流沙，我不挣扎，随它去吧，我不害怕。",
            chineseTranslation = "温柔醇厚的蓝调旋律，轻轻抚平一整天满课后的疲惫。",
            moodTag = "傍晚微风 · Soul Groove"
        ),
        LyricQuote(
            id = 20,
            artistTheme = ArtistTheme.DAVID_TAO,
            songTitle = "找自己",
            japaneseLyric = "哗啦啦啦啦啦天在下雨，哗啦啦啦啦啦云在哭泣，哗啦啦啦啦啦滴入我的心。",
            chineseTranslation = "就算遇上下雨天的早八，也要在轻快律动中找回属于自己的节奏。",
            moodTag = "雨天律动 · Rainy Beat"
        ),

        // Taylor Swift
        LyricQuote(
            id = 21,
            artistTheme = ArtistTheme.TAYLOR_SWIFT,
            songTitle = "Lavender Haze",
            japaneseLyric = "Meet me at midnight, staring at the ceiling with you, oh you don't ever say too much.",
            chineseTranslation = "在午夜十二点相逢，薰衣草薄雾与漫天星辰下的静谧时刻。",
            moodTag = "午夜星辰 · Midnights"
        ),
        LyricQuote(
            id = 22,
            artistTheme = ArtistTheme.TAYLOR_SWIFT,
            songTitle = "Anti-Hero",
            japaneseLyric = "It's me, hi, I'm the problem, it's me. At tea time, everybody agrees.",
            chineseTranslation = "坦率接纳偶尔手忙脚乱的自己，轻快合成器节拍陪你继续前行。",
            moodTag = "自信节拍 · Synth Pop"
        ),
        LyricQuote(
            id = 23,
            artistTheme = ArtistTheme.TAYLOR_SWIFT,
            songTitle = "Lover",
            japaneseLyric = "Can I go where you go? Can we always be this close forever and ever?",
            chineseTranslation = "暮色霞粉与复古华尔兹圆舞曲，把平凡的一天染上浪漫色彩。",
            moodTag = "浪漫霞光 · Pastel Sky"
        ),
        LyricQuote(
            id = 24,
            artistTheme = ArtistTheme.TAYLOR_SWIFT,
            songTitle = "Cruel Summer",
            japaneseLyric = "Fever dream high in the quiet of the night, you know that I caught it.",
            chineseTranslation = "静谧夏夜里滚烫的梦境与心跳，充满感染力的流行金曲。",
            moodTag = "盛夏热浪 · Cruel Summer"
        ),

        // aespa
        LyricQuote(
            id = 25,
            artistTheme = ArtistTheme.AESPA,
            songTitle = "Supernova",
            japaneseLyric = "Event horizon, 사건의 지평선 너머로, Supernova! Nova, can't stop hyperstellar.",
            chineseTranslation = "跨越事件视界，引爆超新星般的专注能量，开启高能学习状态。",
            moodTag = "超新星爆发 · Hyperstellar"
        ),
        LyricQuote(
            id = 26,
            artistTheme = ArtistTheme.AESPA,
            songTitle = "Whiplash",
            japaneseLyric = "One look give 'em Whiplash! Beat drop with a big flash, 집중해 좀 더 Think fast.",
            chineseTranslation = "极简凌厉的电子低音节拍，更专注、更敏锐地攻克每一个难题。",
            moodTag = "电光石火 · Think Fast"
        ),
        LyricQuote(
            id = 27,
            artistTheme = ArtistTheme.AESPA,
            songTitle = "Armageddon",
            japaneseLyric = "I'ma get it done! (Aw, wayo wayo) 널 향해 겨눠 Get it, gone!",
            chineseTranslation = "液态金属美学与笃定气场：锁定今日目标，把所有任务干脆拿下。",
            moodTag = "液态金属 · Get It Done"
        ),
        LyricQuote(
            id = 28,
            artistTheme = ArtistTheme.AESPA,
            songTitle = "Next Level",
            japaneseLyric = "I'm on the Next Level Yeah, 절대적 룰을 지켜, 내 손을 놓지 말아.",
            chineseTranslation = "打破常规界限，带着无畏的先锋姿态迈向下一个进阶维度。",
            moodTag = "进阶维度 · Next Level"
        )
    )

    /**
     * 初始默认课程列表为空。
     * 用户需通过教务系统导入、课表文件导入或手动添加来创建自己的课程。
     */
    val sampleCourses: List<CourseItem> = emptyList()
}
