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
- **排序**：右上角 `⋮` 菜单可切换「按名称排序 / 按类型排序 / 按日期排序」，另有「刷新」——名称由小到大（不区分大小写），类型按扩展名由小到大、同扩展名再按名称（文件夹没有类型，该模式下同按名称），日期由近及远（文件夹按创建时间、文件按修改时间，最新在前）；当前排序方式带对勾标记，默认按名称排序；**所选排序方式会被记住，且按列表分别记忆**（库的页面共用一个选择，深入子文件夹或重启应用后依然保持；「本地图片」另有自己的一份，两个列表的排序互不影响，不会退回默认的名称排序）。
- **底部导航**：首页底部为「Mono（首页）/ 本地 / 工具 / 设置」四个标签，点击即切换到对应页面（标签选中的是当前页面，同一标签重复点击不会重复入栈；按返回键回到首页而不是在标签之间回退）；进入子文件夹后该栏隐藏。其中「本地」已放入「本地图片」卡片（该页顶栏也有 `⋮` 菜单），「工具 / 设置」仍是空白占位页（页面骨架：顶栏标题 + 底部导航栏）。
- **本地图片**：`本地` 标签页中的「本地图片」卡片把**所有不属于 MonoGallery 的图片**收进同一个虚拟文件夹——即设备存储中位于 MonoGallery 目录及其子目录之外的图片，无视它们原本分散在哪些文件夹里（扁平展示）；点击卡片进入该页，点击图片全屏查看（可缩放、拖动，点击画面隐藏 / 显示标题栏），**退出全屏后网格停在原来的滚动位置**（不会跳回顶部）。该页与库内页面拥有同一套顶栏与卡片行为：顶栏 `⋮` 菜单可「刷新」（重新扫描）与切换三种排序（本地图片有自己的排序偏好），**点卡片名称弹出菜单**，可「重命名」（含扩展名修改的二次确认与 ` (2)` 去重）、「属性」、「打开方式」、「分享」。**长按卡片进入多选**，选择边框、「已选择」角标与库内一致，底部栏就是库内那一条（`SelectionBottomBar`，动作同为「添加到 / 删除 / 压缩」）：「添加到」打开库内同一套文件夹选择页，可以把选中的图片**复制或移动进库**（复制＝库里多一份、原图留在设备上；移动＝入库后删除原图），「压缩」把它们打包成一个 `.zip` 存进库里任意目录（默认库根目录），「删除」带确认弹窗，会把这些图片从设备上真正删除。该页不提供「新建」，也没有「复制到剪切板」——图片不是文字，无需取字。全屏看图、属性页与多选都按返回键逐层关闭（先退出多选 / 属性 / 全屏，全部关掉后才回到「本地」标签），不会一次退回上一页。扫描与查看只读文件系统，不需要额外权限；`.thumbnails` 等隐藏目录与 `Android/data`、`Android/obb` 会被跳过。
- **多选**：底部栏提供「添加到 / 删除 / 压缩」，返回键退出多选，删除带确认弹窗；三项对文件与文件夹同样生效（文件夹递归）。「本地图片」页用的是同一个 `SelectionBottomBar`，动作一致，只因删除对象不同而换了确认文案。
- **添加到文件夹**：浏览式选择——点击文件夹进入其内部、「上一层」返回上级、「到根目录」cd 到根目录，底部「确认」后弹出「复制 / 移动 / 取消」。库内条目与「本地图片」的多选共用这一页（`AddToFolderPicker`），「复制 / 移动」的实际含义由调用方决定。
- **分享**：文件/文件夹可分享；文件夹分享会先弹出「压缩并分享」进度对话框，后台打包为 `.zip` 后自动打开系统分享面板。FileProvider 配置覆盖整盘共享存储（`file_paths.xml` 的 `external-path` 指向 `.`），库外图片同样能生成 content URI。
- **属性页**：文件展示名称、文件类型、文件大小、创建时间、修改时间与文件路径；文件夹展示名称、项目数量、文件夹大小、创建时间与文件夹路径；「本地图片」页的图片与虚拟文件夹也有属性页，同样是这些项目，但虚拟文件夹**不含路径行**（它并不对应磁盘上的目录），其创建时间取其中最早一张图片的时间。

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

「本地图片」扫描的是该目录之外的共享存储（即 `/storage/emulated/0/` 全盘减去 `MonoGallery/`）；库目录本身以 `library.storageRoot` 形式传入，不在此处另写一份路径。该页「刷新」会重新走盘；卡片菜单中的「重命名」会原位改名该图片（不搬动所在目录），其余操作不写入文件系统。虚拟文件夹的「属性」页展示名称 / 项目数量 / 文件夹大小 / 创建时间，**故意没有路径行**（它并不对应磁盘上的某个目录），且它自身没有创建时间，用其中最早一张图片的时间代替。多选删除会直接调用 `File.delete()`（绕过库，不经回收站），删完重新扫描。多选栏的「添加到」则反向写入库：`MonoLibrary.importExternalFiles` 把选中的图片复制进目标库目录（`MonoStorage.importExternalFiles`，逐张按 `uniqueName` 去重、复制完再 `refresh()`），选择「移动」时复制成功后删掉原图。

