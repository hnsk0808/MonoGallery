package com.sichuan.monogallery

import android.content.Context
import android.os.Environment
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val STORAGE_DIR = "MonoGallery"

/** Sanitizes a name into a safe file/folder name. */
fun sanitizeName(name: String): String {
    val cleaned = name
        .replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001f]"), "_")
        .trim()
        .trimEnd('.', ' ')
    return cleaned.ifBlank { "未命名" }
}

/** Splits a full file name into (name, extension); the extension is empty when there is none. */
fun splitFullName(fullName: String): Pair<String, String> {
    val dot = fullName.lastIndexOf('.')
    return if (dot <= 0) fullName to "" else fullName.substring(0, dot) to fullName.substring(dot + 1)
}

/**
 * Local storage: the on-disk directory structure mirrors the UI hierarchy.
 * A folder is a (nested) directory, and a file is a file whose name is name + extension.
 * A folder's location is represented by [path], the list of path segments relative to the
 * root directory (an empty list = the root directory). Stored under the shared external
 * storage root at /storage/emulated/0.
 */
class MonoStorage(context: Context) {
    private val root: File =
        File(Environment.getExternalStorageDirectory(), STORAGE_DIR).apply { mkdirs() }

    private val cacheDir: File = context.cacheDir

    private fun dir(path: List<String>): File =
        path.fold(root) { file, segment -> File(file, segment) }

    private fun fileIn(path: List<String>, name: String, extension: String): File {
        val fileName = if (extension.isBlank()) name else "$name.$extension"
        return File(dir(path), fileName)
    }

    /** On-disk location of a file (used for sharing and so on). */
    fun fileFor(path: List<String>, name: String, extension: String): File = fileIn(path, name, extension)

    /** On-disk location of a folder (used by the properties screen and so on). */
    fun folderFor(path: List<String>): File = dir(path)

