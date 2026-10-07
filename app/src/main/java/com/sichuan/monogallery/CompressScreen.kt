package com.sichuan.monogallery

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

/**
 * Compress screen for library entries: the selected files and folders of the current directory are
 * packed into a single `.zip`, whose name and save location the user fills in.
 *
 * [parentFolderId] is the directory the selection lives in: the archive's entries are read from there,
 * and it is also the save location the screen starts on. The "保存位置" row browses the library for
 * another destination folder.
 */
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

    CompressForm(
        library = library,
        initialName = library.firstSelectedName(ids),
        hint = "将把选中的 ${ids.size} 个项目压缩为一个 .zip",
        initialDestinationId = parentFolderId,
        confirmEnabled = ids.isNotEmpty(),
        onBack = onBack,
        onConfirm = { zipName, destinationId ->
            val zip = library.compressItemsToZip(
                fileIds = fileIds,
                folderIds = folderIds,
                zipName = zipName,
                sourceFolderId = parentFolderId,
                destinationFolderId = destinationId,
            )
            Toast.makeText(
                context,
                if (zip != null) "已压缩为 ${zip.name}" else "压缩失败",
                Toast.LENGTH_SHORT,
            ).show()
            onDone()
        },
    )
}

/**
 * Compress screen for the images of the 本地图片 page: the same form as the library's [CompressScreen]
 * — name plus save location — except that the entries are files on the device rather than library
 * entries, so the archive starts out in the library root.
 */
@Composable
fun LocalImagesCompressScreen(
    library: MonoLibrary,
    files: List<File>,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current

    CompressForm(
        library = library,
        initialName = files.firstOrNull()?.nameWithoutExtension.orEmpty(),
        hint = "将把选中的 ${files.size} 张图片压缩为一个 .zip",
        initialDestinationId = null,
        confirmEnabled = files.isNotEmpty(),
        onBack = onBack,
        onConfirm = { zipName, destinationId ->
            val zip = library.compressExternalFilesToZip(files, zipName, destinationId)
            Toast.makeText(
                context,
                if (zip != null) "已压缩为 ${zip.name}" else "压缩失败",
                Toast.LENGTH_SHORT,
            ).show()
            onDone()
        },
    )
}

/**
 * The compress form itself — a name, a save location and the folder sheet behind it — shared by the
 * library selection ([CompressScreen]) and the local images ([LocalImagesCompressScreen]): [onConfirm]
 * packs whatever those callers selected, so only the entry list differs between them.
 */
@Composable
internal fun CompressForm(
    library: MonoLibrary,
    initialName: String,
    hint: String,
    initialDestinationId: Long?,
    confirmEnabled: Boolean,
    onBack: () -> Unit,
    onConfirm: (zipName: String, destinationFolderId: Long?) -> Unit,
) {
    var destinationId by remember { mutableStateOf(initialDestinationId) }
    var showPicker by remember { mutableStateOf(false) }

    NameFormScreen(
        title = "压缩",
        label = "压缩名称",
        placeholder = "例如：随笔合集",
        confirmText = "压缩",
        onBack = onBack,
        initialValue = initialName,
        hint = hint,
        confirmEnabled = confirmEnabled,
        extraContent = {
            DestinationRow(
                folderName = folderLabel(library, destinationId),
                onClick = { showPicker = true },
            )
        },
        onConfirm = { name -> onConfirm(name, destinationId) },
    )

    if (showPicker) {
        DestinationPickerSheet(
            library = library,
            initialFolderId = destinationId,
            onDismiss = { showPicker = false },
            onPick = { picked ->
                destinationId = picked
                showPicker = false
            },
        )
    }
}

/** Display name of a library folder id; a null id means the library root. */
private fun folderLabel(library: MonoLibrary, folderId: Long?): String =
    folderId?.let { library.folder(it)?.name } ?: "根目录"

/** "保存位置" row of the compress form: the current save folder, tappable to browse for another. */
@Composable
private fun DestinationRow(folderName: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "保存位置", fontSize = 15.sp, color = ColorTextPrimary)
        Spacer(Modifier.weight(1f))
        Text(text = folderName, fontSize = 15.sp, color = ColorTextSecondary)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = ColorTextSecondary,
        )
    }
}

/**
 * Bottom sheet that browses the library's folders to pick where the archive is written, starting at
 * [initialFolderId]. The grid inside is [FolderBrowser] — the same browser the "添加到" picker uses —
 * so both pickers navigate identically.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DestinationPickerSheet(
    library: MonoLibrary,
    initialFolderId: Long?,
    onDismiss: () -> Unit,
    onPick: (Long?) -> Unit,
) {
    var browseId by remember { mutableStateOf(initialFolderId) }
    var showNewFolderDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxHeight(0.85f)) {
            Text(
                text = "保存到「${folderLabel(library, browseId)}」",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = ColorTextPrimary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            )
            // weight(1f): the browser is a lazy grid, so it needs a bounded height inside the sheet
            FolderBrowser(
                library = library,
                currentFolderId = browseId,
                onNavigate = { browseId = it },
                onNewFolder = { showNewFolderDialog = true },
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { onPick(browseId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding(),
            ) {
                Text("保存到此处")
            }
        }
    }

    if (showNewFolderDialog) {
        NameInputDialog(
            title = "新建文件夹",
            label = "名称",
            placeholder = "例如：压缩包",
            onConfirm = { name ->
                library.createFolder(name, parentId = browseId)
                showNewFolderDialog = false
            },
            onDismiss = { showNewFolderDialog = false },
        )
    }
}
