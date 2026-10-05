package com.sichuan.monogallery

/**
 * File type, derived from the extension and used to dispatch card rendering and open logic.
 * To add a type, add a mapping in [fromExtension] and handle it by [MonoFile.type] at the
 * rendering and open sites.
 */
enum class FileType(val label: String) {
    TEXT("文字"),
    IMAGE("图片"),
    AUDIO("音频"),
    VIDEO("视频"),
    PDF("PDF"),
    OTHER("文件");

    companion object {
        fun fromExtension(extension: String): FileType = when (extension.lowercase()) {
            "txt" -> TEXT
            "png", "jpg", "jpeg", "gif", "webp", "bmp", "heic", "heif" -> IMAGE
            "mp3", "wav", "m4a", "aac", "ogg", "flac", "opus", "amr", "wma", "mid", "midi", "aiff" -> AUDIO
            "mp4", "mkv", "webm", "avi", "mov", "mpg", "mpeg", "m2v", "3gp", "3g2",
            "m4v", "flv", "f4v", "ts", "m2ts", "mts", "wmv", "ogv", "rmvb", "vob", "asf", "divx" -> VIDEO
            "pdf" -> PDF
            else -> OTHER
        }
    }
}

/**
 * A file: [name] excludes the extension, [extension] is the actual extension (such as
 * "txt" or "zip"), [type] is derived from the extension, and a null [folderId] means the
 * file is in the root directory.
 */
data class MonoFile(
    val id: Long,
    val name: String,
    val extension: String,
    val content: String = "",
    val folderId: Long? = null,
) {
    val type: FileType get() = FileType.fromExtension(extension)

    /** Full file name, including the extension. */
    val fullName: String get() = if (extension.isBlank()) name else "$name.$extension"
}

/** A folder: folders can be nested (a null [parentId] means the root directory) and can contain files and subfolders. */
data class Folder(
    val id: Long,
    val name: String,
    val parentId: Long? = null,
)

/** On-disk file metadata: size in bytes and creation and modification times (millisecond timestamps). */
data class FileInfo(
    val size: Long,
    val createdMillis: Long,
    val modifiedMillis: Long,
)

/** On-disk folder metadata: recursive size in bytes and creation time (millisecond timestamp). */
data class FolderInfo(
    val size: Long,
    val createdMillis: Long,
)

/** Sort order for folder and file lists. */
enum class SortMode {
    /** Sort by name from small to large (ascending, case-insensitive). */
    NAME,

    /** Sort by extension A-Z, then by file name A-Z within the same extension. */
    TYPE,

    /** Sort by date from near to far (the newest item first). */
    DATE,
}
