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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Text
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
 * preview ([countText]) below. Tapping the name renames the folder; tapping the preview opens it;
 * long-pressing any region enters multi-selection (same behavior as the file card).
 *
 * The name and the preview text are passed in instead of being read from a [Folder], so the same
 * layout also serves a folder that has no library entry of its own (see 本地图片).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderCardContent(
    name: String,
    countText: String,
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
                    text = name,
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
                text = countText,
                fontSize = 12.sp,
                color = ColorTextSecondary,
            )
        }
    }
}

/**
 * File card content: the full name (name plus extension, shown together) on top, a divider in the
 * middle, and a body preview below, chosen by [type] (for a text file the preview is [content]).
 * Tapping the name renames the file; tapping the body opens it; long-pressing any region enters
 * multi-selection.
 *
 * The fields are passed in instead of being read from a [MonoFile], so the same layout also serves
 * a file that has no library entry of its own (see 本地图片).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileCardContent(
    name: String,
    type: FileType,
    content: String,
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
                    text = name,
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
            when (type) {
                FileType.TEXT -> if (content.isNotBlank()) {
                    Text(
                        text = content,
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

/**
 * Card preview area shared by the image, video and PDF thumbnails: decodes the thumbnail through
 * [load] at the card preview resolution, crops it to fill the box, and draws [overlay] on top.
 */
@Composable
private fun ThumbnailBox(
    file: File?,
    contentDescription: String,
    modifier: Modifier,
    load: @Composable (File?, Int, Int) -> ImageResult,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val target = with(LocalDensity.current) { 256.dp.roundToPx() }
    val result = load(file, target, target)

    Box(modifier = modifier.background(ColorSearchField)) {
        (result as? ImageResult.Success)?.let {
            Image(
                bitmap = it.bitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
            )
        }
        overlay()
    }
}

/** Image thumbnail: cropped to fill the card preview area while preserving the aspect ratio. */
@Composable
private fun ImageThumbnail(file: File?, modifier: Modifier = Modifier) {
    ThumbnailBox(
        file = file,
        contentDescription = "图片缩略图",
        modifier = modifier,
        load = { f, width, height -> rememberImageResult(f, width, height) },
    )
}

/** PDF card preview: renders the first page (the cover), cropped to fill the card preview area. */
@Composable
private fun PdfCoverThumbnail(file: File?, modifier: Modifier = Modifier) {
    ThumbnailBox(
        file = file,
        contentDescription = "PDF 封面",
        modifier = modifier,
        load = { f, width, height -> rememberPdfCover(f, width, height) },
    )
}

/** Video card preview: a frame from the video with a play badge in the center, cropped to fill the card preview area. */
@Composable
private fun VideoThumbnail(file: File?, modifier: Modifier = Modifier) {
    ThumbnailBox(
        file = file,
        contentDescription = "视频缩略图",
        modifier = modifier,
        load = { f, width, height -> rememberVideoFrame(f, width, height) },
    ) {
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

// ---------- Selectable cards (shared by the home root directory and folders) ----------

/**
 * Card scaffolding shared by the selectable folder and file cards: a square card highlighted
 * while [selected], a check badge in the top-right corner, and the context menu whose entries
 * are [menuItems].
 */
@Composable
private fun SelectableCard(
    selected: Boolean,
    showMenu: Boolean,
    onDismissMenu: () -> Unit,
    menuItems: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
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
            content()
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
            onDismissRequest = onDismissMenu,
            content = menuItems,
        )
    }
}

/** "Rename" entry of a card's context menu. */
@Composable
private fun RenameMenuItem(onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text("重命名") },
        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
        onClick = onClick,
    )
}

/** "Properties" entry of a card's context menu. */
@Composable
private fun PropertiesMenuItem(onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text("属性") },
        leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
        onClick = onClick,
    )
}

