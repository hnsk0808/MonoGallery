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
