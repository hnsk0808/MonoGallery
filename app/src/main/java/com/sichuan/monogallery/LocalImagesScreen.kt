package com.sichuan.monogallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import java.io.File

/**
 * The 本地图片 page: every image outside the library (see [scanLocalImages]) gathered into a single
 * flat grid — the folders those images live in are not reproduced.
 *
 * The page is deliberately read-only. Its top bar has no "new" action and its cards have no context
 * menu, because these files belong to the device rather than to the library: creating, renaming,
 * sharing and deleting them is left to whichever app owns them. The only action is refresh, which
 * re-walks the storage.
 */
@Composable
fun LocalImagesScreen(
    state: LocalImagesState,
    onBack: () -> Unit,
) {
    // The image shown fullscreen, or null while the grid is up
    var opened by remember { mutableStateOf<File?>(null) }

    val image = opened
    if (image == null) {
        LocalImagesGrid(
            state = state,
            onBack = onBack,
            onOpenImage = { opened = it },
        )
    } else {
        LocalImageViewer(file = image, onClose = { opened = null })
    }
}

/** The grid of local images, plus the refresh action that re-runs the scan. */
@Composable
private fun LocalImagesGrid(
    state: LocalImagesState,
    onBack: () -> Unit,
    onOpenImage: (File) -> Unit,
) {
    val scope = rememberCoroutineScope()

    MonoScaffold(
        title = LocalImagesFolderName,
        onBack = onBack,
        actions = {
            IconButton(onClick = { scope.launch { state.scan() } }) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = "刷新")
            }
        },
    ) { innerPadding ->
        val images = state.images
        when {
            !state.hasScanned -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            images.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "暂无本地图片", color = ColorTextSecondary)
            }

            else -> MonoGrid(modifier = Modifier.padding(innerPadding)) {
                items(images, key = { it.absolutePath }) { image ->
                    LocalImageCard(file = image, onClick = { onOpenImage(image) })
                }
            }
        }
    }
}

/**
 * Fullscreen viewer for one local image: the shared overlay chrome and image viewer in place, since
 * a file outside the library has no library id to route to.
 */
@Composable
private fun LocalImageViewer(
    file: File,
    onClose: () -> Unit,
) {
    var chromeVisible by remember { mutableStateOf(true) }

    // The overlay's back button goes through the activity dispatcher, so intercept it here
    BackHandler(onBack = onClose)

    FullscreenPreviewScreen(
        title = file.name,
        onBack = onClose,
        chromeVisible = chromeVisible,
        onChromeVisibleChange = { chromeVisible = it },
    ) {
        ImageViewer(
            file = file,
            modifier = Modifier.fillMaxSize(),
            onTap = { chromeVisible = !chromeVisible },
        )
    }
}
