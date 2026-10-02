package com.sichuan.monogallery

import android.content.Context
import java.io.File

private const val STORAGE_DIR = "MonoGallery"

/** 把名称转成安全的文件名/目录名。 */
fun sanitizeName(name: String): String {
    val cleaned = name
        .replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001f]"), "_")
        .trim()
        .trimEnd('.', ' ')
    return cleaned.ifBlank { "未命名" }
}

/**
 * 本地存储：磁盘目录结构镜像界面层级。
 * 文件夹 = 目录，文件 = 文件（后缀由 [FileType] 决定）。
 * 保存在应用专属外部目录 Android/data/<package>/files 下。
 */
class MonoStorage(context: Context) {
    private val root: File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, STORAGE_DIR).apply { mkdirs() }

    private fun folderDir(folderName: String?): File =
        folderName?.let { File(root, sanitizeName(it)) } ?: root

    private fun filePath(folderName: String?, name: String, type: FileType): File =
        File(folderDir(folderName), "${sanitizeName(name)}.${type.extension}")

    fun load(): Pair<List<Folder>, List<MonoFile>> {
        val folders = mutableListOf<Folder>()
        val files = mutableListOf<MonoFile>()
        var nextId = 1L

        root.listFiles()?.sortedBy { it.name }?.forEach { entry ->
            when {
                entry.isDirectory -> {
                    val folder = Folder(id = nextId++, name = entry.name)
                    folders.add(folder)
                    entry.listFiles()?.sortedBy { it.name }?.forEach { child ->
                        if (child.isFile) {
                            files.add(
                                MonoFile(
                                    id = nextId++,
                                    name = child.nameWithoutExtension,
                                    type = FileType.fromExtension(child.extension),
                                    content = readTextSafe(child),
                                    folderId = folder.id,
                                )
                            )
                        }
                    }
                }

                entry.isFile -> {
                    files.add(
                        MonoFile(
                            id = nextId++,
                            name = entry.nameWithoutExtension,
                            type = FileType.fromExtension(entry.extension),
                            content = readTextSafe(entry),
                        )
                    )
                }
            }
        }
        return folders to files
    }

    fun createFolderDir(name: String) {
        File(root, sanitizeName(name)).mkdirs()
    }

    fun createFile(folderName: String?, name: String, type: FileType, content: String) {
        filePath(folderName, name, type).apply {
            parentFile?.mkdirs()
            writeText(content)
        }
    }

    fun renameFolder(oldName: String, newName: String) {
        File(root, sanitizeName(oldName)).renameTo(File(root, sanitizeName(newName)))
    }

    fun renameFile(folderName: String?, oldName: String, newName: String, type: FileType) {
        filePath(folderName, oldName, type).renameTo(filePath(folderName, newName, type))
    }

    fun moveFile(oldFolderName: String?, oldName: String, newFolderName: String?, newName: String, type: FileType) {
        val src = filePath(oldFolderName, oldName, type)
        val dst = filePath(newFolderName, newName, type)
        dst.parentFile?.mkdirs()
        src.renameTo(dst)
    }

    fun deleteFolder(name: String) {
        File(root, sanitizeName(name)).deleteRecursively()
    }

    fun deleteFile(folderName: String?, name: String, type: FileType) {
        filePath(folderName, name, type).delete()
    }

    fun writeFile(folderName: String?, name: String, type: FileType, content: String) {
        filePath(folderName, name, type).apply {
            parentFile?.mkdirs()
            writeText(content)
        }
    }

    private fun readTextSafe(file: File): String = try {
        file.readText()
    } catch (_: Exception) {
        ""
    }
}
