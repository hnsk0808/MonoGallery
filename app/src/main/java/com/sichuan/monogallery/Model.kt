package com.sichuan.monogallery

/**
 * 文件类型：由扩展名推导，用于卡片渲染与打开逻辑的分发。
 * 新增类型时，在 [fromExtension] 中增加映射，并在渲染 / 打开处按 [MonoFile.type] 处理。
 */
enum class FileType(val label: String) {
    TEXT("文字"),
    OTHER("文件");

    companion object {
        fun fromExtension(extension: String): FileType = when (extension.lowercase()) {
            "txt" -> TEXT
            else -> OTHER
        }
    }
}

/**
 * 文件：[name] 不含扩展名，[extension] 为实际扩展名（如 "txt"、"zip"）；
 * [type] 由扩展名推导，[folderId] 为 null 表示位于根目录。
 */
data class MonoFile(
    val id: Long,
    val name: String,
    val extension: String,
    val content: String = "",
    val folderId: Long? = null,
) {
    val type: FileType get() = FileType.fromExtension(extension)

    /** 完整文件名（含扩展名）。 */
    val fullName: String get() = if (extension.isBlank()) name else "$name.$extension"
}

/** 文件夹：只包含文件，不能嵌套文件夹。 */
data class Folder(
    val id: Long,
    val name: String,
)
