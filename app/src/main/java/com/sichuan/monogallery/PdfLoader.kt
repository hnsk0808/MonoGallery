package com.sichuan.monogallery

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders the first page (the cover) of a PDF on an IO thread, downsamples it to no larger
 * than [targetWidth] x [targetHeight] while preserving the aspect ratio, and paints a white
 * background (PDF pages may be transparent). Mirrors [rememberImageResult] so the file card
 * can show the PDF cover the same way it shows an image thumbnail. Returns [ImageResult.Error]
 * when the file is missing, not a valid PDF, or has no pages.
 *
 * Cover bitmaps are cached in memory (bounded) so a card that leaves and re-enters composition
 * while scrolling the grid reuses the cached bitmap instead of re-opening and re-rendering the
 * PDF every time. Bitmaps are never recycled manually — on API 29+ the GC reclaims them, and
 * recycling could crash while a card still shows the cover.
 */
@Composable
fun rememberPdfCover(file: File?, targetWidth: Int, targetHeight: Int): ImageResult {
    // Key on the path (value equality), not the File object: fileOnDisk returns a new File on
    // every recomposition, and using File as the key would re-render the cover repeatedly.
    val path = file?.absolutePath
    return produceState<ImageResult>(initialValue = ImageResult.Loading, path, targetWidth, targetHeight) {
        value = if (path == null) {
            ImageResult.Error
        } else {
            withContext(Dispatchers.IO) {
                renderPdfCover(File(path), targetWidth, targetHeight)
            }
        }
    }.value
}

/** Bounded in-memory cache of cover bitmaps keyed by path, render size and last-modified time. */
private val coverCache = object : LruCache<String, Bitmap>(COVER_CACHE_BYTES) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
}

private const val COVER_CACHE_BYTES = 16 * 1024 * 1024

/** Opens the PDF, renders page 0 into a scaled ARGB bitmap, and always closes the page, renderer, and file descriptor. */
private fun renderPdfCover(file: File, targetWidth: Int, targetHeight: Int): ImageResult {
    if (!file.exists()) return ImageResult.Error
    // lastModified() invalidates the cache if the file is replaced underneath us.
    val key = "${file.absolutePath}|$targetWidth|$targetHeight|${file.lastModified()}"
    coverCache.get(key)?.let { return ImageResult.Success(it.asImageBitmap()) }
    var pfd: ParcelFileDescriptor? = null
    var renderer: PdfRenderer? = null
    var page: PdfRenderer.Page? = null
    return try {
        pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        renderer = PdfRenderer(pfd)
        if (renderer.pageCount <= 0) return ImageResult.Error
        page = renderer.openPage(0)
        if (page.width <= 0 || page.height <= 0) return ImageResult.Error
        // Scale by min(target / source) so the aspect ratio is preserved and stretching is avoided
        val factor = minOf(
            targetWidth.toFloat() / page.width,
            targetHeight.toFloat() / page.height,
            1f,
        )
        val width = maxOf(1, (page.width * factor).toInt())
        val height = maxOf(1, (page.height * factor).toInt())
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        coverCache.put(key, bitmap)
        ImageResult.Success(bitmap.asImageBitmap())
    } catch (_: Exception) {
        ImageResult.Error
    } finally {
        page?.close()
        renderer?.close()
        pfd?.close()
    }
}
