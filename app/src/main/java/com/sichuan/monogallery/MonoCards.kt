package com.sichuan.monogallery

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

// ---------- Shared content ----------

/**
 * Folder card content: a title row (icon + name) on top, a divider in the middle, and a content
 * preview (item count) below. Tapping the name renames the folder; tapping the preview opens it;
 * long-pressing any region enters multi-selection (same behavior as the file card).
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
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
            ) {
                Text(
                    text = folder.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorTextPrimary,
                    maxLines = 1,
                    softWrap = false,
                )
            }
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
 * File card content: the name plus extension (shown together) on top, a divider in the middle,
 * and a body preview below. Tapping the name renames the file; tapping the body opens it;
 * long-pressing any region enters multi-selection.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileCardContent(
    file: MonoFile,
    thumbnailFile: File?,
    onNameClick: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onNameClick, onLongClick = onLongClick)
                .padding(vertical = 4.dp),
        ) {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                Text(
                    text = file.fullName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorTextPrimary,
                    maxLines = 1,
                    softWrap = false,
                )
            }
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
                FileType.IMAGE -> ImageThumbnail(
                    file = thumbnailFile,
                    modifier = Modifier.fillMaxSize(),
                )
                FileType.VIDEO -> VideoThumbnail(
                    file = thumbnailFile,
                    modifier = Modifier.fillMaxSize(),
                )
                FileType.PDF -> PdfCoverThumbnail(
                    file = thumbnailFile,
                    modifier = Modifier.fillMaxSize(),
                )
                FileType.AUDIO -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = "音频",
                        tint = ColorAccent,
                        modifier = Modifier.size(32.dp),
                    )
                }
                FileType.OTHER -> Unit
            }
        }
    }
}

/** Image thumbnail: cropped to fill the card preview area while preserving the aspect ratio. */
@Composable
private fun ImageThumbnail(file: File?, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val target = with(density) { 256.dp.roundToPx() }
    val result = rememberImageResult(file, target, target)
    Box(modifier = modifier.background(ColorSearchField)) {
        (result as? ImageResult.Success)?.let {
            Image(
                bitmap = it.bitmap,
                contentDescription = "图片缩略图",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
            )
        }
    }
}

/** PDF card preview: renders the first page (the cover), cropped to fill the card preview area. */
@Composable
private fun PdfCoverThumbnail(file: File?, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val target = with(density) { 256.dp.roundToPx() }
    val result = rememberPdfCover(file, target, target)
    Box(modifier = modifier.background(ColorSearchField)) {
        (result as? ImageResult.Success)?.let {
            Image(
                bitmap = it.bitmap,
                contentDescription = "PDF 封面",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
            )
        }
    }
}

/** Video card preview: a frame from the video with a play badge in the center, cropped to fill the card preview area. */
@Composable
private fun VideoThumbnail(file: File?, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val target = with(density) { 256.dp.roundToPx() }
    val result = rememberVideoFrame(file, target, target)
    Box(modifier = modifier.background(ColorSearchField)) {
        (result as? ImageResult.Success)?.let {
            Image(
                bitmap = it.bitmap,
                contentDescription = "视频缩略图",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
            )
        }
        // The play badge stays visible while the frame loads so the card type is still recognizable
        Icon(
            imageVector = Icons.Filled.PlayCircle,
            contentDescription = "视频",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .size(40.dp),
        )
    }
}

// ---------- Selectable folder card (shared by the home root directory and folders) ----------

/**
 * Folder card supporting multi-selection. Tapping the name region opens the context menu
 * (rename, properties, share) when not selecting, and toggles selection while selecting.
 * Highlights the selected state and shows a check icon when selected.
 */
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
    onProperties: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
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
                onNameClick = { if (isSelecting) onClick() else showMenu = true },
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
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("重命名") },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                onClick = {
                    showMenu = false
                    showRename = true
                },
            )
            DropdownMenuItem(
                text = { Text("属性") },
                leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                onClick = {
                    showMenu = false
                    onProperties()
                },
            )
            DropdownMenuItem(
                text = { Text("分享") },
                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                onClick = {
                    showMenu = false
                    onShare()
                },
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

// ---------- Selectable file card (shared by the home root directory and folders) ----------

/**
 * File card supporting multi-selection. Tapping the name region opens the context menu
 * (open with, rename, properties, share, plus copy-to-clipboard for text files) when not
 * selecting, and toggles selection while selecting. Highlights the selected state and shows
 * a check icon.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectableFileCard(
    file: MonoFile,
    thumbnailFile: File?,
    selected: Boolean,
    isSelecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onOpenWith: () -> Unit,
    onRename: (String) -> Unit,
    onProperties: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
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
                thumbnailFile = thumbnailFile,
                onNameClick = { if (isSelecting) onClick() else showMenu = true },
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
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("重命名") },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                onClick = {
                    showMenu = false
                    showRename = true
                },
            )
            DropdownMenuItem(
                text = { Text("属性") },
                leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                onClick = {
                    showMenu = false
                    onProperties()
                },
            )
            DropdownMenuItem(
                text = { Text("打开方式") },
                leadingIcon = { Icon(Icons.Filled.OpenWith, contentDescription = null) },
                onClick = {
                    showMenu = false
                    onOpenWith()
                },
            )
            DropdownMenuItem(
                text = { Text("分享") },
                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                onClick = {
                    showMenu = false
                    onShare()
                },
            )
            if (file.type == FileType.TEXT) {
                CopyClipboardMenuItem(
                    text = file.content,
                    onCopied = { showMenu = false },
                )
            }
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

// ---------- Folder picker: tapping enters the target folder ----------

/**
 * Folder card used by a folder picker: tapping anywhere enters the target folder. It has no
 * selection mode and no context menu.
 */
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

// ---------- Dialogs ----------

/**
 * Rename dialog for a folder: edits the display name only (the folder has no extension) and
 * confirms with the trimmed, non-blank value.
 */
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

/** File rename: the extension can be edited; a second confirmation is requested when the extension changes. */
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

/** Delete confirmation dialog (shared by folders and files). */
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

/** Copy-to-clipboard menu item: shown only for text files, with a background color that distinguishes it from the generic menu items. */
@Composable
private fun CopyClipboardMenuItem(
    text: String,
    onCopied: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorSelected)
            .clickable {
                clipboardManager.setText(AnnotatedString(text))
                Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                onCopied()
            }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = null,
                tint = ColorTextPrimary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "复制到剪切板",
                fontSize = 16.sp,
                color = ColorTextPrimary,
            )
        }
    }
}
