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

/** Text viewer/editor: editable with autosave (writes back 400 ms after typing stops, and saves immediately on leaving). */
@Composable
fun TextEditor(
    library: MonoLibrary,
    fileId: Long,
    initialContent: String,
    modifier: Modifier = Modifier,
) {
    var text by remember(fileId) { mutableStateOf(initialContent) }
    val latestText by rememberUpdatedState(text)

    // Debounced save: write back to local storage 400 ms after typing stops
    LaunchedEffect(text) {
        delay(400)
        library.updateFileContent(fileId, text)
    }
    // Save immediately when leaving the screen so the last input is not lost
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
