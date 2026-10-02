# MonoGallery

一款用于**收集文字**的 Android 应用。以卡片的形式组织文件夹与文件，支持多选、复制/移动、重命名、压缩等操作，界面层级与磁盘目录一一对应。

## 功能特性

- **文件夹与文件卡片**：统一方形卡片（2 列网格），文件夹不可嵌套，文件不分类型统一展示。
- **新建**：顶部 `+` 展开「新建文件夹 / 新建文件」。
- **文件夹**：单击打开；长按弹出菜单（重命名 / 压缩为同名 `.zip` / 删除）。
- **文件卡片**：
  - 名字与扩展名放在一起显示，名字与正文预览之间有一条分隔线；
  - 点击**名字区域**重命名，点击**正文区域**打开文件；
  - 长按进入多选。
- **重命名**：支持修改扩展名；扩展名变更时二次确认；重名时在名称后自动追加 ` (2)`、` (3)`…（Windows 风格）。
- **多选**：底部栏提供「添加到 / 删除 / 复制到剪切板」，返回键退出多选，删除带确认弹窗。
- **添加到文件夹**：目标选择页含 `+ 新建文件夹`、`到根目录` 按钮，选中后弹出「复制 / 移动 / 取消」。
- **文件编辑**：文字文件可直接编辑并自动保存（400ms 防抖），其他类型显示占位提示。

## 技术栈

- Kotlin + Jetpack Compose（Material 3）
- Navigation Compose（字符串路由）
- 内存仓库 + 写穿透持久化（`MonoStorage`）

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

应用文件保存在应用专属外部目录：

```
Android/data/com.sichuan.monogallery/files/MonoGallery/
```

磁盘目录结构镜像界面层级：

- 文件夹 = 目录
- 文件 = 文件（文件名 = 名称 + 扩展名，如 `随笔.txt`）

> 卸载应用时该目录会一并被删除；仅文字类（`.txt`）文件会读取内容用于预览。

## 项目结构

```
app/src/main/java/com/sichuan/monogallery/
├── MainActivity.kt        # 应用入口，NavHost 路由装配
├── Model.kt               # 数据模型：FileType / MonoFile / Folder
├── MonoStorage.kt         # 本地磁盘读写，目录镜像界面层级
├── MonoLibrary.kt         # 内存仓库，改动即时写回磁盘
├── MonoCards.kt           # 卡片与各类弹窗
├── FileSelection.kt       # 多选状态 + 底部操作栏
├── HomeScreen.kt          # 首页
├── FolderContentScreen.kt # 文件夹内容页
├── FolderPickerScreen.kt  # 「添加到文件夹」目标选择页
├── FileViewScreen.kt      # 文件查看 / 编辑页
├── NewFolderScreen.kt     # 新建文件夹页
└── MonoColors.kt          # 主题配色
```

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

1. 首页点击 `+` 新建文件夹或文件；点击文件夹卡片进入，点击文件卡片打开。
2. **重命名文件**：点击文件卡片上的名字区域；修改扩展名会弹出确认。
3. **多选**：长按任意文件卡片，底部出现操作栏，可批量添加到文件夹、删除或复制到剪切板。
4. **压缩**：长按文件夹，选择「压缩」，会生成同名 `.zip` 到同一目录。