    /**
     * Loads the directory tree from disk. When [existingFolderIds] / [existingFileIds]
     * (relative path from the root directory -> id) are supplied, existing items retain
     * their original ids so that after a refresh the currently shown screen does not
     * resolve to a different item because of id reassignment; unmatched items get a fresh id.
     */
    fun load(
        existingFolderIds: Map<String, Long> = emptyMap(),
        existingFileIds: Map<String, Long> = emptyMap(),
    ): Pair<List<Folder>, List<MonoFile>> {
        val folders = mutableListOf<Folder>()
        val files = mutableListOf<MonoFile>()
        val usedIds = mutableSetOf<Long>()
        // Fresh ids start after all retained ids so they never collide with a retained id
        var nextId = maxOf(
            existingFolderIds.values.maxOrNull() ?: 0L,
            existingFileIds.values.maxOrNull() ?: 0L,
        ) + 1

        fun freshId(): Long {
            val id = nextId++
            usedIds.add(id)
            return id
        }

        fun reuseOrFresh(relativePath: String, existing: Map<String, Long>): Long {
            val old = existing[relativePath]
            if (old != null && old !in usedIds) {
                usedIds.add(old)
                return old
            }
            return freshId()
        }

        fun walk(directory: File, parentId: Long?, pathSegments: List<String>) {
            directory.listFiles()?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name })?.forEach { entry ->
                val relativePath = (pathSegments + entry.name).joinToString("/")
                when {
                    entry.isDirectory -> {
                        val folder = Folder(id = reuseOrFresh(relativePath, existingFolderIds), name = entry.name, parentId = parentId)
                        folders.add(folder)
                        walk(entry, folder.id, pathSegments + entry.name)
                    }

                    entry.isFile -> files.add(toFile(reuseOrFresh(relativePath, existingFileIds), entry, parentId))
                }
            }
        }

        walk(root, null, emptyList())
        return folders to files
    }

    /** Builds a [MonoFile] from an on-disk [file], reading text content for text files. */
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

    /** Creates the folder named [name] inside the folder at [path]. */
    fun createFolderDir(path: List<String>, name: String) {
        File(dir(path), sanitizeName(name)).mkdirs()
    }

    /** Creates a file with the given [name], [extension] and [content] inside the folder at [path]. */
    fun createFile(path: List<String>, name: String, extension: String, content: String) {
        fileIn(path, name, extension).apply {
            parentFile?.mkdirs()
            writeText(content)
        }
    }

    /** Copies a file from [oldPath]/[oldName] to [newPath]/[newName], keeping [extension]. */
    fun copyFile(oldPath: List<String>, oldName: String, newPath: List<String>, newName: String, extension: String) {
        val src = fileIn(oldPath, oldName, extension)
        val dst = fileIn(newPath, newName, extension)
        dst.parentFile?.mkdirs()
        src.copyTo(dst, overwrite = true)
    }

    /** Recursively copies the folder at [oldPath] into [newPath]. */
    fun copyFolder(oldPath: List<String>, newPath: List<String>) {
        dir(oldPath).copyRecursively(dir(newPath), overwrite = true)
    }

    /** Renames the folder [oldName] to [newName] within the parent folder at [parentPath]. */
    fun renameFolder(parentPath: List<String>, oldName: String, newName: String) {
        File(dir(parentPath), oldName).renameTo(File(dir(parentPath), newName))
    }

    /** Renames a file from [oldName]/[oldExtension] to [newName]/[newExtension] within [path]. */
    fun renameFile(path: List<String>, oldName: String, oldExtension: String, newName: String, newExtension: String) {
        fileIn(path, oldName, oldExtension).renameTo(fileIn(path, newName, newExtension))
    }

    /** Moves a file from [oldPath]/[oldName] to [newPath]/[newName], keeping [extension]. */
    fun moveFile(oldPath: List<String>, oldName: String, newPath: List<String>, newName: String, extension: String) {
        val src = fileIn(oldPath, oldName, extension)
        val dst = fileIn(newPath, newName, extension)
        dst.parentFile?.mkdirs()
        src.renameTo(dst)
    }

    /** Moves the folder at [oldPath] to [newPath]. */
    fun moveFolder(oldPath: List<String>, newPath: List<String>) {
        val dst = dir(newPath)
        dst.parentFile?.mkdirs()
        dir(oldPath).renameTo(dst)
    }

    /** Deletes the folder at [path] recursively, including all its contents. */
    fun deleteFolder(path: List<String>) {
        dir(path).deleteRecursively()
    }

    /** Deletes the file at [path]/[name] with the given [extension]. */
    fun deleteFile(path: List<String>, name: String, extension: String) {
        fileIn(path, name, extension).delete()
    }

    /** Overwrites the file at [path]/[name] with the given [extension] and [content]. */
    fun writeFile(path: List<String>, name: String, extension: String, content: String) {
        fileIn(path, name, extension).apply {
            parentFile?.mkdirs()
            writeText(content)
        }
    }

    /** Compresses the selected files and folders into a single `.zip` (placed in [parentPath]) and returns the resulting archive. */
    fun compressItemsToZip(
        parentPath: List<String>,
        zipName: String,
        fileItems: List<Pair<String, String>>,
        folderItems: List<String>,
    ): File {
        val dst = uniqueZipFile(dir(parentPath), sanitizeName(zipName))
        ZipOutputStream(BufferedOutputStream(FileOutputStream(dst))).use { zip ->
            folderItems.forEach { folderName ->
                val src = File(dir(parentPath), folderName)
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

    /** Zips a folder (including its contents) into the cache directory for sharing and returns the resulting zip file. */
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

    /** Builds a `.zip` path that does not clash with existing files. */
    private fun uniqueZipFile(directory: File, base: String): File {
        var file = File(directory, "$base.zip")
        var i = 2
        while (file.exists()) {
            file = File(directory, "$base ($i).zip")
            i++
        }
        return file
    }

    /** Reads a text file as a string, returning an empty string when reading fails. */
    private fun readTextSafe(file: File): String = try {
        file.readText()
    } catch (_: Exception) {
        ""
    }

    /** File metadata: size in bytes and creation and modification times. */
    fun fileInfo(path: List<String>, name: String, extension: String): FileInfo {
        val f = fileIn(path, name, extension)
        return FileInfo(
            size = f.length(),
            createdMillis = creationTimeMillis(f),
            modifiedMillis = f.lastModified(),
        )
    }

    /** Folder metadata: recursive size in bytes and creation time. */
    fun folderInfo(path: List<String>): FolderInfo {
        val d = dir(path)
        return FolderInfo(
            size = d.walkTopDown().filter { it.isFile }.sumOf { it.length() },
            createdMillis = creationTimeMillis(d),
        )
    }
}

/** Reads the file system creation time when available, falling back to the last-modified time. */
private fun creationTimeMillis(file: File): Long = try {
    Files.readAttributes(file.toPath(), BasicFileAttributes::class.java).creationTime().toMillis()
} catch (_: Exception) {
    file.lastModified()
}
