package com.sichuan.monogallery

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

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
 * video every time.
 */
@Composable
fun rememberVideoFrame(file: File?, targetWidth: Int, targetHeight: Int): ImageResult =
    rememberBitmapResult(file, targetWidth, targetHeight, ::extractVideoFrame)

/** Bounded in-memory cache of frame bitmaps keyed by path, render size and last-modified time. */
private val frameCache = BitmapLruCache(BITMAP_CACHE_BYTES)

/** Time position of the thumbnail frame: 1 second into the video (microseconds). */
private const val THUMBNAIL_FRAME_TIME_US = 1_000_000L

/** Opens the video, extracts and scales a frame, and always releases the retriever. */
private fun extractVideoFrame(file: File, targetWidth: Int, targetHeight: Int): ImageResult {
    val key = bitmapCacheKey(file, targetWidth, targetHeight)
    frameCache.get(key)?.let { return ImageResult.Success(it.asImageBitmap()) }
    var retriever: MediaMetadataRetriever? = null
    return try {
        retriever = MediaMetadataRetriever()
        retriever.setDataSource(file.absolutePath)

        val srcWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            ?.toIntOrNull() ?: 0
        val srcHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            ?.toIntOrNull() ?: 0

        val size = if (srcWidth > 0 && srcHeight > 0) {
            fitWithin(srcWidth, srcHeight, targetWidth, targetHeight)
        } else {
            scaledSize(targetWidth, targetHeight, 1f)
        }
        val dstWidth = size.width
        val dstHeight = size.height

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
            val scaled = scaledSize(
                bitmap.width,
                bitmap.height,
                fitScale(bitmap.width, bitmap.height, dstWidth, dstHeight),
            )
            val resized = Bitmap.createScaledBitmap(bitmap, scaled.width, scaled.height, true)
            if (resized !== bitmap) bitmap.recycle()
            bitmap = resized
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
