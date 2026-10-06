package com.sichuan.monogallery

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Image preview: dark background, fit-to-screen, supporting pinch-to-zoom and pan when zoomed
 * in. A tap invokes [onTap], which the fullscreen layout uses to toggle the overlay title bar.
 * GIF files autoplay via [AnimatedImageDrawable]; other formats use the static bitmap path.
 */
@Composable
fun ImageViewer(
    file: File?,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val targetWidth = with(density) { configuration.screenWidthDp.dp.roundToPx() }
    val targetHeight = with(density) { configuration.screenHeightDp.dp.roundToPx() }
    val isGif = file?.extension?.lowercase() == "gif"
    // Static path is skipped for GIF so we never waste a decode on the first frame.
    val result = if (!isGif) rememberImageResult(file, targetWidth, targetHeight) else null

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(onTap) {
                // Separate detector: taps toggle the overlay, while transform gestures handle zoom/pan
                detectTapGestures(onTap = { onTap?.invoke() })
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    // Clamp zoom between 1x and 5x; only allow panning while zoomed in, and reset the
                    // offset when the image returns to 1x so it stays centered
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    scale = newScale
                    offset = if (newScale > 1f) offset + pan else Offset.Zero
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isGif) {
            GifImage(
                file = file,
                targetWidth = targetWidth,
                targetHeight = targetHeight,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                    ),
            )
        } else {
            when (result) {
                ImageResult.Loading -> CircularProgressIndicator(color = Color.White)
                ImageResult.Error -> Text("无法加载图片", color = Color.White)
                is ImageResult.Success -> Image(
                    bitmap = result.bitmap,
                    contentDescription = "图片预览",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y,
                        ),
                )
                null -> CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

/**
 * GIF preview: decodes with [ImageDecoder.decodeDrawable] so multi-frame GIFs come back as an
 * [AnimatedImageDrawable], which starts on display (autoplay) and stops on dispose.
 * Downsamples to the screen size like the static path to keep large GIFs cheap.
 */
@Composable
private fun GifImage(
    file: File?,
    targetWidth: Int,
    targetHeight: Int,
    modifier: Modifier = Modifier,
) {
    val path = file?.absolutePath
    var drawable by remember(path) { mutableStateOf<Drawable?>(null) }
    var loadError by remember(path) { mutableStateOf(false) }

    LaunchedEffect(path, targetWidth, targetHeight) {
        val target = path?.let { File(it) }
        if (target == null || !target.exists()) {
            loadError = true
            return@LaunchedEffect
        }
        drawable = null
        loadError = false
        val decoded = withContext(Dispatchers.IO) {
            try {
                val source = ImageDecoder.createSource(target)
                ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
                    val srcWidth = info.size.width
                    val srcHeight = info.size.height
                    if (srcWidth > 0 && srcHeight > 0) {
                        val size = fitWithin(srcWidth, srcHeight, targetWidth, targetHeight)
                        decoder.setTargetSize(size.width, size.height)
                    }
                }
            } catch (_: Exception) {
                null
            }
        }
        if (decoded == null) {
            loadError = true
        } else {
            drawable = decoded
        }
    }

    DisposableEffect(drawable) {
        (drawable as? AnimatedImageDrawable)?.start()
        onDispose {
            (drawable as? AnimatedImageDrawable)?.stop()
        }
    }

    when {
        loadError -> Text("无法加载图片", color = Color.White)
        drawable == null -> CircularProgressIndicator(color = Color.White)
        else -> AndroidView(
            factory = { context ->
                ImageView(context).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
            },
            update = { view ->
                if (view.drawable !== drawable) {
                    view.setImageDrawable(drawable)
                }
            },
            modifier = modifier,
        )
    }
}
