package com.sichuan.monogallery

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Multi-selection mode state: enter, toggle selection, and exit. */
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

/**
 * Bottom action bar shown while items are selected: the "已选 n" counter with the same three actions
 * everywhere — "添加到", "删除", "压缩". The library folder page and the 本地图片 page share this bar;
 * only the wording of the delete confirmation differs ([deleteTitle] / [deleteMessage]), because one
 * deletes library entries and the other deletes pictures that live on the device.
 */
@Composable
fun SelectionBottomBar(
    count: Int,
    onAddTo: () -> Unit,
    onCompress: () -> Unit,
    onDelete: () -> Unit,
    deleteTitle: String = "删除项目",
    deleteMessage: String = "确定删除选中的 $count 个项目吗？文件夹及其内容会一并删除，此操作不可恢复。",
) {
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
                text = "已选 $count",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ColorTextPrimary,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(Modifier.weight(1f))
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                TextButton(onClick = onAddTo) { Text("添加到") }
                TextButton(onClick = { showDeleteConfirm = true }) { Text("删除") }
                TextButton(onClick = onCompress, enabled = count > 0) { Text("压缩") }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            title = deleteTitle,
            message = deleteMessage,
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}
