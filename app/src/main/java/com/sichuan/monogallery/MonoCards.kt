package com.sichuan.monogallery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- 共享内容 ----------

@Composable
private fun FolderCardContent(folder: Folder, itemCount: Int) {
    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = ColorFolder,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = folder.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = ColorTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "$itemCount 项",
            fontSize = 12.sp,
            color = ColorTextSecondary,
        )
    }
}

@Composable
private fun FileCardContent(file: MonoFile) {
    when (file.type) {
        FileType.TEXT -> TextFileCardContent(file)
    }
}

@Composable
private fun TextFileCardContent(file: MonoFile) {
    Column(Modifier.padding(16.dp)) {
        Text(
            text = file.name,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = ColorTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (file.content.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = file.content,
                fontSize = 12.sp,
                color = ColorTextSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ---------- 首页：文件夹卡片（打开 + 重命名/删除） ----------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderCard(
    folder: Folder,
    itemCount: Int,
    onOpen: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(onClick = onOpen, onLongClick = { menuExpanded = true }),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ColorCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            FolderCardContent(folder, itemCount)
        }

        CardContextMenu(
            expanded = menuExpanded,
            onDismiss = { menuExpanded = false },
            onRename = {
                menuExpanded = false
                showRename = true
            },
            onDelete = {
                menuExpanded = false
                showDeleteConfirm = true
            },
        )

        if (showRename) {
            RenameDialog(
                currentName = folder.name,
                onConfirm = {
                    showRename = false
                    onRename(it)
                },
                onDismiss = { showRename = false },
            )
        }

        if (showDeleteConfirm) {
            ConfirmDeleteDialog(
                title = "删除文件夹",
                message = "确定删除「${folder.name}」吗？文件夹内的文件也会一并删除。",
                onConfirm = {
                    showDeleteConfirm = false
                    onDelete()
                },
                onDismiss = { showDeleteConfirm = false },
            )
        }
    }
}

// ---------- 可多选的文件卡片（首页根目录 & 文件夹内共用） ----------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectableFileCard(
    file: MonoFile,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (selected) ColorSelected else ColorCard,
            ),
            border = if (selected) BorderStroke(2.dp, ColorAccent) else null,
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            FileCardContent(file)
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "已选择",
                tint = ColorAccent,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(22.dp),
            )
        }
    }
}

// ---------- 文件夹选择器：点击即选目标文件夹 ----------

@Composable
fun PickerFolderCard(
    folder: Folder,
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ColorCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            FolderCardContent(folder, itemCount)
        }
    }
}

// ---------- 长按菜单 & 重命名弹窗 ----------

@Composable
private fun CardContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("重命名") }, onClick = onRename)
        DropdownMenuItem(text = { Text("删除") }, onClick = onDelete)
    }
}

@Composable
private fun RenameDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember(currentName) { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名") },
        text = {
            TextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("名称") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value.trim()) },
                enabled = value.isNotBlank(),
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

/** 删除确认弹窗（文件夹 / 文件通用）。 */
@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("删除") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
