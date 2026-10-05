package com.sichuan.monogallery

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

/**
 * Renders the first page (the cover) of a PDF on an IO thread, downsamples it to no larger
 * than [targetWidth] x [targetHeight] while preserving the aspect ratio, and paints a white
 * background (PDF pages may be transparent). Mirrors [rememberImageResult] so the file card
 * can show the PDF cover the same way it shows an image thumbnail. Returns [ImageResult.Error]
 * when the file is missing, not a valid PDF, or has no pages.
 *
 * Cover bitmaps are cached in memory (bounded) so a card that leaves and re-enters composition
 * while scrolling the grid reuses the cached bitmap instead of re-opening and re-rendering the
 * PDF every time.
 */
@Composable
fun rememberPdfCover(file: File?, targetWidth: Int, targetHeight: Int): ImageResult =
    rememberBitmapResult(file, targetWidth, targetHeight, ::renderPdfCover)

/** Bounded in-memory cache of cover bitmaps keyed by path, render size and last-modified time. */
private val coverCache = BitmapLruCache(BITMAP_CACHE_BYTES)

/** Opens the PDF, renders page 0 into a scaled ARGB bitmap, and always closes the page, renderer, and file descriptor. */
private fun renderPdfCover(file: File, targetWidth: Int, targetHeight: Int): ImageResult {
    val key = bitmapCacheKey(file, targetWidth, targetHeight)
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
        val size = fitWithin(page.width, page.height, targetWidth, targetHeight)
        val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
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