/** "Share" entry of a card's context menu. */
@Composable
private fun ShareMenuItem(onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text("分享") },
        leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
        onClick = onClick,
    )
}

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

    SelectableCard(
        selected = selected,
        showMenu = showMenu,
        onDismissMenu = { showMenu = false },
        menuItems = {
            RenameMenuItem {
                showMenu = false
                showRename = true
            }
            PropertiesMenuItem {
                showMenu = false
                onProperties()
            }
            ShareMenuItem {
                showMenu = false
                onShare()
            }
        },
        modifier = modifier,
    ) {
        FolderCardContent(
            name = folder.name,
            countText = "$itemCount 项",
            onNameClick = { if (isSelecting) onClick() else showMenu = true },
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }

    if (showRename) {
        NameInputDialog(
            title = "重命名",
            label = "名称",
            initialValue = folder.name,
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

    SelectableCard(
        selected = selected,
        showMenu = showMenu,
        onDismissMenu = { showMenu = false },
        menuItems = {
            RenameMenuItem {
                showMenu = false
                showRename = true
            }
            PropertiesMenuItem {
                showMenu = false
                onProperties()
            }
            DropdownMenuItem(
                text = { Text("打开方式") },
                leadingIcon = { Icon(Icons.Filled.OpenWith, contentDescription = null) },
                onClick = {
                    showMenu = false
                    onOpenWith()
                },
            )
            ShareMenuItem {
                showMenu = false
                onShare()
            }
            if (file.type == FileType.TEXT) {
                CopyClipboardMenuItem(
                    text = file.content,
                    onCopied = { showMenu = false },
                )
            }
        },
        modifier = modifier,
    ) {
        FileCardContent(
            name = file.fullName,
            type = file.type,
            content = file.content,
            thumbnailFile = thumbnailFile,
            onNameClick = { if (isSelecting) onClick() else showMenu = true },
            onClick = onClick,
            onLongClick = onLongClick,
        )
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
@Composable
fun PickerFolderCard(
    folder: Folder,
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = PickerFolderCard(
    name = folder.name,
    countText = "$itemCount 项",
    onClick = onClick,
    modifier = modifier,
)

/**
 * [PickerFolderCard] for a folder that has no library entry of its own: the same card driven by an
 * explicit [name] and [countText] (see 本地图片).
 *
 * [onProperties] is what separates it from the folder-picker variant: when it is given the card
 * behaves like a library folder card, where tapping the name region opens a context menu holding
 * "属性" instead of entering the folder. Pass null (the default) for the picker, which needs a plain
 * card that enters on every tap.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PickerFolderCard(
    name: String,
    countText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onProperties: (() -> Unit)? = null,
) {
    if (onProperties == null) {
        PlainFolderCard(name = name, countText = countText, onClick = onClick, modifier = modifier)
        return
    }

    var showMenu by remember { mutableStateOf(false) }

    SelectableCard(
        selected = false,
        showMenu = showMenu,
        onDismissMenu = { showMenu = false },
        menuItems = {
            PropertiesMenuItem {
                showMenu = false
                onProperties()
            }
        },
        modifier = modifier,
    ) {
        FolderCardContent(
            name = name,
            countText = countText,
            onNameClick = { showMenu = true },
            onClick = onClick,
            onLongClick = {},
        )
    }
}

/** The plain, menu-less folder card: every tap enters the folder (folder-picker behaviour). */
@Composable
private fun PlainFolderCard(
    name: String,
    countText: String,
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
                name = name,
                countText = countText,
                onNameClick = onClick,
                onClick = onClick,
                onLongClick = {},
            )
        }
    }
}

// ---------- Local images: cards for files that live outside the library ----------

/**
 * Image card for a file outside the library (the 本地图片 page): the shared file-card layout driven
 * by an on-disk [file] instead of a library entry.
 *
 * The image itself is never referenced by the library, but the card behaves exactly like a library
 * image card: the same selection highlight and check badge, and the same context menu reached by
 * tapping the name region — rename, properties, open with, share. There is one omission, the
 * "复制到剪切板" entry: images are not text.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocalImageCard(
    file: File,
    selected: Boolean,
    isSelecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRename: (String) -> Unit,
    onProperties: () -> Unit,
    onOpenWith: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }

    SelectableCard(
        selected = selected,
        showMenu = showMenu,
        onDismissMenu = { showMenu = false },
        menuItems = {
            RenameMenuItem {
                showMenu = false
                showRename = true
            }
            PropertiesMenuItem {
                showMenu = false
                onProperties()
            }
            DropdownMenuItem(
                text = { Text("打开方式") },
                leadingIcon = { Icon(Icons.Filled.OpenWith, contentDescription = null) },
                onClick = {
                    showMenu = false
                    onOpenWith()
                },
            )
            ShareMenuItem {
                showMenu = false
                onShare()
            }
        },
        modifier = modifier,
    ) {
        FileCardContent(
            name = file.name,
            type = FileType.IMAGE,
            content = "",
            thumbnailFile = file,
            // While selecting, a tap on the name toggles the selection like on a library card
            onNameClick = { if (isSelecting) onClick() else showMenu = true },
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }

    if (showRename) {
        RenameFileDialog(
            currentFullName = file.name,
            currentExtension = file.extension,
            onConfirm = { newFullName ->
                showRename = false
                onRename(newFullName)
            },
            onDismiss = { showRename = false },
        )
    }
}

// ---------- Dialogs ----------

/** File rename: the extension can be edited; a second confirmation is requested when the extension changes. */
@Composable
private fun RenameFileDialog(
    currentFullName: String,
    currentExtension: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var pendingConfirm by remember { mutableStateOf<String?>(null) }

    NameInputDialog(
        title = "重命名",
        label = "名称",
        initialValue = currentFullName,
        onConfirm = { newName ->
            val (_, ext) = splitFullName(newName)
            if (ext.lowercase() != currentExtension.lowercase()) {
                pendingConfirm = newName
            } else {
                onConfirm(newName)
            }
        },
        onDismiss = onDismiss,
    )

    pendingConfirm?.let { newName ->
        ConfirmDialog(
            title = "修改扩展名",
            message = "扩展名已被修改，可能导致文件无法正常打开。确定继续吗？",
            onConfirm = {
                pendingConfirm = null
                onConfirm(newName)
            },
            onDismiss = { pendingConfirm = null },
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
    ConfirmDialog(
        title = title,
        message = message,
        confirmText = "删除",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
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
