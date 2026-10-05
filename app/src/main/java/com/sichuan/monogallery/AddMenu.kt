package com.sichuan.monogallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Plus button: expands a "New folder / New file" menu. Shared by the home screen and the folder content screen for consistency. */
@Composable
fun AddMenu(
    onNewFolder: () -> Unit,
    onNewFile: () -> Unit,
) {
    var addExpanded by remember { mutableStateOf(false) }
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
        }
    }
}
