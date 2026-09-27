# 合成大奶蛋

暖黄色 2048 小游戏，包含可直接在浏览器游玩的静态版和原生安卓版本。1.1 版修复网页版新生成及合成方块的抽搐，两版会在动画结束后处理期间最后一次滑动，合成音与滑动动画同时开始。两版底部均显示“创意：hzr”“制作：ytw”。

## 网页版

GitHub Pages 地址：[在线游玩](https://mousemaster114.github.io/naiwa2048/)。网页版位于 `docs/`，手机滑动或桌面方向键都可操作；得分、棋盘和声音设置保存在浏览器本地。

本地预览：运行 `node scripts/serve-site.mjs`，打开 `http://127.0.0.1:8787/`。规则测试：`node --test docs/engine.test.mjs`。所有网页素材均在 `docs/assets/`，页面不依赖外部库或网络接口。

## 安卓版

Kotlin + Android View/Canvas，支持 Android 8.0（API 26）及以上，无网络权限、广告或账号。

## 安装

交付目录中的 `合成大奶蛋-1.1.apk` 为调试签名安装包，适合个人安装体验，不用于商店发布。将 APK 传到手机，允许文件管理器安装此来源的应用后打开即可。

## 构建

需要 JDK 17、Android SDK Platform 34、Build Tools 36.1.0，以及首次构建时可访问 Google Maven/Maven Central 的网络。使用 Gradle 8.13、Android Gradle Plugin 8.13.2 和 Kotlin 2.3.0。

1. 在 `local.properties` 配置 `sdk.dir` 指向本机 Android SDK（不要提交这个文件）。
2. Windows 执行 `./gradlew.bat :app:assembleDebug :app:lintDebug`。测试可执行 `./gradlew.bat :app:testDebugUnitTest`；若中文用户路径导致 Gradle 测试进程异常，使用下述测试脚本。
3. 安装包输出为 `app/build/outputs/apk/debug/app-debug.apk`。

如果已有本地 Maven 仓库，可以设置环境变量 `NAIWA_MAVEN_REPO` 后使用 `--offline` 构建；普通联网构建无需此变量。

Windows 中文用户路径下，如果 Gradle 测试进程启动失败，运行 `./scripts/test-windows.ps1` 直接启动相同 JUnit 测试。可用 `-GradleExecutable` 指定已安装的 Gradle，或用 `-RobolectricRuntimeDirectory` 指定已下载的 Robolectric Android 运行时目录。测试首次运行需要下载 Android 13/14 的 Robolectric 运行时。若 Kotlin 编译守护进程无法访问用户临时目录，构建时可追加 `-Pkotlin.compiler.execution.strategy=in-process`。

## 玩法

- 上下左右滑动，两个相同数字合成翻倍；每个方块在同一轮只能合成一次。
- 开局两个随机方块；每次有效移动后产生一个方块，2/4 的概率为 90%/10%。通关步不再生成新方块。
- 合成数字累加得分，达到 2048 即通关；无空位且无法合成时结束。
- 每次有合成的滑动随机播放一个音效，即使同时合成多次也只播放一次。
- 通关合成先触发合成音，在弹窗出现时切换至 `end.mp3`。后台停止播放，恢复界面不重播通关音。
- 自动保存进度、最高分和声音设置。重新开始需要确认。

## 工程结构

- `Game.kt`：独立规则引擎、随机生成和移动轨迹。
- `BoardView.kt`：棋盘绘制、手势识别、滑动/合成/生成动画。
- `MainActivity.kt`：界面、计分、存档、弹窗与生命周期。
- `GameAudio.kt`：随机合成音、通关音及播放器释放。
- `ConfettiView.kt`：通关粒子动效。
- `GameTest.kt`：规则自动化测试。

## 素材对应

保留工作区原始素材，应用中的资源副本为：

| 原文件 | Android 资源 |
|---|---|
| `picture.jpg` | `res/drawable-nodpi/picture.jpg` |
| `music/安迪.mp3` | `res/raw/merge_one.mp3` |
| `music/嘎嘎滴辣虾.mp3` | `res/raw/merge_two.mp3` |
| `end.mp3` | `res/raw/end.mp3` |

## 手动验收

安装后检查四向滑动、快速连续滑动、声音开关、重新开始确认、切后台再恢复和强制结束后重新打开。检查小屏上棋盘与按钮、1024/2048 数字清晰度；通关时标题应为“恭喜你合成了大奶蛋”，其下完整显示图片并播放一次通关音。多处合成一次滑动只播放一次声音。

本机的设备/模拟器可用情况和实际自动检查结果见交付目录 `验证结果.md`。
