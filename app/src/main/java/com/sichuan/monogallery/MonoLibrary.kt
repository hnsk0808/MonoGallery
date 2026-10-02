package com.sichuan.monogallery

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import java.io.File

/**
 * 数据仓库：内存态为主，改动即时写回本地磁盘。
 * 磁盘结构 = 界面结构：每个文件夹对应一个目录（可嵌套），每个文件对应一个文件。
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

    fun rootFolders(): List<Folder> = folders.filter { it.parentId == null }
    fun subfoldersOf(parentId: Long?): List<Folder> = folders.filter { it.parentId == parentId }
    fun rootFiles(): List<MonoFile> = files.filter { it.folderId == null }
    fun filesIn(folderId: Long): List<MonoFile> = files.filter { it.folderId == folderId }

    /** 项目数量：文件数 + 子文件夹数（每个子文件夹算一项）。 */
    fun itemCount(folderId: Long): Int =
        files.count { it.folderId == folderId } + folders.count { it.parentId == folderId }

    /** 文件夹相对根目录的路径段（根目录为空列表）。 */
    private fun pathOf(folderId: Long?): List<String> {
        val segments = mutableListOf<String>()
        var current = folderId
        while (current != null) {
            val f = folder(current) ?: break
            segments.add(0, f.name)
            current = f.parentId
        }
        return segments
    }

    fun createFolder(name: String, parentId: Long? = null): Folder {
        val siblings = folders.filter { it.parentId == parentId }.map { it.name }.toSet()
        val finalName = uniqueName(sanitizeName(name), siblings)
        storage.createFolderDir(pathOf(parentId), finalName)
        val folder = Folder(id = newId(), name = finalName, parentId = parentId)
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
        storage.createFile(pathOf(folderId), finalName, extension, content)
        val file = MonoFile(id = newId(), name = finalName, extension = extension, content = content, folderId = folderId)
        files.add(file)
        return file
    }

    fun renameFolder(id: Long, newName: String) {
        val index = folders.indexOfFirst { it.id == id }
        if (index < 0) return
        val old = folders[index]
        val siblings = folders.filter { it.parentId == old.parentId && it.id != id }.map { it.name }.toSet()
        val finalName = if (newName == old.name) old.name else uniqueName(sanitizeName(newName), siblings)
        if (finalName == old.name) return
        storage.renameFolder(pathOf(old.parentId), old.name, finalName)
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
        storage.renameFile(pathOf(old.folderId), old.name, old.extension, finalName, newExtension)
        files[index] = old.copy(name = finalName, extension = newExtension)
    }

    /** 收集 [id] 及其所有子孙文件夹的 id。 */
    private fun descendantFolderIds(id: Long): Set<Long> {
        val result = mutableSetOf(id)
        var frontier = listOf(id)
        while (frontier.isNotEmpty()) {
            val children = folders.filter { it.parentId in frontier }.map { it.id }
            result.addAll(children)
            frontier = children
        }
        return result
    }

    /** 判断 [id] 的某个祖先是否在 [ids] 中（避免父子文件夹同时被移动/复制）。 */
    private fun hasAncestorIn(id: Long, ids: Set<Long>): Boolean {
        var current = folder(id)?.parentId
        while (current != null) {
            if (current in ids) return true
            current = folder(current)?.parentId
        }
        return false
    }

    fun deleteFolder(id: Long) {
        val folder = folder(id) ?: return
        val ids = descendantFolderIds(id)
        storage.deleteFolder(pathOf(id))
        folders.removeAll { it.id in ids }
        files.removeAll { it.folderId in ids }
    }

    fun deleteFile(id: Long) {
        val file = file(id) ?: return
        storage.deleteFile(pathOf(file.folderId), file.name, file.extension)
        files.removeAll { it.id == id }
    }

    fun deleteItems(fileIds: Set<Long>, folderIds: Set<Long>) {
        fileIds.forEach { deleteFile(it) }
        folderIds.forEach { deleteFolder(it) }
    }

    fun updateFileContent(id: Long, content: String) {
        val index = files.indexOfFirst { it.id == id }
        if (index < 0) return
        val file = files[index]
        if (file.content == content) return
        storage.writeFile(pathOf(file.folderId), file.name, file.extension, content)
        files[index] = file.copy(content = content)
    }

    /** 把多个文件移动到目标文件夹（targetFolderId 为 null 表示根目录，磁盘同步改名/移动）。 */
    fun moveFilesToFolder(ids: Set<Long>, targetFolderId: Long?) {
        for (id in ids) {
            val file = file(id) ?: continue
            if (file.folderId == targetFolderId) continue
            val siblings = files.filter { it.folderId == targetFolderId && it.id != id }.map { it.name }.toSet()
            val finalName = uniqueName(file.name, siblings)
            storage.moveFile(pathOf(file.folderId), file.name, pathOf(targetFolderId), finalName, file.extension)
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
            storage.copyFile(pathOf(file.folderId), file.name, pathOf(targetFolderId), finalName, file.extension)
            files.add(file.copy(id = newId(), name = finalName, folderId = targetFolderId))
        }
    }

    /** 把多个文件夹移动到目标文件夹（含内容；目标为自身或子孙时跳过）。 */
    fun moveFoldersToFolder(ids: Set<Long>, targetFolderId: Long?) {
        for (id in ids) {
            val folder = folder(id) ?: continue
            if (folder.parentId == targetFolderId) continue
            if (targetFolderId != null && targetFolderId in descendantFolderIds(id)) continue
            if (hasAncestorIn(id, ids)) continue
            val siblings = folders.filter { it.parentId == targetFolderId && it.id != id }.map { it.name }.toSet()
            val finalName = uniqueName(folder.name, siblings)
            storage.moveFolder(pathOf(id), pathOf(targetFolderId) + finalName)
            val index = folders.indexOfFirst { it.id == id }
            folders[index] = folder.copy(name = finalName, parentId = targetFolderId)
        }
    }

    /** 把多个文件夹复制到目标文件夹（含内容，保留原件；目标为自身或子孙时跳过）。 */
    fun copyFoldersToFolder(ids: Set<Long>, targetFolderId: Long?) {
        for (id in ids) {
            val folder = folder(id) ?: continue
            if (folder.parentId == targetFolderId) continue
            if (targetFolderId != null && targetFolderId in descendantFolderIds(id)) continue
            if (hasAncestorIn(id, ids)) continue
            val siblings = folders.filter { it.parentId == targetFolderId }.map { it.name }.toSet()
            val finalName = uniqueName(folder.name, siblings)
            storage.copyFolder(pathOf(id), pathOf(targetFolderId) + finalName)
            cloneFolderTree(id, targetFolderId, finalName)
        }
    }

    /** 在内存中复制文件夹树（新 id），与磁盘 [MonoStorage.copyFolder] 结构保持一致。 */
    private fun cloneFolderTree(srcId: Long, newParentId: Long?, newName: String): Long {
        val src = folder(srcId) ?: return -1L
        val newFolderId = newId()
        folders.add(Folder(id = newFolderId, name = newName, parentId = newParentId))
        folders.filter { it.parentId == srcId }.forEach { child ->
            cloneFolderTree(child.id, newFolderId, child.name)
        }
        files.filter { it.folderId == srcId }.forEach { file ->
            files.add(file.copy(id = newId(), folderId = newFolderId))
        }
        return newFolderId
    }

    /** 选中项中第一个卡片的名字（先文件夹后文件，按列表顺序），用于压缩默认名。 */
    fun firstSelectedName(ids: Set<Long>): String {
        folders.firstOrNull { it.id in ids }?.let { return it.name }
        files.firstOrNull { it.id in ids }?.let { return it.name }
        return ""
    }

    /** 把选中的文件与文件夹压缩为单个 `.zip`（落在 parentFolderId 目录），返回生成的压缩文件。 */
    fun compressItemsToZip(
        fileIds: Set<Long>,
        folderIds: Set<Long>,
        zipName: String,
        parentFolderId: Long?,
    ): File? {
        val fileItems = fileIds.mapNotNull { file(it) }.map { it.name to it.extension }
        val folderItems = folderIds.mapNotNull { folder(it) }.map { it.name }
        if (fileItems.isEmpty() && folderItems.isEmpty()) return null
        return storage.compressItemsToZip(pathOf(parentFolderId), zipName, fileItems, folderItems)
    }

    private fun uniqueName(base: String, existing: Set<String>): String {
        if (base !in existing) return base
        var i = 2
        while ("$base ($i)" in existing) i++
        return "$base ($i)"
    }
}
