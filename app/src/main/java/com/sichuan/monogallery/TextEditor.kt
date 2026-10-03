package com.sichuan.monogallery

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** 文字查看 / 编辑：可编辑并自动保存（停止输入 400ms 后写回，离开时立即保存）。 */
@Composable
fun TextEditor(
    library: MonoLibrary,
    fileId: Long,
    initialContent: String,
    modifier: Modifier = Modifier,
) {
    var text by remember(fileId) { mutableStateOf(initialContent) }
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

    TextField(
        value = text,
        onValueChange = { text = it },
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        textStyle = TextStyle(fontSize = 15.sp, lineHeight = 24.sp, color = ColorTextPrimary),
        placeholder = { Text("开始输入……", color = ColorTextSecondary) },
    )
}
