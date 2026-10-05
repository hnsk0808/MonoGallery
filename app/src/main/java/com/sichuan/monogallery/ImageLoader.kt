package com.sichuan.monogallery

import android.graphics.ImageDecoder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Image decoding result: loading, error, or success. */
sealed interface ImageResult {
    object Loading : ImageResult
    object Error : ImageResult
    data class Success(val bitmap: ImageBitmap) : ImageResult
}

/**
 * Decodes an image on an IO thread, downsamples it to no larger than
 * [targetWidth] x [targetHeight] while preserving the original aspect ratio, and applies
 * the EXIF rotation automatically. Returns [ImageResult.Error] when the file is missing
 * or decoding fails.
 */
@Composable
fun rememberImageResult(file: File?, targetWidth: Int, targetHeight: Int): ImageResult {
    // Use the path (value equality) rather than the File object (reference equality) as the key:
    // fileOnDisk creates a new File instance on every recomposition, so using File as the key would
    // restart produceState on every recomposition, repeatedly decoding thumbnails and causing scroll jank.
    val path = file?.absolutePath
    return produceState<ImageResult>(initialValue = ImageResult.Loading, path, targetWidth, targetHeight) {
        val target = path?.let { File(it) }
        value = if (target == null || !target.exists()) {
            ImageResult.Error
        } else {
            withContext(Dispatchers.IO) {
                try {
                    val source = ImageDecoder.createSource(target)
                    val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        val srcWidth = info.size.width
                        val srcHeight = info.size.height
                        if (srcWidth > 0 && srcHeight > 0) {
                            // Scale by min(target / source) so the aspect ratio is preserved and stretching is avoided
                            val factor = minOf(
                                targetWidth.toFloat() / srcWidth,
                                targetHeight.toFloat() / srcHeight,
                                1f,
                            )
                            decoder.setTargetSize(
                                maxOf(1, (srcWidth * factor).toInt()),
                                maxOf(1, (srcHeight * factor).toInt()),
                            )
                        }
                    }
                    ImageResult.Success(bitmap.asImageBitmap())
                } catch (_: Exception) {
                    ImageResult.Error
                }
            }
        }
    }.value
}
