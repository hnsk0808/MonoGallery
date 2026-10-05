package com.sichuan.monogallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
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

/**
 * Add-to-folder picker: browse to choose a target directory (enter subfolders / go up one
 * level / return to the root directory); tapping "Confirm" at the bottom opens a
 * "Copy / Move / Cancel" sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPickerScreen(
    library: MonoLibrary,
    ids: Set<Long>,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val fileIds = ids.filter { library.file(it) != null }.toSet()
    val folderIds = ids.filter { library.folder(it) != null }.toSet()

    // A null currentFolderId means we are currently in the root directory.
    var currentFolderId by remember { mutableStateOf<Long?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }

    val currentFolder = currentFolderId?.let { library.folder(it) }
    val subfolders = library.subfoldersOf(currentFolderId)

    Scaffold(
        containerColor = ColorBackground,
        topBar = {
            TopAppBar(
                title = { Text(currentFolder?.name ?: "根目录") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = ColorCard, shadowElevation = 8.dp) {
                Button(
                    onClick = { showSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                ) {
                    Text("确认")
                }
            }
        },
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Go up one level / jump to the root directory (cd back under the root)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { currentFolderId = currentFolder?.parentId },
                        enabled = currentFolderId != null,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("上一层")
                    }
                    OutlinedButton(
                        onClick = { currentFolderId = null },
                        enabled = currentFolderId != null,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Filled.Home, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("到根目录")
                    }
                }
            }
            // "New folder" creates a subfolder inside the current directory
            item(span = { GridItemSpan(maxLineSpan) }) {
                OutlinedButton(
                    onClick = { showNewFolderDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("新建文件夹")
                }
            }
            items(subfolders, key = { it.id }) { folder ->
                PickerFolderCard(
                    folder = folder,
                    itemCount = library.itemCount(folder.id),
                    onClick = { currentFolderId = folder.id },
                )
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            Column(Modifier.padding(bottom = 32.dp)) {
                Text(
                    text = "添加到「${currentFolder?.name ?: "根目录"}」",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTextPrimary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
                SheetOption("复制") {
                    showSheet = false
                    library.copyFilesToFolder(fileIds, currentFolderId)
                    library.copyFoldersToFolder(folderIds, currentFolderId)
                    onDone()
                }
                SheetOption("移动") {
                    showSheet = false
                    library.moveFilesToFolder(fileIds, currentFolderId)
                    library.moveFoldersToFolder(folderIds, currentFolderId)
                    onDone()
                }
                SheetOption("取消") {
                    showSheet = false
                }
            }
        }
    }

    if (showNewFolderDialog) {
        NewFolderDialog(
            onCreate = { name ->
                library.createFolder(name, parentId = currentFolderId)
                showNewFolderDialog = false
            },
            onDismiss = { showNewFolderDialog = false },
        )
    }
}

/** A single clickable option row inside the confirm bottom sheet. */
@Composable
private fun SheetOption(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(text, fontSize = 15.sp, color = ColorTextPrimary)
    }
}

/** Dialog that prompts for a folder name and creates a folder in the current directory. */
@Composable
private fun NewFolderDialog(
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建文件夹") },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名称") },
                placeholder = { Text("例如：随笔") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name.trim()) }, // trim: drop leading/trailing whitespace from the new folder name
                enabled = name.isNotBlank(),
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
