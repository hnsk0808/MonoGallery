package com.sichuan.monogallery

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * File viewer screen: dispatches by type. Video, image, and PDF fill the screen through the
 * shared [FullscreenPreviewScreen] (the title bar floats on top and fades together with the
 * viewer's own controls); text uses the text editor ([TextEditor]), audio uses [AudioPlayer],
 * and other types show an unsupported notice in the standard layout.
 */
@Composable
fun FileViewScreen(
    library: MonoLibrary,
    fileId: Long,
    onBack: () -> Unit,
) {
    val file = library.file(fileId)

    when (file?.type) {
        // Fullscreen viewers: the overlay title bar shares the viewer's control-surface visibility
        FileType.VIDEO, FileType.IMAGE, FileType.PDF -> {
            var chromeVisible by remember { mutableStateOf(true) }
            val toggleChrome = { visible: Boolean -> chromeVisible = visible }

            FullscreenPreviewScreen(
                title = file.fullName,
                onBack = onBack,
                chromeVisible = chromeVisible,
                onChromeVisibleChange = toggleChrome,
            ) {
                val onDisk = library.fileOnDisk(fileId)
                when (file.type) {
                    FileType.VIDEO -> VideoPlayer(
                        file = onDisk,
                        onBack = onBack,
                        controlsVisible = chromeVisible,
                        onControlsVisibleChange = toggleChrome,
                        modifier = Modifier.fillMaxSize(),
                    )

                    FileType.IMAGE -> ImageViewer(
                        file = onDisk,
                        modifier = Modifier.fillMaxSize(),
                        onTap = { chromeVisible = !chromeVisible },
                    )

                    else -> PdfViewer(
                        file = onDisk,
                        modifier = Modifier.fillMaxSize(),
                        chromeVisible = chromeVisible,
                        onChromeVisibleChange = toggleChrome,
                    )
                }
            }
        }

        else -> StandardPreview(
            library = library,
            fileId = fileId,
            file = file,
            onBack = onBack,
        )
    }
}

/** Standard layout for text, audio, and other types: a regular title bar on top that takes layout space, with the content below. */
@Composable
private fun StandardPreview(
    library: MonoLibrary,
    fileId: Long,
    file: MonoFile?,
    onBack: () -> Unit,
) {
    MonoScaffold(
        title = {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                Text(
                    text = file?.fullName ?: "",
                    maxLines = 1,
                    softWrap = false,
                )
            }
        },
        onBack = onBack,
    ) { innerPadding ->
        when (file?.type) {
            FileType.TEXT -> TextEditor(
                library = library,
                fileId = fileId,
                initialContent = file.content,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            FileType.AUDIO -> AudioPlayer(
                file = library.fileOnDisk(fileId),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            FileType.OTHER -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                Text("暂不支持预览此类型文件", color = ColorTextSecondary)
            }

            // IMAGE / VIDEO / PDF are handled by FullscreenPreviewScreen and never reach this layout
            FileType.IMAGE, FileType.VIDEO, FileType.PDF, null -> Unit
        }
    }
}
