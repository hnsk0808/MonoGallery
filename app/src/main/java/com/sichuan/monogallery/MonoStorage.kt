package com.sichuan.monogallery

import android.content.Context
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val STORAGE_DIR = "MonoGallery"

/** 把名称转成安全的文件名/目录名。 */
fun sanitizeName(name: String): String {
    val cleaned = name
        .replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001f]"), "_")
        .trim()
        .trimEnd('.', ' ')
    return cleaned.ifBlank { "未命名" }
}

/** 把完整文件名拆成 (名称, 扩展名)，无扩展名时扩展名为空。 */
fun splitFullName(fullName: String): Pair<String, String> {
    val dot = fullName.lastIndexOf('.')
    return if (dot <= 0) fullName to "" else fullName.substring(0, dot) to fullName.substring(dot + 1)
}

/**
 * 本地存储：磁盘目录结构镜像界面层级。
 * 文件夹 = 目录，文件 = 文件（文件名 = 名称 + 扩展名）。
 * 保存在应用专属外部目录 Android/data/<package>/files 下。
 */
class MonoStorage(context: Context) {
    private val root: File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, STORAGE_DIR).apply { mkdirs() }

    private fun folderDir(folderName: String?): File =
        folderName?.let { File(root, sanitizeName(it)) } ?: root

    private fun filePath(folderName: String?, name: String, extension: String): File {
        val fileName = if (extension.isBlank()) sanitizeName(name) else "${sanitizeName(name)}.$extension"
        return File(folderDir(folderName), fileName)
    }

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
                            files.add(toFile(nextId++, child, folder.id))
                        }
                    }
                }

                entry.isFile -> {
                    files.add(toFile(nextId++, entry, null))
                }
            }
        }
        return folders to files
    }

    private fun toFile(id: Long, file: File, folderId: Long?): MonoFile {
        val extension = file.extension
        val content = if (FileType.fromExtension(extension) == FileType.TEXT) readTextSafe(file) else ""
        return MonoFile(
            id = id,
            name = file.nameWithoutExtension,
            extension = extension,
            content = content,
            folderId = folderId,
        )
    }

    fun createFolderDir(name: String) {
        File(root, sanitizeName(name)).mkdirs()
    }

    fun createFile(folderName: String?, name: String, extension: String, content: String) {
        filePath(folderName, name, extension).apply {
            parentFile?.mkdirs()
            writeText(content)
        }
    }

    fun copyFile(oldFolderName: String?, oldName: String, newFolderName: String?, newName: String, extension: String) {
        val src = filePath(oldFolderName, oldName, extension)
        val dst = filePath(newFolderName, newName, extension)
        dst.parentFile?.mkdirs()
        src.copyTo(dst, overwrite = true)
    }

    fun renameFolder(oldName: String, newName: String) {
        File(root, sanitizeName(oldName)).renameTo(File(root, sanitizeName(newName)))
    }

    fun renameFile(folderName: String?, oldName: String, oldExtension: String, newName: String, newExtension: String) {
        filePath(folderName, oldName, oldExtension).renameTo(filePath(folderName, newName, newExtension))
    }

    fun moveFile(oldFolderName: String?, oldName: String, newFolderName: String?, newName: String, extension: String) {
        val src = filePath(oldFolderName, oldName, extension)
        val dst = filePath(newFolderName, newName, extension)
        dst.parentFile?.mkdirs()
        src.renameTo(dst)
    }

    fun deleteFolder(name: String) {
        File(root, sanitizeName(name)).deleteRecursively()
    }

    fun deleteFile(folderName: String?, name: String, extension: String) {
        filePath(folderName, name, extension).delete()
    }

    fun writeFile(folderName: String?, name: String, extension: String, content: String) {
        filePath(folderName, name, extension).apply {
            parentFile?.mkdirs()
            writeText(content)
        }
    }

    /** 把文件夹压缩为同名 .zip，返回生成的压缩文件。 */
    fun compressFolder(name: String): File {
        val src = File(root, sanitizeName(name))
        val dst = File(root, "${sanitizeName(name)}.zip")
        ZipOutputStream(BufferedOutputStream(FileOutputStream(dst))).use { zip ->
            src.walkTopDown().filter { it.isFile }.forEach { file ->
                zip.putNextEntry(ZipEntry(file.relativeTo(src).invariantSeparatorsPath))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return dst
    }

    private fun readTextSafe(file: File): String = try {
        file.readText()
    } catch (_: Exception) {
        ""
    }
}
