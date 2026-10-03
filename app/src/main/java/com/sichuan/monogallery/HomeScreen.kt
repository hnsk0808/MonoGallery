package com.sichuan.monogallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 首页：展示根目录文件夹与文件，支持多选（添加到 / 删除 / 压缩 / 复制到剪切板）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    library: MonoLibrary,
    onOpenFolder: (Folder) -> Unit,
    onOpenFile: (MonoFile) -> Unit,
    onNewFolder: () -> Unit,
    onNewFile: () -> Unit,
    onRefresh: () -> Unit,
    onAddToFolder: (Set<Long>) -> Unit,
    onCompress: (Set<Long>) -> Unit,
    onOpenFolderProperties: (Long) -> Unit,
    onOpenFileProperties: (Long) -> Unit,
) {
    val selection = remember { FileSelectionState() }
    val context = LocalContext.current

    BackHandler(enabled = selection.mode) { selection.exit() }

    Scaffold(
        containerColor = ColorBackground,
        topBar = { HomeTopBar(onNewFolder = onNewFolder, onNewFile = onNewFile, onRefresh = onRefresh) },
        bottomBar = {
            if (selection.mode) {
                SelectionBottomBar(
                    library = library,
                    selection = selection,
                    onAddTo = { onAddToFolder(it) },
                    onCompress = { onCompress(it) },
                    onDelete = { fileIds, folderIds ->
                        library.deleteItems(fileIds, folderIds)
                        selection.exit()
                    },
                )
            }
        },
    ) { innerPadding ->
        if (library.rootFolders().isEmpty() && library.rootFiles().isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "点击 + 新建文件夹或文件", color = ColorTextSecondary)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(library.rootFolders(), key = { it.id }) { folder ->
                    SelectableFolderCard(
                        folder = folder,
                        itemCount = library.itemCount(folder.id),
                        selected = folder.id in selection.ids,
                        isSelecting = selection.mode,
                        onClick = {
                            if (selection.mode) selection.toggle(folder.id) else onOpenFolder(folder)
                        },
                        onLongClick = { selection.enter(folder.id) },
                        onRename = { library.renameFolder(folder.id, it) },
                        onProperties = { onOpenFolderProperties(folder.id) },
                        onShare = { library.folderShareZip(folder.id)?.let { shareZip(context, it) } },
                    )
                }
                items(library.rootFiles(), key = { it.id }) { file ->
                    SelectableFileCard(
                        file = file,
                        thumbnailFile = library.fileOnDisk(file.id),
                        selected = file.id in selection.ids,
                        isSelecting = selection.mode,
                        onClick = {
                            if (selection.mode) selection.toggle(file.id) else onOpenFile(file)
                        },
                        onLongClick = { selection.enter(file.id) },
                        onRename = { library.renameFile(file.id, it) },
                        onProperties = { onOpenFileProperties(file.id) },
                        onShare = { library.fileOnDisk(file.id)?.let { shareFile(context, it, file.extension) } },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    onNewFolder: () -> Unit,
    onNewFile: () -> Unit,
    onRefresh: () -> Unit,
) {
    var query by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Mono",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ColorTextPrimary,
        )

        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .padding(horizontal = 12.dp),
            placeholder = {
                Text(text = "搜索", fontSize = 14.sp, color = ColorTextSecondary)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "搜索",
                    tint = ColorTextSecondary,
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = ColorSearchField,
                unfocusedContainerColor = ColorSearchField,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = ColorTextPrimary,
            ),
        )

        AddMenu(onNewFolder = onNewFolder, onNewFile = onNewFile)

        MoreMenu(onRefresh = onRefresh)
    }
}

/** 加号 -> 展开「新建文件夹 / 新建文件」，首页与文件夹内容页共用，保持一致性。 */
@Composable
fun AddMenu(
    onNewFolder: () -> Unit,
    onNewFile: () -> Unit,
) {
    var addExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { addExpanded = true }) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "新建",
                tint = ColorTextPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
        DropdownMenu(
            expanded = addExpanded,
            onDismissRequest = { addExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("新建文件夹") },
                leadingIcon = {
                    Icon(Icons.Filled.CreateNewFolder, contentDescription = null)
                },
                onClick = {
                    addExpanded = false
                    onNewFolder()
                },
            )
            DropdownMenuItem(
                text = { Text("新建TXT文件") },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null)
                },
                onClick = {
                    addExpanded = false
                    onNewFile()
                },
            )
        }
    }
}

/** 更多操作菜单：首页与文件夹内容页共用，保持一致性。 */
@Composable
fun MoreMenu(
    onRefresh: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuExpanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "更多",
                tint = ColorTextPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("刷新") },
                leadingIcon = {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                },
                onClick = {
                    menuExpanded = false
                    onRefresh()
                },
            )
        }
    }
}
