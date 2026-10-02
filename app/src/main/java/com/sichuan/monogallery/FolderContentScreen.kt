package com.sichuan.monogallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 文件夹内容页：展示子文件夹与文件，支持与首页一致的多选操作与「+」新建菜单。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderContentScreen(
    library: MonoLibrary,
    folderId: Long,
    onBack: () -> Unit,
    onOpenFolder: (Folder) -> Unit,
    onOpenFile: (MonoFile) -> Unit,
    onNewFolder: () -> Unit,
    onNewFile: () -> Unit,
    onAddToFolder: (Set<Long>) -> Unit,
    onCompress: (Set<Long>) -> Unit,
) {
    val folder = library.folder(folderId)
    val subfolders = library.subfoldersOf(folderId)
    val files = library.filesIn(folderId)
    val selection = remember { FileSelectionState() }

    BackHandler(enabled = selection.mode) { selection.exit() }

    Scaffold(
        containerColor = ColorBackground,
        topBar = {
            TopAppBar(
                title = { Text(folder?.name ?: "文件夹") },
                navigationIcon = {
                    IconButton(onClick = { if (selection.mode) selection.exit() else onBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
                    AddMenu(onNewFolder = onNewFolder, onNewFile = onNewFile)
                },
            )
        },
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
        if (subfolders.isEmpty() && files.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "暂无内容，点右上角 + 新建", color = ColorTextSecondary)
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
                items(subfolders, key = { it.id }) { subfolder ->
                    SelectableFolderCard(
                        folder = subfolder,
                        itemCount = library.itemCount(subfolder.id),
                        selected = subfolder.id in selection.ids,
                        isSelecting = selection.mode,
                        onClick = {
                            if (selection.mode) selection.toggle(subfolder.id) else onOpenFolder(subfolder)
                        },
                        onLongClick = { selection.enter(subfolder.id) },
                        onRename = { library.renameFolder(subfolder.id, it) },
                    )
                }
                items(files, key = { it.id }) { file ->
                    SelectableFileCard(
                        file = file,
                        selected = file.id in selection.ids,
                        isSelecting = selection.mode,
                        onClick = {
                            if (selection.mode) selection.toggle(file.id) else onOpenFile(file)
                        },
                        onLongClick = { selection.enter(file.id) },
                        onRename = { library.renameFile(file.id, it) },
                    )
                }
            }
        }
    }
}
