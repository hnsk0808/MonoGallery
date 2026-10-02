package com.sichuan.monogallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** 文件查看 / 编辑页：文字类型可编辑并自动保存。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewScreen(
    library: MonoLibrary,
    fileId: Long,
    onBack: () -> Unit,
) {
    val file = library.file(fileId)
    var text by remember(file?.id) { mutableStateOf(file?.content ?: "") }
    val latestText by rememberUpdatedState(text)

    // 防抖保存：停止输入 400ms 后写回本地
    LaunchedEffect(text) {
        delay(400)
        library.updateFileContent(fileId, text)
    }
    // 离开界面时立即保存，避免丢失最后一段输入
    DisposableEffect(fileId) {
        onDispose { library.updateFileContent(fileId, latestText) }
    }

    Scaffold(
        containerColor = ColorBackground,
        topBar = {
            TopAppBar(
                title = { Text(file?.fullName ?: "") },
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
            FileType.TEXT -> TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                textStyle = TextStyle(fontSize = 15.sp, lineHeight = 24.sp, color = ColorTextPrimary),
                placeholder = { Text("开始输入……", color = ColorTextSecondary) },
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
