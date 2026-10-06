# MonoGallery

一款 **Android 文件管理工具**。以卡片的形式组织文件夹与文件，支持多选、复制/移动、重命名、压缩、分享等操作，界面层级与磁盘目录一一对应；内置文字编辑、图片预览、音频播放、视频播放与 PDF 预览。

## 功能特性

- **文件夹与文件卡片**：统一方形卡片（2 列网格），文件夹可嵌套，卡片分标题栏与内容预览栏（文件夹预览显示项目数量，子文件夹算 1 项）。
- **新建与导入**：顶部 `+` 展开「新建文件夹 / 新建TXT文件」，以及「导入文件夹 / 导入文件」（从系统文件选择器导入）；导入时可选保留或不保留原文件，不保留即在导入后删除原文件。根目录与文件夹内保持一致。
- **文件夹卡片**：单击预览区打开、点击名字重命名、长按进入多选（与文件卡片一致）。
- **文件卡片**：
  - 名字与扩展名放在一起显示，名字与内容预览之间有一条分隔线；
  - 图片文件在预览区显示**缩略图**，文字文件显示正文前几行，音频文件显示音符图标，PDF 文件显示**首页封面**；
  - 点击**名字区域**重命名，点击**预览区域**打开文件；
  - 长按进入多选。
- **文字编辑**：文字文件（`.txt`）可直接编辑并自动保存（400ms 防抖）。
- **图片预览**：图片文件全屏预览，等比显示、支持双指缩放与拖动，自动处理 EXIF 旋转方向；标题栏浮于图片之上，点击图片可隐藏 / 显示标题栏。
- **音频播放**：音频文件（mp3 / wav / m4a / aac / ogg / flac 等）预览页，含可拖动进度条、总时长 / 播放时间 / 剩余时间、播放 / 暂停、前后 10 秒跳转；点击播放时间可输入时间跳转。
- **视频播放**：视频文件（mp4 / mkv / mov / avi / webm / m4v / 3gp 等）卡片预览区显示**视频帧缩略图**与播放角标；打开后全屏等比播放并自动开始，含播放 / 暂停（画面中央大按钮）、可拖动进度条、播放时间 / 剩余时间、前后 10 秒跳转，点击播放时间可输入时间跳转；点击画面切换控制层，播放时控制层 3 秒后自动隐藏，**标题栏浮于视频之上并与控制层一起淡入淡出**（视频真正全屏铺满，不被标题栏挤压）；**右下角按钮可切换横屏 / 竖屏**（旋转不中断播放），横屏时按返回先回到竖屏，竖屏按返回先停止播放再退出，无画面残留。
- **PDF 预览**：PDF 文件（`.pdf`）卡片以**首页封面**作为预览；打开后全部页面纵向排列，可从上到下连续滑动阅读，页面随滚动位置按需渲染、滑出后自动回收内存；标题栏浮于页面之上，点击页面可与底部工具条一起隐藏 / 显示。
- **重命名**：支持修改扩展名；扩展名变更时二次确认；重名时在名称后自动追加 ` (2)`、` (3)`…（Windows 风格）。
- **排序**：右上角 `⋮` 菜单可切换「按名称排序 / 按类型排序 / 按日期排序」，另有「刷新」——名称由小到大（不区分大小写），类型按扩展名由小到大、同扩展名再按名称（文件夹没有类型，该模式下同按名称），日期由近及远（文件夹按创建时间、文件按修改时间，最新在前）；当前排序方式带对勾标记，默认按名称排序；**所选排序方式会被记住**（全局生效，深入子文件夹或重启应用后依然保持，不会退回默认的名称排序）。
- **多选**：底部栏提供「添加到 / 删除 / 压缩 / 复制到剪切板」，返回键退出多选，删除带确认弹窗；「删除 / 添加到 / 压缩」对文件与文件夹同样生效（文件夹递归），「复制到剪切板」仅收集文件文字。
- **添加到文件夹**：浏览式选择——点击文件夹进入其内部、「上一层」返回上级、「到根目录」cd 到根目录，底部「确认」后弹出「复制 / 移动 / 取消」。
- **分享**：文件/文件夹可分享；文件夹分享会先弹出「压缩并分享」进度对话框，后台打包为 `.zip` 后自动打开系统分享面板。
- **属性页**：展示名称、类型、大小与时间。

## 技术栈

- Kotlin + Jetpack Compose（Material 3）
- Navigation Compose（字符串路由）
- 内存仓库 + 写穿透持久化（`MonoStorage`）
- 界面偏好持久化（`SortPreference`，SharedPreferences）

| 项 | 值 |
| --- | --- |
| 包名 / applicationId | `com.sichuan.monogallery` |
| compileSdk / targetSdk | 36 |
| minSdk | 29 |
| Kotlin | 2.0.21 |
| AGP | 8.13.2 |
| Compose BOM | 2024.12.01 |
| JVM target | 11 |

## 数据存储

应用文件保存在共享外部存储根目录：

```
/storage/emulated/0/MonoGallery/
```

磁盘目录结构镜像界面层级：

- 文件夹 = 目录
- 文件 = 文件（文件名 = 名称 + 扩展名，如 `随笔.txt`）