压缩的**源目录与保存位置是两个参数**：条目一律从选中项所在目录 `sourceFolderId` 读取，`.zip` 落在 `destinationFolderId`（默认仍是选中项所在目录，可在压缩界面用「保存位置」改到库里任意目录，含根目录）。两种目录都以 `List<String>` 路径片段传给 `MonoStorage.compressItemsToZip(sourcePath, destinationPath, …)`；目标目录取的是已存在的库目录，`MonoStorage.dir` 不会 `mkdirs`。同一张表单也服务于「本地图片」：那里没有库条目，条目改成设备上的图片文件（`MonoStorage.compressFilesToZip` 直接按绝对路径读取并写进 `.zip`），保存位置默认库根目录。

## 项目结构

```
app/src/main/java/com/sichuan/monogallery/
├── MainActivity.kt        # 应用入口，NavHost 路由装配 + 存储权限
├── Model.kt               # 数据模型：FileType / MonoFile / Folder / SortMode
├── SortPreference.kt      # 持久化的界面偏好（SharedPreferences，按列表分别记住所选排序方式）
├── MonoStorage.kt         # 本地磁盘读写，目录镜像界面层级
├── MonoLibrary.kt         # 内存仓库，改动即时写回磁盘
├── MonoSharing.kt         # 系统分享（文件 / 文件夹 zip）与分享进度弹窗
├── MonoComponents.kt      # 通用 UI 组件：页面骨架 / 命名表单 / 确认弹窗 / 双列网格（滚动位置可外部持有）
├── MonoCards.kt           # 卡片（含图片缩略图）与各类弹窗
├── MonoItemGrid.kt        # 双列卡片网格（卡片区域）
├── FileSelection.kt       # 多选状态 + 底部操作栏
├── AddMenu.kt             # 「+」新建 / 导入菜单
├── MoreMenu.kt            # 「⋮」菜单（刷新 / 按名称、类型或日期排序）
├── HomeScreen.kt          # 首页（根目录）
├── FolderContentScreen.kt # 文件夹内容页
├── HomeBottomBar.kt       # 首页底部导航栏（Mono / 本地 / 工具 / 设置）
├── LocalScreen.kt         # 本地标签页（「本地图片」卡片，顶栏 ⋮ 菜单）
├── ToolsScreen.kt         # 工具标签页（空白占位）
├── SettingsScreen.kt      # 设置标签页（空白占位）
├── LocalImages.kt         # 库外图片的扫描 / 排序 / 重命名（本地图片）
├── LocalImagesScreen.kt   # 本地图片页（扁平网格 + 多选 + 全屏看图 + 属性）
├── FolderBrowser.kt       # 库内文件夹浏览网格（「添加到」选择页与压缩「保存位置」共用）
├── AddToFolderPicker.kt   # 「添加到」文件夹选择页（库内条目与本地图片共用；确认后选复制 / 移动）
├── FolderPickerScreen.kt  # 库内条目的「添加到文件夹」入口（把选中的 id 交给 AddToFolderPicker）
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
├── CompressScreen.kt      # 压缩界面（库内条目与「本地图片」共用表单：输入名称 + 选择保存位置）
├── PropertiesScreen.kt    # 文件 / 文件夹属性页
└── MonoColors.kt          # 主题配色
```

### 复用约定

新增页面或加载器时优先复用下列单一实现，避免再次出现平行实现：

