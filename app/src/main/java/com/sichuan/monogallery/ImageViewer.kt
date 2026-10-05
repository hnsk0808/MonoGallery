package com.sichuan.monogallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import java.io.File

/**
 * Image preview: dark background, fit-to-screen, supporting pinch-to-zoom and pan when zoomed
 * in. A tap invokes [onTap], which the fullscreen layout uses to toggle the overlay title bar.
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
    val result = rememberImageResult(file, targetWidth, targetHeight)

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
        }
    }
}