> 读写共享存储需要授权：Android 11+ 授予「所有文件访问权限」，Android 10 授予存储权限。

## 项目结构

```
app/src/main/java/com/sichuan/monogallery/
├── MainActivity.kt        # 应用入口，NavHost 路由装配 + 存储权限
├── Model.kt               # 数据模型：FileType / MonoFile / Folder / SortMode
├── SortPreference.kt      # 持久化的界面偏好（SharedPreferences，记住所选排序方式）
├── MonoStorage.kt         # 本地磁盘读写，目录镜像界面层级
├── MonoLibrary.kt         # 内存仓库，改动即时写回磁盘
├── MonoSharing.kt         # 系统分享（文件 / 文件夹 zip）与分享进度弹窗
├── MonoComponents.kt      # 通用 UI 组件：页面骨架 / 命名表单 / 确认弹窗 / 双列网格
├── MonoCards.kt           # 卡片（含图片缩略图）与各类弹窗
├── MonoItemGrid.kt        # 双列卡片网格（卡片区域）
├── FileSelection.kt       # 多选状态 + 底部操作栏
├── AddMenu.kt             # 「+」新建 / 导入菜单
├── MoreMenu.kt            # 「⋮」菜单（刷新 / 按名称、类型或日期排序）
├── HomeScreen.kt          # 首页（根目录）
├── FolderContentScreen.kt # 文件夹内容页
├── FolderPickerScreen.kt  # 「添加到文件夹」目标选择页
├── FileViewScreen.kt       # 文件查看页（按类型分发）
├── FullscreenPreviewScreen.kt # 通用全屏预览布局（视频 / 图片 / PDF 复用：内容铺满 + 叠加标题栏）
├── TextEditor.kt          # 文字查看 / 编辑
├── ImageViewer.kt         # 图片全屏预览（缩放 / 拖动）
├── AudioPlayer.kt         # 音频预览页（MediaPlayer，进度条 / 播放控制）
├── VideoPlayer.kt         # 视频预览页（MediaPlayer + TextureView，全屏等比 / 横竖屏切换 / 控制层自动隐藏）
├── PdfViewer.kt           # PDF 预览页（PdfRenderer，纵向逐页 / 按需渲染）
├── BitmapSupport.kt       # 缩略图共用设施：内存缓存 / 降采样计算 / 异步加载骨架
├── ImageLoader.kt         # 图片解码（降采样 / EXIF 方向）
├── VideoLoader.kt         # 视频帧提取（MediaMetadataRetriever，卡片缩略图）
├── PdfLoader.kt           # PDF 首页封面渲染（卡片缩略图）
├── CompressScreen.kt      # 压缩界面（输入名称，文件/文件夹混选压缩）
├── PropertiesScreen.kt    # 文件 / 文件夹属性页
└── MonoColors.kt          # 主题配色
```

### 复用约定

新增页面或加载器时优先复用下列单一实现，避免再次出现平行实现：

| 场景 | 复用点 |
| --- | --- |
| 带返回键与标题的整页骨架 | `MonoComponents.kt` 的 `MonoScaffold` |
| 只输入一个名称的表单页 | `MonoComponents.kt` 的 `NameFormScreen` |
| 输入名称 / 确认操作的弹窗 | `MonoComponents.kt` 的 `NameInputDialog`、`ConfirmDialog` |
| 双列卡片网格 | `MonoComponents.kt` 的 `MonoGrid` |
| 卡片缩略图（图片 / 视频帧 / PDF 封面） | `BitmapSupport.kt` 的 `rememberBitmapResult`、`BitmapLruCache`、`fitWithin`；卡片侧统一走 `MonoCards.kt` 的 `ThumbnailBox` |
| 名称去重（追加 ` (2)`、` (3)`…） | `MonoStorage.kt` 的顶层 `uniqueName` |
| 多选 id 拆成文件 / 文件夹 | `MonoLibrary.partitionIds` |
| 全屏预览（视频 / 图片 / PDF） | `FullscreenPreviewScreen.kt` |

## 构建与运行

在 Android Studio 中打开项目直接运行，或使用命令行：

```bash
# macOS / Linux
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

生成的调试包位于 `app/build/outputs/apk/debug/`。

## 使用说明

1. 首页点击 `+` 新建文件夹 / TXT 文件，或从系统选择器导入文件与文件夹；点击文件夹卡片进入，点击文件卡片打开。
2. **排序**：点右上角 `⋮`，可按名称（小→大）、类型（按扩展名）或日期（近→远）排列当前文件夹与文件，也可在此刷新列表。所选排序方式全局生效并被持久化，切换文件夹或重启应用后不会退回默认排序。
3. **重命名文件**：点击文件卡片上的名字区域；修改扩展名会弹出确认。
4. **多选**：长按任意文件或文件夹卡片，底部出现操作栏，可批量添加到文件夹、删除、压缩或复制到剪切板。
5. **压缩**：多选后点底部栏「压缩」，进入压缩界面，输入压缩名称（默认第一个卡片名），将选中的文件与文件夹压缩为一个 `.zip`。
