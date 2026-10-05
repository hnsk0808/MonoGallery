package com.sichuan.monogallery

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import java.io.File

/**
 * Repository that keeps the state in memory and writes every change straight back to local disk.
 * The on-disk layout mirrors the UI structure: each folder maps to a (nested) directory and
 * each file maps to a file.
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
        // Reserve the next id above the largest id already on disk so new items never collide
        nextId = maxOf(
            folders.maxOfOrNull { it.id } ?: 0L,
            files.maxOfOrNull { it.id } ?: 0L,
        ) + 1
    }

    /** Allocates the next unique item id from the single shared id space used by folders and files. */
    private fun newId(): Long = nextId++

    /**
     * Reloads from disk to stay in sync with external changes, then refreshes the UI.
     * Existing item ids are retained so the UI does not shift after an id reassignment.
     */
    fun refresh() {
        val existingFolderIds = folders.associate { (pathOf(it.parentId) + it.name).joinToString("/") to it.id }
        val existingFileIds = files.associate { (pathOf(it.folderId) + it.fullName).joinToString("/") to it.id }
        val (loadedFolders, loadedFiles) = storage.load(existingFolderIds, existingFileIds)
        folders.clear()
        files.clear()
        folders.addAll(loadedFolders)
        files.addAll(loadedFiles)
        nextId = maxOf(
            folders.maxOfOrNull { it.id } ?: 0L,
            files.maxOfOrNull { it.id } ?: 0L,
        ) + 1
    }

    fun folder(id: Long): Folder? = folders.firstOrNull { it.id == id }
    fun file(id: Long): MonoFile? = files.firstOrNull { it.id == id }

    fun rootFolders(): List<Folder> = folders.filter { it.parentId == null }
    fun subfoldersOf(parentId: Long?): List<Folder> = folders.filter { it.parentId == parentId }
    fun rootFiles(): List<MonoFile> = files.filter { it.folderId == null }
    fun filesIn(folderId: Long): List<MonoFile> = files.filter { it.folderId == folderId }

    /**
     * Folders directly under [parentId], sorted by [mode]: by name ascending or by creation
     * time with the newest first. Folders have no type, so [SortMode.TYPE] falls back to name
     * order. Folders whose metadata is unavailable sink to the bottom.
     */
    fun sortedFolders(parentId: Long?, mode: SortMode): List<Folder> {
        val list = folders.filter { it.parentId == parentId }
        return when (mode) {
            SortMode.NAME, SortMode.TYPE -> list.sortedBy { it.name.lowercase() }
            SortMode.DATE -> list.sortedByDescending { folderInfo(it.id)?.createdMillis ?: 0L }
        }
    }

    /**
     * Files directly in [folderId], sorted by [mode]: by name ascending, by extension A-Z and
     * then by name A-Z within the same extension, or by last-modified time with the most recent
     * first. Files whose metadata is unavailable sink to the bottom.
     */
    fun sortedFiles(folderId: Long?, mode: SortMode): List<MonoFile> {
        val list = files.filter { it.folderId == folderId }
        return when (mode) {
            SortMode.NAME -> list.sortedBy { it.name.lowercase() }
            SortMode.TYPE -> list.sortedWith(
                compareBy({ it.extension.lowercase() }, { it.name.lowercase() })
            )
            SortMode.DATE -> list.sortedByDescending { fileInfo(it.id)?.modifiedMillis ?: 0L }
        }
    }

    /** Number of items in a folder: its files plus its subfolders (each subfolder counts as one item). */
    fun itemCount(folderId: Long): Int =
        files.count { it.folderId == folderId } + folders.count { it.parentId == folderId }

    /** On-disk file metadata (for the properties page), or null when the file does not exist. */
    fun fileInfo(id: Long): FileInfo? =
        file(id)?.let { storage.fileInfo(pathOf(it.folderId), it.name, it.extension) }

    /** On-disk folder metadata (for the properties page), or null when the folder does not exist. */
    fun folderInfo(id: Long): FolderInfo? =
        folder(id)?.let { storage.folderInfo(pathOf(it.id)) }

    /** On-disk location of the file (used for sharing), or null when the file does not exist. */
    fun fileOnDisk(id: Long): File? =
        file(id)?.let { storage.fileFor(pathOf(it.folderId), it.name, it.extension) }

    /** Absolute on-disk path of the file (for the properties page), or null when the file does not exist. */
    fun filePath(id: Long): String? = fileOnDisk(id)?.absolutePath

    /** Absolute on-disk path of the folder (for the properties page), or null when the folder does not exist. */
    fun folderPath(id: Long): String? =
        folder(id)?.let { storage.folderFor(pathOf(it.id)).absolutePath }

    /** Zips the folder into the cache directory for sharing, or null when the folder does not exist. */
    fun folderShareZip(id: Long): File? =
        folder(id)?.let { storage.zipFolderToCache(pathOf(it.id)) }

    /** Path segments of the folder relative to the root directory (empty list for the root directory itself). */
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

    /** Creates a folder on disk and in memory, sanitizing the name and de-duplicating it against siblings. */
    fun createFolder(name: String, parentId: Long? = null): Folder {
        val siblings = folders.filter { it.parentId == parentId }.map { it.name }.toSet()
        val finalName = uniqueName(sanitizeName(name), siblings)
        storage.createFolderDir(pathOf(parentId), finalName)
        val folder = Folder(id = newId(), name = finalName, parentId = parentId)
        folders.add(folder)
        return folder
    }

    /** Creates a file on disk and in memory, sanitizing the name and de-duplicating it against siblings. */
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

    /** Renames a folder on disk and in memory, de-duplicating the new name against its siblings. */
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

    /** Renames a file on disk and in memory, splitting the full name into base name and extension. */
    fun renameFile(id: Long, newFullName: String) {
        val index = files.indexOfFirst { it.id == id }
        if (index < 0) return
        val old = files[index]

        val (base, ext) = splitFullName(sanitizeName(newFullName.trim()))
        val newExtension = ext.lowercase()

        // De-duplicate: if the same full file name already exists in the directory, append (n)
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

    /** Collects the ids of [id] and all of its descendant folders. */
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

    /** Returns whether an ancestor of [id] is in [ids] (prevents moving/copying a parent folder together with its child). */
    private fun hasAncestorIn(id: Long, ids: Set<Long>): Boolean {
        var current = folder(id)?.parentId
        while (current != null) {
            if (current in ids) return true
            current = folder(current)?.parentId
        }
        return false
    }

    /** Deletes a folder and all of its descendants, recursively removing their files too. */
    fun deleteFolder(id: Long) {
        val folder = folder(id) ?: return
        val ids = descendantFolderIds(id)
        storage.deleteFolder(pathOf(id))
        folders.removeAll { it.id in ids }
        files.removeAll { it.folderId in ids }
    }

    /** Deletes a single file from disk and memory. */
    fun deleteFile(id: Long) {
        val file = file(id) ?: return
        storage.deleteFile(pathOf(file.folderId), file.name, file.extension)
        files.removeAll { it.id == id }
    }

    /** Deletes a multi-selection of files and folders. */
    fun deleteItems(fileIds: Set<Long>, folderIds: Set<Long>) {
        fileIds.forEach { deleteFile(it) }
        folderIds.forEach { deleteFolder(it) }
    }

    /** Writes new file content back to disk and updates it in memory (no-op when unchanged). */
    fun updateFileContent(id: Long, content: String) {
        val index = files.indexOfFirst { it.id == id }
        if (index < 0) return
        val file = files[index]
        if (file.content == content) return
        storage.writeFile(pathOf(file.folderId), file.name, file.extension, content)
        files[index] = file.copy(content = content)
    }

    /**
     * Moves several files to the target folder (a null targetFolderId means the root directory),
     * renaming on disk as needed to keep names unique.
     */
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

    /** Copies several files to the target folder (a null targetFolderId means the root directory), keeping the originals. */
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

    /** Moves several folders (with their contents) to the target folder; skips when the target is the folder itself or a descendant. */
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

    /** Copies several folders (with their contents) to the target folder, keeping the originals; skips when the target is the folder itself or a descendant. */
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

    /** Clones the folder tree in memory (fresh ids), matching the on-disk structure of [MonoStorage.copyFolder]. */
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

    /** Name of the first selected card (folders before files, in list order), used as the default compress name. */
    fun firstSelectedName(ids: Set<Long>): String {
        folders.firstOrNull { it.id in ids }?.let { return it.name }
        files.firstOrNull { it.id in ids }?.let { return it.name }
        return ""
    }

    /** Compresses the selected files and folders into a single `.zip` (placed in the parentFolderId directory), returning the created archive. */
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
