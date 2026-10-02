package com.sichuan.monogallery

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import java.io.File

/**
 * 数据仓库：内存态为主，改动即时写回本地磁盘。
 * 磁盘结构 = 界面结构：每个文件夹对应一个目录，每个文件对应一个文件。
 */
class MonoLibrary(context: Context) {
    private val storage = MonoStorage(context)
    val folders = mutableStateListOf<Folder>()
    val files = mutableStateListOf<MonoFile>()
    private var nextId = 1L

    init {
        val (loadedFolders, loadedFiles) = storage.load()
        folders.addAll(loadedFolders)
        files.addAll(loadedFiles)
        nextId = maxOf(
            folders.maxOfOrNull { it.id } ?: 0L,
            files.maxOfOrNull { it.id } ?: 0L,
        ) + 1
    }

    private fun newId(): Long = nextId++

    fun folder(id: Long): Folder? = folders.firstOrNull { it.id == id }
    fun file(id: Long): MonoFile? = files.firstOrNull { it.id == id }
    fun rootFiles(): List<MonoFile> = files.filter { it.folderId == null }
    fun filesIn(folderId: Long): List<MonoFile> = files.filter { it.folderId == folderId }
    fun itemCount(folderId: Long): Int = files.count { it.folderId == folderId }

    private fun folderName(folderId: Long?): String? = folderId?.let { id -> folder(id)?.name }

    fun createFolder(name: String): Folder {
        val finalName = uniqueName(sanitizeName(name), folders.map { it.name }.toSet())
        storage.createFolderDir(finalName)
        val folder = Folder(id = newId(), name = finalName)
        folders.add(folder)
        return folder
    }

    fun createFile(
        name: String,
        folderId: Long? = null,
        content: String = "",
        extension: String = "txt",
    ): MonoFile {
        val siblings = files.filter { it.folderId == folderId }.map { it.name }.toSet()
        val finalName = uniqueName(sanitizeName(name), siblings)
        storage.createFile(folderName(folderId), finalName, extension, content)
        val file = MonoFile(id = newId(), name = finalName, extension = extension, content = content, folderId = folderId)
        files.add(file)
        return file
    }

    fun renameFolder(id: Long, newName: String) {
        val index = folders.indexOfFirst { it.id == id }
        if (index < 0) return
        val old = folders[index]
        val finalName = if (newName == old.name) old.name
        else uniqueName(sanitizeName(newName), folders.filter { it.id != id }.map { it.name }.toSet())
        if (finalName == old.name) return
        storage.renameFolder(old.name, finalName)
        folders[index] = old.copy(name = finalName)
    }

    fun renameFile(id: Long, newFullName: String) {
        val index = files.indexOfFirst { it.id == id }
        if (index < 0) return
        val old = files[index]

        val (base, ext) = splitFullName(sanitizeName(newFullName.trim()))
        val newExtension = ext.lowercase()

        // 唯一化：同目录下若已存在相同完整文件名，则在名称后追加 (n)
        var finalName = base
        var i = 2
        while (files.any {
                it.folderId == old.folderId && it.id != id &&
                    it.name == finalName && it.extension.lowercase() == newExtension
            }
        ) {
            finalName = "$base ($i)"
            i++
        }

        if (finalName == old.name && newExtension == old.extension.lowercase()) return
        storage.renameFile(folderName(old.folderId), old.name, old.extension, finalName, newExtension)
        files[index] = old.copy(name = finalName, extension = newExtension)
    }

    fun deleteFolder(id: Long) {
        val folder = folder(id) ?: return
        storage.deleteFolder(folder.name)
        folders.removeAll { it.id == id }
        files.removeAll { it.folderId == id }
    }

    fun deleteFile(id: Long) {
        val file = file(id) ?: return
        storage.deleteFile(folderName(file.folderId), file.name, file.extension)
        files.removeAll { it.id == id }
    }

    fun updateFileContent(id: Long, content: String) {
        val index = files.indexOfFirst { it.id == id }
        if (index < 0) return
        val file = files[index]
        if (file.content == content) return
        storage.writeFile(folderName(file.folderId), file.name, file.extension, content)
        files[index] = file.copy(content = content)
    }

    fun deleteFiles(ids: Set<Long>) {
        ids.forEach { deleteFile(it) }
    }

    /** 把多个文件移动到目标文件夹（targetFolderId 为 null 表示根目录，磁盘同步改名/移动）。 */
    fun moveFilesToFolder(ids: Set<Long>, targetFolderId: Long?) {
        for (id in ids) {
            val file = file(id) ?: continue
            if (file.folderId == targetFolderId) continue
            val siblings = files.filter { it.folderId == targetFolderId && it.id != id }.map { it.name }.toSet()
            val finalName = uniqueName(file.name, siblings)
            storage.moveFile(folderName(file.folderId), file.name, folderName(targetFolderId), finalName, file.extension)
            val index = files.indexOfFirst { it.id == id }
            files[index] = file.copy(name = finalName, folderId = targetFolderId)
        }
    }

    /** 把多个文件复制到目标文件夹（targetFolderId 为 null 表示根目录，保留原件）。 */
    fun copyFilesToFolder(ids: Set<Long>, targetFolderId: Long?) {
        for (id in ids) {
            val file = file(id) ?: continue
            if (file.folderId == targetFolderId) continue
            val siblings = files.filter { it.folderId == targetFolderId }.map { it.name }.toSet()
            val finalName = uniqueName(file.name, siblings)
            storage.copyFile(folderName(file.folderId), file.name, folderName(targetFolderId), finalName, file.extension)
            files.add(file.copy(id = newId(), name = finalName, folderId = targetFolderId))
        }
    }

    /** 把文件夹压缩为同名 .zip，返回生成的压缩文件（folder 不存在时返回 null）。 */
    fun compressFolder(id: Long): File? {
        val folder = folder(id) ?: return null
        return storage.compressFolder(folder.name)
    }

    private fun uniqueName(base: String, existing: Set<String>): String {
        if (base !in existing) return base
        var i = 2
        while ("$base ($i)" in existing) i++
        return "$base ($i)"
    }
}
