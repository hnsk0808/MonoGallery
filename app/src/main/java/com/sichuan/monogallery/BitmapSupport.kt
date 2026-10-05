package com.sichuan.monogallery

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Bitmap loading result: loading, error, or success. */
sealed interface ImageResult {
    object Loading : ImageResult
    object Error : ImageResult
    data class Success(val bitmap: ImageBitmap) : ImageResult
}

/** Default in-memory budget for a card thumbnail cache. */
internal const val BITMAP_CACHE_BYTES = 16 * 1024 * 1024

/**
 * In-memory bitmap cache bounded by the total byte count of the bitmaps it holds.
 *
 * Bitmaps are never recycled manually — on API 29+ the GC reclaims them, and recycling could
 * crash while a card still shows the bitmap.
 */
internal class BitmapLruCache(maxBytes: Int) : LruCache<String, Bitmap>(maxBytes) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
}

/**
 * Cache key for a bitmap derived from [file] at a given render size.
 * [File.lastModified] is part of the key so the cache is invalidated if the file is replaced
 * underneath us.
 */
internal fun bitmapCacheKey(file: File, targetWidth: Int, targetHeight: Int): String =
    "${file.absolutePath}|$targetWidth|$targetHeight|${file.lastModified()}"

/**
 * Scale factor that fits [srcWidth] x [srcHeight] inside [targetWidth] x [targetHeight] while
 * preserving the aspect ratio. May exceed 1 (upscaling); callers that must not upscale apply
 * `minOf(scale, 1f)` via [fitWithin].
 */
internal fun fitScale(srcWidth: Int, srcHeight: Int, targetWidth: Int, targetHeight: Int): Float =
    minOf(
        targetWidth.toFloat() / srcWidth,
        targetHeight.toFloat() / srcHeight,
    )

/** Applies [scale] to a source size, never yielding a zero-sized dimension. */
internal fun scaledSize(srcWidth: Int, srcHeight: Int, scale: Float): IntSize =
    IntSize(
        maxOf(1, (srcWidth * scale).toInt()),
        maxOf(1, (srcHeight * scale).toInt()),
    )

/**
 * Size that fits [srcWidth] x [srcHeight] inside [targetWidth] x [targetHeight], preserving the
 * aspect ratio and never upscaling. Scale by min(target / source) so stretching is avoided.
 */
internal fun fitWithin(srcWidth: Int, srcHeight: Int, targetWidth: Int, targetHeight: Int): IntSize =
    scaledSize(srcWidth, srcHeight, minOf(fitScale(srcWidth, srcHeight, targetWidth, targetHeight), 1f))

/**
 * Decodes a bitmap for [file] off the main thread and exposes it as [ImageResult], re-running
 * whenever the path or target size changes. Returns [ImageResult.Error] when the file is missing.
 *
 * The state is keyed on the *path* (value equality) rather than the [File] object, because
 * `fileOnDisk` creates a new File instance on every recomposition: using the File itself as the
 * key would restart the effect every time, repeatedly decoding thumbnails and causing scroll jank.
 */
@Composable
internal fun rememberBitmapResult(
    file: File?,
    targetWidth: Int,
    targetHeight: Int,
    decode: (File, Int, Int) -> ImageResult,
): ImageResult {
    val path = file?.absolutePath
    return produceState<ImageResult>(initialValue = ImageResult.Loading, path, targetWidth, targetHeight) {
        val target = path?.let { File(it) }
        value = if (target == null || !target.exists()) {
            ImageResult.Error
        } else {
            withContext(Dispatchers.IO) { decode(target, targetWidth, targetHeight) }
        }
    }.value
}
