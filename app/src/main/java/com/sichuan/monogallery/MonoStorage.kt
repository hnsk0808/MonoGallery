package com.sichuan.monogallery

import android.content.Context
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
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
 * 文件夹 = 目录（可嵌套），文件 = 文件（文件名 = 名称 + 扩展名）。
 * 文件夹位置用相对根目录的路径段列表 [path] 表示（空列表 = 根目录）。
 * 保存在应用专属外部目录 Android/data/<package>/files 下。
 */
class MonoStorage(context: Context) {
    private val root: File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, STORAGE_DIR).apply { mkdirs() }

    private val cacheDir: File = context.cacheDir

    private fun dir(path: List<String>): File =
        path.fold(root) { file, segment -> File(file, sanitizeName(segment)) }

    private fun fileIn(path: List<String>, name: String, extension: String): File {
        val fileName = if (extension.isBlank()) sanitizeName(name) else "${sanitizeName(name)}.$extension"
        return File(dir(path), fileName)
    }

    /** 文件在磁盘上的位置（用于分享等）。 */
    fun fileFor(path: List<String>, name: String, extension: String): File = fileIn(path, name, extension)

    fun load(): Pair<List<Folder>, List<MonoFile>> {
        val folders = mutableListOf<Folder>()
        val files = mutableListOf<MonoFile>()
        var nextId = 1L

        fun walk(directory: File, parentId: Long?) {
            directory.listFiles()?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name })?.forEach { entry ->
                when {
                    entry.isDirectory -> {
                        val folder = Folder(id = nextId++, name = entry.name, parentId = parentId)
                        folders.add(folder)
                        walk(entry, folder.id)
                    }

                    entry.isFile -> files.add(toFile(nextId++, entry, parentId))
                }
            }
        }

        walk(root, null)
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

    fun createFolderDir(path: List<String>, name: String) {
        File(dir(path), sanitizeName(name)).mkdirs()
    }

    fun createFile(path: List<String>, name: String, extension: String, content: String) {
        fileIn(path, name, extension).apply {
            parentFile?.mkdirs()
            writeText(content)
        }
    }

    fun copyFile(oldPath: List<String>, oldName: String, newPath: List<String>, newName: String, extension: String) {
        val src = fileIn(oldPath, oldName, extension)
        val dst = fileIn(newPath, newName, extension)
        dst.parentFile?.mkdirs()
        src.copyTo(dst, overwrite = true)
    }

    fun copyFolder(oldPath: List<String>, newPath: List<String>) {
        dir(oldPath).copyRecursively(dir(newPath), overwrite = true)
    }

    fun renameFolder(parentPath: List<String>, oldName: String, newName: String) {
        File(dir(parentPath), sanitizeName(oldName)).renameTo(File(dir(parentPath), sanitizeName(newName)))
    }

    fun renameFile(path: List<String>, oldName: String, oldExtension: String, newName: String, newExtension: String) {
        fileIn(path, oldName, oldExtension).renameTo(fileIn(path, newName, newExtension))
    }

    fun moveFile(oldPath: List<String>, oldName: String, newPath: List<String>, newName: String, extension: String) {
        val src = fileIn(oldPath, oldName, extension)
        val dst = fileIn(newPath, newName, extension)
        dst.parentFile?.mkdirs()
        src.renameTo(dst)
    }

    fun moveFolder(oldPath: List<String>, newPath: List<String>) {
        val dst = dir(newPath)
        dst.parentFile?.mkdirs()
        dir(oldPath).renameTo(dst)
    }

    fun deleteFolder(path: List<String>) {
        dir(path).deleteRecursively()
    }

    fun deleteFile(path: List<String>, name: String, extension: String) {
        fileIn(path, name, extension).delete()
    }

    fun writeFile(path: List<String>, name: String, extension: String, content: String) {
        fileIn(path, name, extension).apply {
            parentFile?.mkdirs()
            writeText(content)
        }
    }

    /** 把选中的文件与文件夹压缩为单个 `.zip`（落在 [parentPath] 目录），返回生成的压缩文件。 */
    fun compressItemsToZip(
        parentPath: List<String>,
        zipName: String,
        fileItems: List<Pair<String, String>>,
        folderItems: List<String>,
    ): File {
        val dst = uniqueZipFile(dir(parentPath), sanitizeName(zipName))
        ZipOutputStream(BufferedOutputStream(FileOutputStream(dst))).use { zip ->
            folderItems.forEach { folderName ->
                val src = File(dir(parentPath), sanitizeName(folderName))
                src.walkTopDown().filter { it.isFile }.forEach { file ->
                    zip.putNextEntry(ZipEntry("${sanitizeName(folderName)}/${file.relativeTo(src).invariantSeparatorsPath}"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            fileItems.forEach { (name, extension) ->
                val entryName = if (extension.isBlank()) sanitizeName(name) else "${sanitizeName(name)}.$extension"
                zip.putNextEntry(ZipEntry(entryName))
                fileIn(parentPath, name, extension).inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return dst
    }

    /** 把文件夹（含子内容）压缩到缓存目录用于分享，返回生成的 zip 文件。 */
    fun zipFolderToCache(path: List<String>): File {
        val folderName = path.last()
        val src = dir(path)
        val dst = File(cacheDir, "${sanitizeName(folderName)}.zip").apply { delete() }
        ZipOutputStream(BufferedOutputStream(FileOutputStream(dst))).use { zip ->
            src.walkTopDown().filter { it.isFile }.forEach { file ->
                zip.putNextEntry(ZipEntry("${sanitizeName(folderName)}/${file.relativeTo(src).invariantSeparatorsPath}"))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return dst
    }

    /** 生成不与现有文件冲突的 `.zip` 路径。 */
    private fun uniqueZipFile(directory: File, base: String): File {
        var file = File(directory, "$base.zip")
        var i = 2
        while (file.exists()) {
            file = File(directory, "$base ($i).zip")
            i++
        }
        return file
    }

    private fun readTextSafe(file: File): String = try {
        file.readText()
    } catch (_: Exception) {
        ""
    }

    /** 文件元数据：字节大小与创建 / 修改时间。 */
    fun fileInfo(path: List<String>, name: String, extension: String): FileInfo {
        val f = fileIn(path, name, extension)
        return FileInfo(
            size = f.length(),
            createdMillis = creationTimeMillis(f),
            modifiedMillis = f.lastModified(),
        )
    }

    /** 文件夹元数据：递归字节大小与创建时间。 */
    fun folderInfo(path: List<String>): FolderInfo {
        val d = dir(path)
        return FolderInfo(
            size = d.walkTopDown().filter { it.isFile }.sumOf { it.length() },
            createdMillis = creationTimeMillis(d),
        )
    }
}

/** 尽量读取文件系统「创建时间」，取不到时回退为最后修改时间。 */
private fun creationTimeMillis(file: File): Long = try {
    Files.readAttributes(file.toPath(), BasicFileAttributes::class.java).creationTime().toMillis()
} catch (_: Exception) {
    file.lastModified()
}
