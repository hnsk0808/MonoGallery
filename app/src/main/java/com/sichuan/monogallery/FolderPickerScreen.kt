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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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

/** 添加到文件夹：选择目标文件夹（或根目录），以复制或移动方式添加选中文件。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPickerScreen(
    library: MonoLibrary,
    fileIds: Set<Long>,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    // sheetFolder 为 null 表示目标是根目录；showSheet 控制底部菜单显隐。
    var sheetFolder by remember { mutableStateOf<Folder?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = ColorBackground,
        topBar = {
            TopAppBar(
                title = { Text("添加到文件夹") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
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
            // 「+ 新建文件夹」按钮与文件夹卡片同一层级（占满整行）
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
            // 「到根目录」按钮：把选中文件添加（复制/移动）到根目录
            item(span = { GridItemSpan(maxLineSpan) }) {
                OutlinedButton(
                    onClick = {
                        sheetFolder = null
                        showSheet = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Filled.Home, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("到根目录")
                }
            }
            items(library.folders, key = { it.id }) { folder ->
                PickerFolderCard(
                    folder = folder,
                    itemCount = library.itemCount(folder.id),
                    onClick = {
                        sheetFolder = folder
                        showSheet = true
                    },
                )
            }
        }
    }

    if (showSheet) {
        val folder = sheetFolder
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            Column(Modifier.padding(bottom = 32.dp)) {
                Text(
                    text = "添加到「${folder?.name ?: "根目录"}」",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTextPrimary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
                SheetOption("复制") {
                    showSheet = false
                    library.copyFilesToFolder(fileIds, folder?.id)
                    onDone()
                }
                SheetOption("移动") {
                    showSheet = false
                    library.moveFilesToFolder(fileIds, folder?.id)
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
                library.createFolder(name)
                showNewFolderDialog = false
            },
            onDismiss = { showNewFolderDialog = false },
        )
    }
}

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
                onClick = { onCreate(name.trim()) },
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
