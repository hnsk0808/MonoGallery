package com.sichuan.monogallery

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.compose.runtime.mutableStateListOf
import java.io.File

/**
 * Repository that keeps the state in memory and writes every change straight back to local disk.
 * The on-disk layout mirrors the UI structure: each folder maps to a (nested) directory and
 * each file maps to a file.
 */
class MonoLibrary(context: Context) {
    private val storage = MonoStorage(context)

    /** The library's directory on disk: everything the app itself stores lives below it. */
    val storageRoot: File get() = storage.root

    val folders = mutableStateListOf<Folder>()
    val files = mutableStateListOf<MonoFile>()
    private var nextId = 1L

    init {
        val (loadedFolders, loadedFiles) = storage.load()
        folders.addAll(loadedFolders)
        files.addAll(loadedFiles)
        reserveNextId()
    }

    /** Allocates the next unique item id from the single shared id space used by folders and files. */
    private fun newId(): Long = nextId++

    /** Reserves the next id above the largest id already loaded so new items never collide. */
    private fun reserveNextId() {
        nextId = maxOf(
            folders.maxOfOrNull { it.id } ?: 0L,
            files.maxOfOrNull { it.id } ?: 0L,
        ) + 1
    }

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
        reserveNextId()
    }

    fun folder(id: Long): Folder? = folders.firstOrNull { it.id == id }
    fun file(id: Long): MonoFile? = files.firstOrNull { it.id == id }

    /** Splits [ids] into the file ids and the folder ids they currently refer to. */
    fun partitionIds(ids: Set<Long>): Pair<Set<Long>, Set<Long>> =
        ids.filter { file(it) != null }.toSet() to ids.filter { folder(it) != null }.toSet()

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

    /**
     * Imports a SAF-picked document ([uri]) into [parentFolderId], keeping its original name and
     * extension (de-duplicated against siblings), and registers it in memory. Text files have
     * their content read in as well.
     */
    fun importFile(context: Context, uri: Uri, parentFolderId: Long?): MonoFile {
        val display = displayNameOf(context, uri)
        val (baseName, ext) = splitFullName(display)
        val finalName = uniqueFileName(sanitizeName(baseName), ext, parentFolderId)
        context.contentResolver.openInputStream(uri)?.use { input ->
            storage.importFile(pathOf(parentFolderId), finalName, ext, input)
        } ?: error("无法读取所选文件")
        val content = readImportedText(pathOf(parentFolderId), finalName, ext)
        val file = MonoFile(
            id = newId(), name = finalName, extension = ext, content = content, folderId = parentFolderId,
        )
        files.add(file)
        return file
    }

    /**
     * Imports a SAF-picked directory tree ([treeUri]) with all its subfolders and files into
     * [parentFolderId], recreating the structure on disk and in memory.
     */
    fun importFolderTree(context: Context, treeUri: Uri, parentFolderId: Long?): Folder {
        val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
        val rootName = uniqueFolderName(
            sanitizeName(displayNameOf(context, rootDocUri)), parentFolderId,
        )
        storage.createFolderDir(pathOf(parentFolderId), rootName)
        val root = Folder(id = newId(), name = rootName, parentId = parentFolderId)
        folders.add(root)
        walkDocumentTree(context, treeUri, treeDocId, root.id)
        return root
    }

    /** Recursively recreates the SAF document [docId] under [parentFolderId]. */
    private fun walkDocumentTree(context: Context, treeUri: Uri, docId: String, parentFolderId: Long) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            ),
            null, null, null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val childDocId = cursor.getString(0)
                val mime = cursor.getString(1)
                val name = cursor.getString(2) ?: continue
                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    val finalName = uniqueFolderName(sanitizeName(name), parentFolderId)
                    storage.createFolderDir(pathOf(parentFolderId), finalName)
                    val folder = Folder(id = newId(), name = finalName, parentId = parentFolderId)
                    folders.add(folder)
                    walkDocumentTree(context, treeUri, childDocId, folder.id)
                } else {
                    val (baseName, ext) = splitFullName(name)
                    val finalName = uniqueFileName(sanitizeName(baseName), ext, parentFolderId)
                    context.contentResolver.openInputStream(docUri)?.use { input ->
                        storage.importFile(pathOf(parentFolderId), finalName, ext, input)
                    }
                    val content = readImportedText(pathOf(parentFolderId), finalName, ext)
                    files.add(
                        MonoFile(
                            id = newId(), name = finalName, extension = ext,
                            content = content, folderId = parentFolderId,
                        )
                    )
                }
            }
        }
    }

    /** Returns the display name of a document URI, falling back to the last URI segment. */
    private fun displayNameOf(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index)?.let { return it }
            }
        }
        return uri.lastPathSegment?.substringAfterLast(':') ?: "导入文件"
    }

    /** De-duplicates a file name for [folderId] against files with the same name and extension. */
    private fun uniqueFileName(base: String, extension: String, folderId: Long?): String =
        uniqueName(base) { name ->
            files.any {
                it.folderId == folderId && it.name == name && it.extension.equals(extension, ignoreCase = true)
            }
        }

    /** De-duplicates a folder name against sibling folders of [parentFolderId]. */
    private fun uniqueFolderName(base: String, parentFolderId: Long?): String =
        uniqueName(base, folders.filter { it.parentId == parentFolderId }.map { it.name }.toSet())

    /** Reads the text content of an imported text file, returning an empty string for other types or failures. */
    private fun readImportedText(path: List<String>, name: String, extension: String): String {
        if (FileType.fromExtension(extension) != FileType.TEXT) return ""
        return try {
            storage.fileFor(path, name, extension).readText()
        } catch (_: Exception) {
            ""
        }
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
        val finalName = uniqueName(base) { candidate ->
            files.any {
                it.folderId == old.folderId && it.id != id &&
                    it.name == candidate && it.extension.lowercase() == newExtension
            }
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
     * Moves ([copy] = false) or copies ([copy] = true) several files to the target folder
     * (a null [targetFolderId] means the root directory), renaming on disk as needed to keep
     * names unique. Copying registers fresh ids, so the originals stay put.
     */
    private fun transferFilesToFolder(ids: Set<Long>, targetFolderId: Long?, copy: Boolean) {
        for (id in ids) {
            val file = file(id) ?: continue
            if (file.folderId == targetFolderId) continue
            val siblings = files.filter { it.folderId == targetFolderId && it.id != id }.map { it.name }.toSet()
            val finalName = uniqueName(file.name, siblings)
            val sourcePath = pathOf(file.folderId)
            val targetPath = pathOf(targetFolderId)
            if (copy) {
                storage.copyFile(sourcePath, file.name, targetPath, finalName, file.extension)
                files.add(file.copy(id = newId(), name = finalName, folderId = targetFolderId))
            } else {
                storage.moveFile(sourcePath, file.name, targetPath, finalName, file.extension)
                val index = files.indexOfFirst { it.id == id }
                files[index] = file.copy(name = finalName, folderId = targetFolderId)
            }
        }
    }

    /**
     * Moves several files to the target folder (a null targetFolderId means the root directory),
     * renaming on disk as needed to keep names unique.
     */
    fun moveFilesToFolder(ids: Set<Long>, targetFolderId: Long?) =
        transferFilesToFolder(ids, targetFolderId, copy = false)

    /** Copies several files to the target folder (a null targetFolderId means the root directory), keeping the originals. */
    fun copyFilesToFolder(ids: Set<Long>, targetFolderId: Long?) =
        transferFilesToFolder(ids, targetFolderId, copy = true)

    /**
     * Moves ([copy] = false) or copies ([copy] = true) several folders (with their contents) to the
     * target folder. Skips a folder when the target is the folder itself, one of its descendants, or
     * when one of its ancestors is also being transferred (a parent brings the child along).
     */
    private fun transferFoldersToFolder(ids: Set<Long>, targetFolderId: Long?, copy: Boolean) {
        for (id in ids) {
            val folder = folder(id) ?: continue
            if (folder.parentId == targetFolderId) continue
            if (targetFolderId != null && targetFolderId in descendantFolderIds(id)) continue
            if (hasAncestorIn(id, ids)) continue
            val siblings = folders.filter { it.parentId == targetFolderId && it.id != id }.map { it.name }.toSet()
            val finalName = uniqueName(folder.name, siblings)
            val targetPath = pathOf(targetFolderId) + finalName
            if (copy) {
                storage.copyFolder(pathOf(id), targetPath)
                cloneFolderTree(id, targetFolderId, finalName)
            } else {
                storage.moveFolder(pathOf(id), targetPath)
                val index = folders.indexOfFirst { it.id == id }
                folders[index] = folder.copy(name = finalName, parentId = targetFolderId)
            }
        }
    }

    /** Moves several folders (with their contents) to the target folder; see [transferFoldersToFolder] for the skips. */
    fun moveFoldersToFolder(ids: Set<Long>, targetFolderId: Long?) =
        transferFoldersToFolder(ids, targetFolderId, copy = false)

    /** Copies several folders (with their contents) to the target folder, keeping the originals. */
    fun copyFoldersToFolder(ids: Set<Long>, targetFolderId: Long?) =
        transferFoldersToFolder(ids, targetFolderId, copy = true)

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
}
