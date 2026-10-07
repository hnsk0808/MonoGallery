package com.sichuan.monogallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Folder browser shared by the folder pickers: the library's folders as the usual cards, with
 * "上一层", "到根目录" and "新建文件夹" above them. A null [currentFolderId] is the root directory.
 *
 * The caller owns the browse position ([currentFolderId] plus [onNavigate] and [onNewFolder]) and
 * decides what the visited folder is used for, so the same grid serves both "添加到" and "the zip
 * is saved here". [modifier] carries the caller's insets or height bound.
 */
@Composable
fun FolderBrowser(
    library: MonoLibrary,
    currentFolderId: Long?,
    onNavigate: (Long?) -> Unit,
    onNewFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentFolder = currentFolderId?.let { library.folder(it) }
    val subfolders = library.subfoldersOf(currentFolderId)

    MonoGrid(modifier = modifier) {
        // Go up one level / jump to the root directory (cd back under the root)
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { onNavigate(currentFolder?.parentId) },
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
                    onClick = { onNavigate(null) },
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
                onClick = onNewFolder,
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
                onClick = { onNavigate(folder.id) },
            )
        }
    }
}
