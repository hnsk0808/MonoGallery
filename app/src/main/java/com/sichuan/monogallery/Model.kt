package com.sichuan.monogallery

/**
 * 文件类型。新增类型只需在此加一个枚举值，
 * 并在卡片渲染 / 打开逻辑里按 [MonoFile.type] 分发，不写死「文字」。
 */
enum class FileType(val label: String, val extension: String) {
    TEXT("文字", "txt");

    // 后续可在此扩展，例如 IMAGE("图片", "png")、AUDIO("音频", "m4a")
    companion object {
        fun fromExtension(ext: String): FileType =
            entries.firstOrNull { it.extension == ext.lowercase() } ?: TEXT
    }
}

/** 文件：不写死类型，通过 [type] 区分；[folderId] 为 null 表示位于根目录。 */
data class MonoFile(
    val id: Long,
    val name: String,
    val type: FileType = FileType.TEXT,
    val content: String = "",
    val folderId: Long? = null,
)

/** 文件夹：只包含文件，不能嵌套文件夹。 */
data class Folder(
    val id: Long,
    val name: String,
)
