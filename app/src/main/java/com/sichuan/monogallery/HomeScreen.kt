package com.sichuan.monogallery

import androidx.compose.runtime.Composable

/**
 * Home screen: the special case of [FolderContentScreen] for the root directory — a null folder id
 * means the root, and no back entry exists because the root is the navigation start destination.
 * The top bar, the card grid, the multi-selection actions and the share dialog are all shared with
 * the folder content screen; only the navigation callbacks (which need the route ids) differ.
 */
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
    FolderContentScreen(
        library = library,
        folderId = null,
        onBack = null,
        onOpenFolder = onOpenFolder,
        onOpenFile = onOpenFile,
        onNewFolder = onNewFolder,
        onNewFile = onNewFile,
        onRefresh = onRefresh,
        onAddToFolder = onAddToFolder,
        onCompress = onCompress,
        onOpenFolderProperties = onOpenFolderProperties,
        onOpenFileProperties = onOpenFileProperties,
    )
}
