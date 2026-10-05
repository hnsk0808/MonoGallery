package com.sichuan.monogallery

import android.graphics.ImageDecoder
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

/**
 * Decodes an image on an IO thread, downsamples it to no larger than
 * [targetWidth] x [targetHeight] while preserving the original aspect ratio, and applies
 * the EXIF rotation automatically. Returns [ImageResult.Error] when the file is missing
 * or decoding fails.
 */
@Composable
fun rememberImageResult(file: File?, targetWidth: Int, targetHeight: Int): ImageResult =
    rememberBitmapResult(file, targetWidth, targetHeight) { target, width, height ->
        try {
            val source = ImageDecoder.createSource(target)
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val srcWidth = info.size.width
                val srcHeight = info.size.height
                if (srcWidth > 0 && srcHeight > 0) {
                    val size = fitWithin(srcWidth, srcHeight, width, height)
                    decoder.setTargetSize(size.width, size.height)
                }
            }
            ImageResult.Success(bitmap.asImageBitmap())
        } catch (_: Exception) {
            ImageResult.Error
        }
    }
