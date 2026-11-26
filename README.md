# 课轨 (ClassTrack)

一个纯本地运行、无广告的 Android 大学课表应用。界面采用音乐专辑与黑胶唱片的配色思路，重点做了高校教务系统导课兼容、跨节长课排版，以及状态栏/通知栏的上课实时提醒。

所有课表和设置都保存在设备本地（SQLite 与 SharedPreferences），不需要注册账号，也不会上传课表数据。

## 功能概览

### 课表视图与作息管理
- **周课表网格**：支持左右滑动切换周次，非当前周浏览时可一键回到本周；自动合并连堂课（包括 3 节及以上的实验课、专业长课），处理单双周与离散周次。
- **自定义作息**：支持 6–16 节每日课表长度调整，可单独修改每节课上下课时刻，或按间隔顺延后续节次。
- **桌面小组件**：提供黑胶样式桌面 Widget，在主屏幕直接查看当天待上课程。

### 教务导课与数据备份
- **内置教务网页抓取**：通过应用内 WebView 登录学校教务系统后直接提取课表，针对二维表格跨行合并、同格多门课、非补零节次等常见教务页面格式做了兼容。目前已预置对外经济贸易大学（综合教务与 `bkjw` 新端）、北京林业大学、中国农业大学、北邮、北航、北理工、人大、北师大、浙大、武大、复旦、上交、南大等高校入口，支持强智、正方/新正方、青果、树维/URP、金智等常见教务系统，也可手动输入任意教务网址。
- **多格式导入导出**：支持导出完整 JSON 备份、生成分享口令，也支持粘贴 CSV 文本导入，或借助提示词让大模型将课表截图/文本整理为标准 JSON 后导入。

### 主题与界面风格
- **两种界面材质**：支持「经典纯净」（实色卡片与细边框）和「液态玻璃」（半透磨砂背景与漫射色块）切换。
- **7 套内置音乐人主题**：
  - 华语：周杰伦（叶惠美·范特西）、陶喆（黑色柳丁）
  - 欧美：Taylor Swift（Midnights / Lover）
  - K-Pop：aespa（Armageddon / Whiplash）
  - 日音：ずっと真夜中でいいのに。、ヨルシカ、藤井 風
- **唱片柜抽屉与歌词栏**：可按流派筛选切换主题，底栏附带对应歌手的歌词短句，点击可切换下一条；支持通过 JSON 导入自定义配色主题。

### 上课提醒与状态栏实况通知
- **定时调度**：基于 `AlarmManager` 排定课前提醒、上课切换与下课收起节点，支持提前 10 / 15 / 20 / 30 分钟提醒。
- **两种通知形态**：
  - **点阵进度卡**：首行展示时间与教室，配合 28 点横向进度条显示当前课程进度，支持在通知上点击「已到教室」直接收起。
  - **状态栏实况胶囊**：适配 Android 16 实况通知及部分国产系统顶栏胶囊协议（ColorOS 流体云、OriginOS 原子岛、HyperOS 焦点通知、MagicOS 实况窗）。
- **后台自检与穿戴同步**：设置页内置通知权限、精确闹钟、电池优化白名单与自启动引导检查，并支持将课前提醒同步至智能手表或手环。

## 开发环境与构建

### 环境要求
- JDK 21
- Android SDK（`compileSdk 36`，`minSdk 26`）
- Gradle 9.1（仓库自带 Wrapper）

### 常用命令

克隆仓库并构建 Debug 安装包：

```bash
git clone https://github.com/Vataraxl/ClassTrack.git
cd ClassTrack
./gradlew assembleDebug
```

生成的 Debug APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

运行本地单元测试：

```bash
./gradlew testDebugUnitTest
```

### 正式签名打包（可选）

如需构建带个人签名的 Release 包，可在项目根目录新建 `keystore.properties`（已被 `.gitignore` 忽略，不会提交到仓库）：

```properties
storeFile=keystore/classtrack-release.jks
storePassword=你的密钥库密码
keyAlias=你的密钥别名
keyPassword=你的密钥密码
```

然后执行：

```bash
./gradlew assembleRelease
```

产物位于 `app/build/outputs/apk/release/app-release.apk`。未配置 `keystore.properties` 时不影响常规克隆与调试编译。

## 目录结构

```text
app/src/main/kotlin/com/classtrack/app/
├── data/          # SQLite 数据库、课表解析导入器 (CourseImporter / TimetableCellParser)、Repository
├── system/        # 闹钟排程 (CourseAlarmScheduler)、实况通知与流体云适配、前台保活服务
├── ui/
│   ├── main/      # 周课表网格、导课与主题工作台、内置教务 WebView、系统设置页
│   └── theme/     # 7 套预设色板、字体排版、液态玻璃绘制修饰符与转场动效
└── widget/        # 桌面 AppWidget 小组件
```

## 开源协议

[MIT License](LICENSE)
