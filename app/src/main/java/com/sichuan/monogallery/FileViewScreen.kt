package com.sichuan.monogallery

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * File viewer screen: dispatches by type. Video, image, and PDF use the shared
 * [FullscreenPreviewScreen] (content fills the screen, the title bar floats on top and fades
 * together with the viewer's own controls); text uses the text editor ([TextEditor]), audio
 * uses [AudioPlayer], and other types show an unsupported notice in a standard scaffold.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewScreen(
    library: MonoLibrary,
    fileId: Long,
    onBack: () -> Unit,
) {
    val file = library.file(fileId)

    when (file?.type) {
        FileType.VIDEO -> {
            // The overlay title bar shares the video player's control-surface visibility
            var chromeVisible by remember { mutableStateOf(true) }
            FullscreenPreviewScreen(
                title = file.fullName,
                onBack = onBack,
                chromeVisible = chromeVisible,
                onChromeVisibleChange = { chromeVisible = it },
            ) {
                VideoPlayer(
                    file = library.fileOnDisk(fileId),
                    onBack = onBack,
                    controlsVisible = chromeVisible,
                    onControlsVisibleChange = { chromeVisible = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        FileType.IMAGE -> {
            var chromeVisible by remember { mutableStateOf(true) }
            FullscreenPreviewScreen(
                title = file.fullName,
                onBack = onBack,
                chromeVisible = chromeVisible,
                onChromeVisibleChange = { chromeVisible = it },
            ) {
                ImageViewer(
                    file = library.fileOnDisk(fileId),
                    modifier = Modifier.fillMaxSize(),
                    onTap = { chromeVisible = !chromeVisible },
                )
            }
        }

        FileType.PDF -> {
            var chromeVisible by remember { mutableStateOf(true) }
            FullscreenPreviewScreen(
                title = file.fullName,
                onBack = onBack,
                chromeVisible = chromeVisible,
                onChromeVisibleChange = { chromeVisible = it },
            ) {
                PdfViewer(
                    file = library.fileOnDisk(fileId),
                    modifier = Modifier.fillMaxSize(),
                    chromeVisible = chromeVisible,
                    onChromeVisibleChange = { chromeVisible = it },
                )
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StandardPreview(
    library: MonoLibrary,
    fileId: Long,
    file: MonoFile?,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = ColorBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        Text(
                            text = file?.fullName ?: "",
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
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
