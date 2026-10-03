package com.sichuan.monogallery

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** 文件查看页：按类型分发到文字编辑（[TextEditor]）/ 图片预览（[ImageViewer]）/ 不支持提示。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewScreen(
    library: MonoLibrary,
    fileId: Long,
    onBack: () -> Unit,
) {
    val file = library.file(fileId)

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
                initialContent = file?.content ?: "",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            FileType.IMAGE -> ImageViewer(
                file = library.fileOnDisk(fileId),
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
                contentAlignment = Alignment.Center,
            ) {
                Text("暂不支持预览此类型文件", color = ColorTextSecondary)
            }

            null -> Unit
        }
    }
}
