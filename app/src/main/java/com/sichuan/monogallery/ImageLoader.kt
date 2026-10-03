package com.sichuan.monogallery

import android.graphics.ImageDecoder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 图片解码结果：加载中 / 失败 / 成功。 */
sealed interface ImageResult {
    object Loading : ImageResult
    object Error : ImageResult
    data class Success(val bitmap: ImageBitmap) : ImageResult
}

/**
 * 在 IO 线程解码图片，并降采样到不超过 [targetWidth]×[targetHeight]（保持原始宽高比），
 * 同时自动处理 EXIF 旋转方向。文件不存在或解码失败时返回 [ImageResult.Error]。
 */
@Composable
fun rememberImageResult(file: File?, targetWidth: Int, targetHeight: Int): ImageResult {
    // 用路径（值相等）而非 File 对象（引用相等）作 key：fileOnDisk 每次重组都会新建 File 实例，
    // 若用 File 作 key 会导致 produceState 每次重组都重启、缩略图反复解码造成滚动卡顿。
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
                            // 按 min(目标/原始) 计算缩放系数，保证宽高比不变，避免拉伸
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
