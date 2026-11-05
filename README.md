# 课轨 · ClassTrack 🎵

> **让课表如音乐专辑般流动。**
> 专为大学生打造的高颜值、120Hz 丝滑流体美学与音乐播放器质感课表应用。

---

## ✨ 核心特性

### 1. 🎨 7 款内置多元流派音乐人专属主题
- **华语流行**：周杰伦 · 《叶惠美 / 范特西》（复古琥珀摩卡金与古典羊皮纸暖调）
- **华语 R&B**：陶喆 · 《黑色柳丁》（美式 R&B 鲜明柳丁橙与曜石黑深邃撞色）
- **欧美流行**：Taylor Swift · 《Midnights / Lover》（午夜星辰靛蓝与浪漫霞光粉）
- **K-Pop**：aespa · 《Armageddon / Whiplash》（液态钛银银铬与赛博电光紫）
- **日音 J-Pop**：
  - ずっと真夜中でいいのに。(ZTMY) · 【真夜星轨】（录音室荧光薄荷与珊瑚霓虹）
  - ヨルシカ (Yorushika) · 【晴空夏草】（皇家晴空蓝与春泥棒珊瑚朱红）
  - 藤井 風 (Fujii Kaze) · 【落日微风】（落日赤陶橘与旅路松针绿）
- **高自由度定制**：支持导入/导出 JSON 自定义主题，可一键发送空值模板给 DeepSeek 等 AI 辅助生成配色。

### 2. 💽 全局「黑胶唱片柜」交互抽屉 (`Vinyl Theme Cabinet`)
- 拟物黑胶唱机展台，动态同心声槽旋转黑胶碟片 (`NOW SPINNING`)。
- 支持按 **全部 / 华语 / 欧美 / K-Pop / 日音 / 自定义** 流派快速筛选切碟。
- 在**顶栏主题胶囊**、**底部歌词条「换碟」按钮**与**主题工作台**均可一键随时调出。

### 3. ⚡ 120Hz 单层瑞士网格极简课表
- 告别笨重的百个卡片节点嵌套，采用**单层 Canvas 测量与精确定位渲染**，滑动跟手率与帧率达到原生 120Hz 满帧。
- 智能横向分页滑周（Pager），支持当前周一键直达、非当前周快速翻阅。
- 底部嵌入滚动式**音乐人代表作歌词状态条**，轻触即切下一首经典曲目。

### 4. 🚀 智能教务一键导课算法
- 覆盖**强智、正方/新正方、青果、树维/URP、金智**等主流高校教务系统。
- 智能识别与自愈：智能解析多周次合并课程、同课程合并跨节（如 3 节加长课连贯合并），容错处理非标准时间字段与脏数据。
- 支持教务系统内置 WebView 登录抓取、JSON/CSV 文本粘贴与分享口令导入。

### 5. 🪐 系统级流体云胶囊与实况通知
- 深度适配 **OPPO / OnePlus ColorOS 流体云实况胶囊** 与 Android 状态栏常驻通知。
- 精准课前倒计时与上课状态轮转（定宽平滑滚动，告别文本折行与重复堆砌）。
- 桌面拟物黑胶小组件（Widget），主屏幕随时感知下一堂课与当周课轨。

---

## 🛠️ 技术架构

- **UI 框架**：Jetpack Compose (Material 3) + 纯净光学液态玻璃修饰符 (`liquidGlassSurface`)
- **语言**：Kotlin 1.9+ (协程 Coroutines + StateFlow 响应式状态容器)
- **持久化**：本地安全 JSON 配置存储与自愈迁移机制
- **通知与系统胶囊**：NotificationManager + RemoteViews + ColorOS Fluid Cloud API
- **架构模式**：MVVM (Model-View-ViewModel) + 统一响应式 Repository 仓储模式

---

## 📦 本地编译与构建

### 前置要求
1. **JDK 17+**
2. **Android SDK Platform 34+**
3. **Gradle 8.7+**

### 构建 Debug 安装包
```bash
# 克隆仓库
git clone <your-repo-url>
cd ClassTrack

# 编译 Debug APK
./gradlew assembleDebug

# 构建产物位于：
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 开源许可证

本项目采用 [MIT License](LICENSE) 授权许可。
