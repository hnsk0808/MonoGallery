package com.sichuan.monogallery

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Compress screen: enter a zip name (defaulting to the first selected card's name) and compress the selected files and folders into a single `.zip` archive. */
@Composable
fun CompressScreen(
    library: MonoLibrary,
    ids: Set<Long>,
    parentFolderId: Long?,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val (fileIds, folderIds) = library.partitionIds(ids)

    NameFormScreen(
        title = "压缩",
        label = "压缩名称",
        placeholder = "例如：随笔合集",
        confirmText = "压缩",
        onBack = onBack,
        initialValue = library.firstSelectedName(ids),
        hint = "将把选中的 ${ids.size} 个项目压缩为一个 .zip",
        confirmEnabled = ids.isNotEmpty(),
        onConfirm = { name ->
            val zip = library.compressItemsToZip(fileIds, folderIds, name, parentFolderId)
            Toast.makeText(
                context,
                if (zip != null) "已压缩为 ${zip.name}" else "压缩失败",
                Toast.LENGTH_SHORT,
            ).show()
            onDone()
        },
    )
}
