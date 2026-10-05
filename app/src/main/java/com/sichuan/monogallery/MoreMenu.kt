package com.sichuan.monogallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SortByAlpha
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

/**
 * Overflow (more) menu. Shared by the home screen and the folder content screen for consistency.
 * Besides refresh, it offers sorting by name (ascending) or by date (newest first); the active
 * sort mode is marked with a check.
 */
@Composable
fun MoreMenu(
    onRefresh: () -> Unit,
    sortMode: SortMode,
    onSortModeChange: (SortMode) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuExpanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "更多",
                tint = ColorTextPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("刷新") },
                leadingIcon = {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                },
                onClick = {
                    menuExpanded = false
                    onRefresh()
                },
            )
            DropdownMenuItem(
                text = { Text("按名称排序") },
                leadingIcon = {
                    Icon(Icons.Filled.SortByAlpha, contentDescription = null)
                },
                trailingIcon = {
                    if (sortMode == SortMode.NAME) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                    }
                },
                onClick = {
                    menuExpanded = false
                    onSortModeChange(SortMode.NAME)
                },
            )
            DropdownMenuItem(
                text = { Text("按日期排序") },
                leadingIcon = {
                    Icon(Icons.Filled.DateRange, contentDescription = null)
                },
                trailingIcon = {
                    if (sortMode == SortMode.DATE) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                    }
                },
                onClick = {
                    menuExpanded = false
                    onSortModeChange(SortMode.DATE)
                },
            )
        }
    }
}
