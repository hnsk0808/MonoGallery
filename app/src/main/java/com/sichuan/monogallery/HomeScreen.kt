package com.sichuan.monogallery

import android.widget.Toast
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

/** 首页：展示文件夹与根目录文件，支持多选（添加到 / 删除 / 复制到剪切板）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    library: MonoLibrary,
    onOpenFolder: (Folder) -> Unit,
    onOpenFile: (MonoFile) -> Unit,
    onNewFolder: () -> Unit,
    onNewFile: () -> Unit,
    onAddToFolder: (Set<Long>) -> Unit,
) {
    val selection = remember { FileSelectionState() }
    val context = LocalContext.current

    BackHandler(enabled = selection.mode) { selection.exit() }

    Scaffold(
        containerColor = ColorBackground,
        topBar = { HomeTopBar(onNewFolder = onNewFolder, onNewFile = onNewFile) },
        bottomBar = {
            if (selection.mode) {
                SelectionBottomBar(
                    library = library,
                    selection = selection,
                    onAddTo = { onAddToFolder(it) },
                    onDelete = {
                        library.deleteFiles(it)
                        selection.exit()
                    },
                )
            }
        },
    ) { innerPadding ->
        if (library.folders.isEmpty() && library.rootFiles().isEmpty()) {
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
                items(library.folders, key = { it.id }) { folder ->
                    FolderCard(
                        folder = folder,
                        itemCount = library.itemCount(folder.id),
                        onOpen = { onOpenFolder(folder) },
                        onRename = { library.renameFolder(folder.id, it) },
                        onCompress = {
                            val zip = library.compressFolder(folder.id)
                            if (zip != null) {
                                Toast.makeText(context, "已压缩为 ${zip.name}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDelete = { library.deleteFolder(folder.id) },
                    )
                }
                items(library.rootFiles(), key = { it.id }) { file ->
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    onNewFolder: () -> Unit,
    onNewFile: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var addExpanded by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

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

        // 加号 -> 展开两个选项
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
                    text = { Text("新建文件") },
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

        // 展开菜单
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
                    text = { Text("排序") },
                    onClick = { menuExpanded = false },
                )
                DropdownMenuItem(
                    text = { Text("设置") },
                    onClick = { menuExpanded = false },
                )
            }
        }
    }
}
