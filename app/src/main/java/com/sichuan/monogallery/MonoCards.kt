package com.sichuan.monogallery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material3.HorizontalDivider
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

/**
 * 文件夹卡片内容：上方为标题行（图标 + 名字），中间一条分隔线，下方为内容预览（项目数量）。
 * 点击名字区域改名，点击预览区域打开；长按任意区域进入多选（与文件卡片一致）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderCardContent(
    folder: Folder,
    itemCount: Int,
    onNameClick: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onNameClick, onLongClick = onLongClick)
                .padding(vertical = 4.dp),
        ) {
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
        HorizontalDivider(color = ColorSearchField, thickness = 1.dp)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        ) {
            Text(
                text = "$itemCount 项",
                fontSize = 12.sp,
                color = ColorTextSecondary,
            )
        }
    }
}

/**
 * 文件卡片内容：上方为名字 + 扩展名（放在一起显示），中间一条分隔线，下方为正文预览。
 * 点击名字区域改名，点击正文区域打开文件；长按任意区域进入多选。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileCardContent(
    file: MonoFile,
    onNameClick: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = file.fullName,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = ColorTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onNameClick, onLongClick = onLongClick)
                .padding(vertical = 4.dp),
        )
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = ColorSearchField, thickness = 1.dp)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        ) {
            when (file.type) {
                FileType.TEXT -> if (file.content.isNotBlank()) {
                    Text(
                        text = file.content,
                        fontSize = 12.sp,
                        color = ColorTextSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FileType.OTHER -> Unit
            }
        }
    }
}

// ---------- 可多选的文件夹卡片（首页根目录 & 文件夹内共用） ----------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectableFolderCard(
    folder: Folder,
    itemCount: Int,
    selected: Boolean,
    isSelecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRename by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (selected) ColorSelected else ColorCard,
            ),
            border = if (selected) BorderStroke(2.dp, ColorAccent) else null,
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            FolderCardContent(
                folder = folder,
                itemCount = itemCount,
                onNameClick = { if (isSelecting) onClick() else showRename = true },
                onClick = onClick,
                onLongClick = onLongClick,
            )
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
}

// ---------- 可多选的文件卡片（首页根目录 & 文件夹内共用） ----------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectableFileCard(
    file: MonoFile,
    selected: Boolean,
    isSelecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRename by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (selected) ColorSelected else ColorCard,
            ),
            border = if (selected) BorderStroke(2.dp, ColorAccent) else null,
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            FileCardContent(
                file = file,
                onNameClick = { if (isSelecting) onClick() else showRename = true },
                onClick = onClick,
                onLongClick = onLongClick,
            )
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

    if (showRename) {
        RenameFileDialog(
            currentFullName = file.fullName,
            currentExtension = file.extension,
            onConfirm = { newFullName ->
                showRename = false
                onRename(newFullName)
            },
            onDismiss = { showRename = false },
        )
    }
}

// ---------- 文件夹选择器：点击即进入目标文件夹 ----------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PickerFolderCard(
    folder: Folder,
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ColorCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            FolderCardContent(
                folder = folder,
                itemCount = itemCount,
                onNameClick = onClick,
                onClick = onClick,
                onLongClick = {},
            )
        }
    }
}

// ---------- 弹窗 ----------

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

/** 文件重命名：可修改扩展名，扩展名被修改时二次确认。 */
@Composable
private fun RenameFileDialog(
    currentFullName: String,
    currentExtension: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember(currentFullName) { mutableStateOf(currentFullName) }
    var pendingConfirm by remember { mutableStateOf<String?>(null) }

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
                onClick = {
                    val trimmed = value.trim()
                    if (trimmed.isBlank()) return@TextButton
                    val (_, ext) = splitFullName(trimmed)
                    if (ext.lowercase() != currentExtension.lowercase()) {
                        pendingConfirm = trimmed
                    } else {
                        onConfirm(trimmed)
                    }
                },
                enabled = value.isNotBlank(),
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )

    pendingConfirm?.let { newName ->
        AlertDialog(
            onDismissRequest = { pendingConfirm = null },
            title = { Text("修改扩展名") },
            text = { Text("扩展名已被修改，可能导致文件无法正常打开。确定继续吗？") },
            confirmButton = {
                TextButton(onClick = {
                    pendingConfirm = null
                    onConfirm(newName)
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirm = null }) { Text("取消") }
            },
        )
    }
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
