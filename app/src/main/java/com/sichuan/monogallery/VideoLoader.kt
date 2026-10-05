package com.sichuan.monogallery

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts a representative frame (around the 1-second mark) of a video on an IO thread,
 * downsamples it to no larger than [targetWidth] x [targetHeight] while preserving the
 * aspect ratio, applies the video's rotation metadata, and returns it as a card thumbnail.
 * Mirrors [rememberPdfCover] so the file card can show a video frame the same way it shows
 * a PDF cover or an image thumbnail. Returns [ImageResult.Error] when the file is missing,
 * not a playable video, or no frame can be decoded.
 *
 * Frame bitmaps are cached in memory (bounded) so a card that leaves and re-enters
 * composition while scrolling the grid reuses the cached frame instead of re-opening the
 * video every time. Bitmaps are never recycled manually — on API 29+ the GC reclaims them,
 * and recycling could crash while a card still shows the frame.
 */
@Composable
fun rememberVideoFrame(file: File?, targetWidth: Int, targetHeight: Int): ImageResult {
    // Key on the path (value equality), not the File object: fileOnDisk returns a new File on
    // every recomposition, and using File as the key would re-extract the frame repeatedly.
    val path = file?.absolutePath
    return produceState<ImageResult>(initialValue = ImageResult.Loading, path, targetWidth, targetHeight) {
        value = if (path == null) {
            ImageResult.Error
        } else {
            withContext(Dispatchers.IO) {
                extractVideoFrame(File(path), targetWidth, targetHeight)
            }
        }
    }.value
}

/** Bounded in-memory cache of frame bitmaps keyed by path, render size and last-modified time. */
private val frameCache = object : LruCache<String, Bitmap>(FRAME_CACHE_BYTES) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
}

private const val FRAME_CACHE_BYTES = 16 * 1024 * 1024

/** Time position of the thumbnail frame: 1 second into the video (microseconds). */
private const val THUMBNAIL_FRAME_TIME_US = 1_000_000L

/** Opens the video, extracts and scales a frame, and always releases the retriever. */
private fun extractVideoFrame(file: File, targetWidth: Int, targetHeight: Int): ImageResult {
    if (!file.exists()) return ImageResult.Error
    // lastModified() invalidates the cache if the file is replaced underneath us.
    val key = "${file.absolutePath}|$targetWidth|$targetHeight|${file.lastModified()}"
    frameCache.get(key)?.let { return ImageResult.Success(it.asImageBitmap()) }
    var retriever: MediaMetadataRetriever? = null
    return try {
        retriever = MediaMetadataRetriever()
        retriever.setDataSource(file.absolutePath)

        val srcWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            ?.toIntOrNull() ?: 0
        val srcHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            ?.toIntOrNull() ?: 0

        var dstWidth = targetWidth
        var dstHeight = targetHeight
        if (srcWidth > 0 && srcHeight > 0) {
            // Scale by min(target / source) so the aspect ratio is preserved and stretching is avoided
            val factor = minOf(
                targetWidth.toFloat() / srcWidth,
                targetHeight.toFloat() / srcHeight,
                1f,
            )
            dstWidth = maxOf(1, (srcWidth * factor).toInt())
            dstHeight = maxOf(1, (srcHeight * factor).toInt())
        }

        // Prefer a frame near the 1-second mark; fall back to the nearest sync frame anywhere
        // in the video (short clips or videos whose first seek lands past the duration).
        var bitmap = retriever.getScaledFrameAtTime(
            THUMBNAIL_FRAME_TIME_US,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
            dstWidth,
            dstHeight,
        ) ?: retriever.getScaledFrameAtTime(
            -1,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
            dstWidth,
            dstHeight,
        ) ?: retriever.frameAtTime

        if (bitmap == null) return ImageResult.Error

        // getScaledFrameAtTime may ignore the requested size on some devices; scale it ourselves
        // if it still exceeds the target bounds.
        if (bitmap.width > dstWidth || bitmap.height > dstHeight) {
            val scale = minOf(
                dstWidth.toFloat() / bitmap.width,
                dstHeight.toFloat() / bitmap.height,
            )
            val scaled = Bitmap.createScaledBitmap(
                bitmap,
                maxOf(1, (bitmap.width * scale).toInt()),
                maxOf(1, (bitmap.height * scale).toInt()),
                true,
            )
            if (scaled !== bitmap) bitmap.recycle()
            bitmap = scaled
        }

        // Frames are returned unrotated; apply the rotation metadata so the thumbnail is upright.
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            ?.toIntOrNull() ?: 0
        if (rotation != 0) {
            val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated !== bitmap) bitmap.recycle()
            bitmap = rotated
        }

        frameCache.put(key, bitmap)
        ImageResult.Success(bitmap.asImageBitmap())
    } catch (_: Exception) {
        ImageResult.Error
    } finally {
        try {
            retriever?.release()
        } catch (_: Exception) {
            // Ignore release failures on an already-closed retriever
        }
    }
}
