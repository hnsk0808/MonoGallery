package com.sichuan.monogallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Folder picker behind every "添加到" action: browse the library (enter a subfolder, go up one level,
 * jump to the root directory, create a folder), confirm, and then pick whether the selection is
 * copied or moved into the folder that was browsed to.
 *
 * What "copy" and "move" mean is the caller's business, through [onConfirm]: the library folder page
 * passes entries of the library, while the 本地图片 page passes pictures that live on the device — both
 * drive this one picker, so "添加到" behaves the same in either list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToFolderPicker(
    library: MonoLibrary,
    onBack: () -> Unit,
    onConfirm: (folderId: Long?, move: Boolean) -> Unit,
) {
    // A null currentFolderId means we are currently in the root directory.
    var currentFolderId by remember { mutableStateOf<Long?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }

    val currentFolder = currentFolderId?.let { library.folder(it) }

    MonoScaffold(
        title = currentFolder?.name ?: "根目录",
        onBack = onBack,
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
        FolderBrowser(
            library = library,
            currentFolderId = currentFolderId,
            onNavigate = { currentFolderId = it },
            onNewFolder = { showNewFolderDialog = true },
            modifier = Modifier.padding(innerPadding),
        )
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
                    onConfirm(currentFolderId, false)
                }
                SheetOption("移动") {
                    showSheet = false
                    onConfirm(currentFolderId, true)
                }
                SheetOption("取消") {
                    showSheet = false
                }
            }
        }
    }

    if (showNewFolderDialog) {
        NameInputDialog(
            title = "新建文件夹",
            label = "名称",
            placeholder = "例如：随笔",
            onConfirm = { name ->
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
