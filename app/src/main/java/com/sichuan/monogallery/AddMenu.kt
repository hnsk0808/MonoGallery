package com.sichuan.monogallery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DriveFolderUpload
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Multi-document picker that also requests write access, so the picked source documents can be
 * deleted when the user chooses not to keep them.
 */
private class OpenMultipleWritableDocuments : ActivityResultContracts.OpenMultipleDocuments() {
    override fun createIntent(context: Context, input: Array<String>): Intent =
        super.createIntent(context, input).apply {
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
}

/**
 * Plus button: expands a menu for creating a folder / TXT file or importing existing folders /
 * files from the device. After picking an import source the user chooses whether to keep it;
 * choosing not to keep deletes the source after copying. Shared by the home screen and the folder
 * content screen; imports land in [parentFolderId] (a null id means the root directory).
 */
@Composable
fun AddMenu(
    library: MonoLibrary,
    parentFolderId: Long?,
    onNewFolder: () -> Unit,
    onNewFile: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var addExpanded by remember { mutableStateOf(false) }
    var pendingFiles by remember { mutableStateOf<List<Uri>?>(null) }
    var pendingFolder by remember { mutableStateOf<Uri?>(null) }

    // Pick one or more documents; ask whether to keep the originals before actually importing
    val pickFiles = rememberLauncherForActivityResult(OpenMultipleWritableDocuments()) { uris ->
        if (uris.isNotEmpty()) pendingFiles = uris
    }

    // Pick a directory tree; ask whether to keep it before actually importing
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) pendingFolder = uri
    }

    fun importFiles(uris: List<Uri>, keepSource: Boolean) {
        scope.launch {
            val message = withContext(Dispatchers.IO) {
                var imported = 0
                var failed = 0
                var deleteFailed = 0
                uris.forEach { uri ->
                    val result = runCatching { library.importFile(context, uri, parentFolderId) }
                    if (result.isFailure) {
                        failed++
                        return@forEach
                    }
                    imported++
                    if (!keepSource) {
                        val deleted = runCatching {
                            DocumentsContract.deleteDocument(context.contentResolver, uri)
                        }
                        if (deleted.isFailure) deleteFailed++
                    }
                }
                buildString {
                    if (keepSource || failed > 0) {
                        append("导入完成：$imported 个成功，$failed 个失败")
                    } else {
                        append("已移动 $imported 个文件")
                    }
                    if (deleteFailed > 0) append("，$deleteFailed 个原文件无法删除")
                }
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun importFolder(treeUri: Uri, keepSource: Boolean) {
        scope.launch {
            val message = withContext(Dispatchers.IO) {
                val imported = runCatching {
                    library.importFolderTree(context, treeUri, parentFolderId)
                }
                when {
                    imported.isFailure -> "导入失败"
                    keepSource -> "导入成功"
                    else -> {
                        val rootUri = DocumentsContract.buildDocumentUriUsingTree(
                            treeUri, DocumentsContract.getTreeDocumentId(treeUri),
                        )
                        val deleted = runCatching {
                            DocumentsContract.deleteDocument(context.contentResolver, rootUri)
                        }
                        if (deleted.isSuccess) "已移动原文件夹"
                        else "导入成功，但原文件夹无法删除"
                    }
                }
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

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
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("导入文件夹") },
                leadingIcon = {
                    Icon(Icons.Filled.DriveFolderUpload, contentDescription = null)
                },
                onClick = {
                    addExpanded = false
                    pickFolder.launch(null)
                },
            )
            DropdownMenuItem(
                text = { Text("导入文件") },
                leadingIcon = {
                    Icon(Icons.Filled.UploadFile, contentDescription = null)
                },
                onClick = {
                    addExpanded = false
                    pickFiles.launch(arrayOf("*/*"))
                },
            )
        }
    }

    pendingFiles?.let { uris ->
        AlertDialog(
            onDismissRequest = { pendingFiles = null },
            title = { Text("导入文件") },
            text = { Text("导入后是否保留原文件？") },
            confirmButton = {
                TextButton(onClick = {
                    pendingFiles = null
                    importFiles(uris, keepSource = true)
                }) { Text("保留") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { pendingFiles = null }) { Text("取消") }
                    TextButton(onClick = {
                        pendingFiles = null
                        importFiles(uris, keepSource = false)
                    }) { Text("不保留") }
                }
            },
        )
    }

    pendingFolder?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingFolder = null },
            title = { Text("导入文件夹") },
            text = { Text("导入后是否保留原文件夹？") },
            confirmButton = {
                TextButton(onClick = {
                    pendingFolder = null
                    importFolder(uri, keepSource = true)
                }) { Text("保留") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { pendingFolder = null }) { Text("取消") }
                    TextButton(onClick = {
                        pendingFolder = null
                        importFolder(uri, keepSource = false)
                    }) { Text("不保留") }
                }
            },
        )
    }
}
