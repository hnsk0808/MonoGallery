package com.sichuan.monogallery

import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Route of the 本地图片 page, the flat list of images that live outside the library. */
internal const val LocalImagesRoute = "local_images"

/** Name of the virtual folder that gathers every image outside the library. */
internal const val LocalImagesFolderName = "本地图片"

/** Route of the 本地图片 folder's properties page. */
internal const val LocalImagesFolderPropertiesRoute = "local_images_props"

/**
 * The images outside the library, as one session-wide scan result (see [scanLocalImages]).
 *
 * The 本地 tab card and the 本地图片 page show the same thing — a count and the grid — so they share
 * this state instead of scanning separately: the count can never disagree with the grid, and the
 * walk over the storage runs once per visit instead of once per reader. The scan happens off the
 * main thread and the result replaces the previous one only when it completes.
 */
@Stable
class LocalImagesState(private val excludedDir: File) {
    /** The images found by the last completed scan (name-sorted); empty until one completes. */
    var images by mutableStateOf<List<File>>(emptyList())
        private set

    /** Whether a scan has completed, so an empty [images] can be told apart from "not scanned yet". */
    var hasScanned by mutableStateOf(false)
        private set

    /** Walks the storage for the images outside the library. */
    suspend fun scan() {
        images = withContext(Dispatchers.IO) { scanLocalImages(excludedDir) }
        hasScanned = true
    }

    /** Scans only when nothing has been scanned yet; the refresh action calls [scan] instead. */
    suspend fun scanOnce() {
        if (!hasScanned) scan()
    }

    /**
     * Points the list at [newFile] in place of [oldFile] after the user renamed it, so the grid shows
     * the new name immediately instead of waiting for the next (whole-storage) scan.
     */
    fun replaceImage(oldFile: File, newFile: File) {
        images = images.map { if (it.absolutePath == oldFile.absolutePath) newFile else it }
    }

    /** The images in [mode] order, mirroring how the library orders its own files. */
    fun sortedImages(mode: SortMode): List<File> = sortLocalImages(images, mode)

    /** Combined size of every image found, for the folder's properties. */
    fun totalSize(): Long = images.sumOf { it.length() }

    /** Modification time of the oldest image found, or null when there is none. */
    fun earliestMillis(): Long? = images.minOfOrNull { it.lastModified() }
}

/**
 * Multi-selection of the 本地图片 grid, the counterpart of [FileSelectionState] for files that have
 * no library id: the selection is keyed by absolute path instead. Enter, toggle and exit work the
 * same way, so the selection feels identical to a library directory's.
 */
@Stable
class LocalImageSelectionState {
    var mode by mutableStateOf(false)
        private set
    var paths by mutableStateOf(setOf<String>())
        private set

    fun enter(path: String) {
        mode = true
        paths = paths + path
    }

    fun toggle(path: String) {
        paths = if (path in paths) paths - path else paths + path
        if (paths.isEmpty()) mode = false
    }

    fun exit() {
        mode = false
        paths = emptySet()
    }
}

/** Remembers this session's [LocalImagesState], created once next to the library it complements. */
@Composable
fun rememberLocalImages(excludedDir: File): LocalImagesState =
    remember(excludedDir) { LocalImagesState(excludedDir) }

/**
 * Every image on the device that does not belong to the library: all files below the shared storage
 * root except the ones inside [excludedDir] (the library's own directory) and its subdirectories,
 * returned as one flat name-sorted list — the folders they came from are walked, never shown.
 *
 * Only the file system is read, which the app is already allowed to do ([MonoStorage] works the
 * same way); no media index is involved, so the result is exact rather than eventual.
 *
 * Pruned along the way: hidden directories (`.thumbnails` and friends) and the two package-private
 * `Android` directories, which hold app internals rather than the user's pictures and are usually
 * unreadable anyway. Unreadable directories anywhere else are skipped instead of failing the scan.
 */
fun scanLocalImages(excludedDir: File): List<File> {
    val root = Environment.getExternalStorageDirectory() ?: return emptyList()
    val excludedPaths = setOf(
        excludedDir.absolutePath,
        File(root, "Android/data").absolutePath,
        File(root, "Android/obb").absolutePath,
    )
    return root.walkTopDown()
        .onEnter { directory ->
            directory.absolutePath !in excludedPaths && directory.name.firstOrNull() != '.'
        }
        .onFail { _, _ -> }
        .filter { it.isFile && it.name.firstOrNull() != '.' && it.isImage() }
        .sortedBy { it.name.lowercase() }
        .toList()
}

/** Whether this file is an image, judged by the shared extension table ([FileType]). */
private fun File.isImage(): Boolean = FileType.fromExtension(extension) == FileType.IMAGE

/**
 * Orders local images by [mode] the same way the library orders its files: name ascending
 * (case-insensitive), type by extension then name, date newest first.
 */
fun sortLocalImages(images: List<File>, mode: SortMode): List<File> = when (mode) {
    SortMode.NAME -> images.sortedBy { it.name.lowercase() }
    SortMode.TYPE -> images.sortedWith(compareBy({ it.extension.lowercase() }, { it.name.lowercase() }))
    SortMode.DATE -> images.sortedByDescending { it.lastModified() }
}

/**
 * Renames a local image inside its own directory (the app does not move files it does not own).
 *
 * The new name goes through the same steps as a library rename — sanitized, split into base name and
 * extension, extension lowercased, duplicates resolved with a Windows-style " (n)" suffix — so the
 * two rename paths behave alike.
 *
 * Returns the renamed file, or null when there is nothing to do (the name is unchanged) or the file
 * system refuses the rename.
 */
fun renameLocalImage(file: File, newFullName: String): File? {
    val parent = file.parentFile ?: return null
    val (base, extension) = splitFullName(sanitizeName(newFullName.trim()))
    val newExtension = extension.lowercase()

    val taken: (String) -> Boolean = { candidate ->
        val sibling = File(parent, nameWith(candidate, newExtension))
        sibling.exists() && sibling.absolutePath != file.absolutePath
    }
    val finalName = uniqueName(base, taken)

    if (finalName == file.nameWithoutExtension && newExtension == file.extension.lowercase()) return null

    val target = File(parent, nameWith(finalName, newExtension))
    return if (file.renameTo(target)) target else null
}

/** Full file name for a base name and extension, matching how library files are named on disk. */
private fun nameWith(base: String, extension: String): String = if (extension.isBlank()) base else "$base.$extension"
