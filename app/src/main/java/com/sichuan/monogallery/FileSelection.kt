package com.sichuan.monogallery

import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 多选模式状态：进入 / 切换选中 / 退出。 */
class FileSelectionState {
    var mode by mutableStateOf(false)
        private set
    var ids by mutableStateOf(setOf<Long>())
        private set

    fun enter(id: Long) {
        mode = true
        ids = ids + id
    }

    fun toggle(id: Long) {
        ids = if (id in ids) ids - id else ids + id
        if (ids.isEmpty()) mode = false
    }

    fun exit() {
        mode = false
        ids = emptySet()
    }
}

/** 多选底部操作条：添加到 / 删除 / 复制到剪切板。 */
@Composable
fun SelectionBottomBar(
    library: MonoLibrary,
    selection: FileSelectionState,
    onAddTo: (Set<Long>) -> Unit,
    onDelete: (Set<Long>) -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(color = ColorCard, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "已选 ${selection.ids.size}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ColorTextPrimary,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { onAddTo(selection.ids) }) { Text("添加到") }
            TextButton(onClick = { showDeleteConfirm = true }) { Text("删除") }
            TextButton(onClick = {
                val text = selection.ids
                    .mapNotNull { library.file(it)?.content }
                    .joinToString("\n\n")
                clipboardManager.setText(AnnotatedString(text))
                Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
            }) {
                Text("复制到剪切板")
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            title = "删除文件",
            message = "确定删除选中的 ${selection.ids.size} 个文件吗？此操作不可恢复。",
            onConfirm = {
                showDeleteConfirm = false
                onDelete(selection.ids)
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}
