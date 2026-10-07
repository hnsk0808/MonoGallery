package com.sichuan.monogallery

import androidx.compose.runtime.Composable

/**
 * "添加到文件夹" picker for library entries: copies or moves the selected files and folders into the
 * folder the user browses to. The browsing and the copy/move sheet are [AddToFolderPicker], which the
 * 本地图片 page drives too, so both lists offer the same picker.
 */
@Composable
fun FolderPickerScreen(
    library: MonoLibrary,
    ids: Set<Long>,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val (fileIds, folderIds) = library.partitionIds(ids)

    AddToFolderPicker(
        library = library,
        onBack = onBack,
        onConfirm = { targetFolderId, move ->
            if (move) {
                library.moveFilesToFolder(fileIds, targetFolderId)
                library.moveFoldersToFolder(folderIds, targetFolderId)
            } else {
                library.copyFilesToFolder(fileIds, targetFolderId)
                library.copyFoldersToFolder(folderIds, targetFolderId)
            }
            onDone()
        },
    )
}