| 场景 | 复用点 |
| --- | --- |
| 带返回键与标题的整页骨架 | `MonoComponents.kt` 的 `MonoScaffold` |
| 只输入一个名称的表单页 | `MonoComponents.kt` 的 `NameFormScreen`（表单里还要放别的控件时用 `extraContent`） |
| 输入名称 / 确认操作的弹窗 | `MonoComponents.kt` 的 `NameInputDialog`、`ConfirmDialog` |
| 双列卡片网格 | `MonoComponents.kt` 的 `MonoGrid`（滚动位置需要跨页面切换保持时，把 `LazyGridState` 从调用方提升后传入 `state`） |
| 顶栏「⋮」菜单（刷新 / 名称、类型、日期排序） | `MoreMenu.kt` 的 `MoreMenu`；排序偏好由调用方传入 `SortPreference`（库与本地图片各用一份，见 `LOCAL_IMAGES_SORT_KEY`） |
| 属性页 | `PropertiesScreen.kt`：库内用 `FilePropertiesScreen` / `FolderPropertiesScreen`，库外用 `LocalImagePropertiesScreen` / `LocalImagesFolderPropertiesScreen`；行与 `formatBytes` / `formatTimestamp` 共用 |
| 库外图片的重命名 | `LocalImages.kt` 的 `renameLocalImage`（与 `MonoLibrary.renameFile` 一致地做 sanitize、扩展名小写、`uniqueName` 去重） |
| 卡片缩略图（图片 / 视频帧 / PDF 封面） | `BitmapSupport.kt` 的 `rememberBitmapResult`、`BitmapLruCache`、`fitWithin`；卡片侧统一走 `MonoCards.kt` 的 `ThumbnailBox` |
| 名称去重（追加 ` (2)`、` (3)`…） | `MonoStorage.kt` 的顶层 `uniqueName` |
| 多选 id 拆成文件 / 文件夹 | `MonoLibrary.partitionIds` |
| 全屏预览（视频 / 图片 / PDF） | `FullscreenPreviewScreen.kt` |
| 库外图片的扫描 | `LocalImages.kt` 的 `scanLocalImages`、`rememberLocalImages`（排除目录由调用方以参数传入，勿重复硬编码库目录名） |
| 库外图片的卡片 | `MonoCards.kt` 的 `LocalImageCard`（重命名 / 属性 / 打开方式 / 分享菜单，`selected` + `isSelecting` 复用同一张卡片的选择态）；无库条目的文件夹用 `PickerFolderCard(name, countText, …)` 重载，需要「属性」菜单时传 `onProperties` |
| 库外图片的多选状态 | `LocalImages.kt` 的 `LocalImageSelectionState`（按绝对路径选中；库内是 `FileSelection.kt` 的 `FileSelectionState`，按条目 id） |
| 浏览库内文件夹并选定目标目录 | `FolderBrowser.kt` 的 `FolderBrowser`（「添加到」选择页与压缩「保存位置」共用；放进弹窗时必须给它 `weight(1f)` 限高，否则懒网格高度无界会崩） |
| 「添加到」的整页选择器（库内条目与本地图片共用） | `AddToFolderPicker.kt` 的 `AddToFolderPicker`（`onConfirm(folderId, move)` 交回调用方，由它决定复制 / 移动各自的含义） |
| 多选底部操作栏 | `FileSelection.kt` 的 `SelectionBottomBar`（库内与「本地图片」共用；删除文案用 `deleteTitle` / `deleteMessage` 覆盖） |
| 压缩表单（名称 + 保存位置） | `CompressScreen.kt` 的 `CompressForm`（库内走 `CompressScreen`、库外图片走 `LocalImagesCompressScreen`，两者只差条目来源） |
| 把库外文件收进库 | `MonoLibrary.importExternalFiles`（库外用 `MonoStorage.importExternalFiles` 复制 + `uniqueName` 去重，`move = true` 时删原图；库外用 `MonoStorage.compressFilesToZip` 直接打包） |

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
2. **排序**：点右上角 `⋮`，可按名称（小→大）、类型（按扩展名）或日期（近→远）排列当前文件夹与文件，也可在此刷新列表。所选排序方式会被持久化，切换文件夹或重启应用后不会退回默认排序；「本地图片」记的是另一份偏好，两处互不影响。
3. **重命名文件**：点击文件卡片上的名字区域；修改扩展名会弹出确认。
4. **多选**：长按任意文件或文件夹卡片，底部出现操作栏，可批量添加到文件夹、删除或压缩；在「本地图片」页长按则是多选图片，底部栏同样是「添加到 / 删除 / 压缩」——「添加到」把选中的图片复制或移动进库，「压缩」把它们打包成 `.zip` 存进库里，按返回键退出多选。
5. **压缩**：多选后点底部栏「压缩」，进入压缩界面，输入压缩名称（默认第一个卡片名），点「保存位置」可把压缩包存到库里任意目录（默认是选中项所在目录），确认后把选中的文件与文件夹压缩为一个 `.zip`。
6. **底部导航**：首页底部点「本地 / 工具 / 设置」可切换到对应页面（工具 / 设置目前为空白页，等待填充内容），点「Mono」回到首页。
7. **本地图片**：切到「本地」标签，卡片上显示图片数量（首次进入显示「扫描中…」，扫描完成后刷新为实际数量），点击卡片进入「本地图片」页查看设备上所有不属于 MonoGallery 的图片。该页顶栏 `⋮` 可刷新（重新扫描）与切换排序；点卡片名称弹出菜单，可重命名 / 查看属性 / 用其他应用打开 / 分享；点卡片预览区全屏看图，退出后网格停在原来的位置；长按卡片可多选（添加到 / 删除 / 压缩），按返回键时先退出全屏、属性页或多选，都关掉后才返回上一页。「本地」标签页上的 `⋮` 菜单与卡片菜单的「属性」同样可用（该属性页没有路径行）。
