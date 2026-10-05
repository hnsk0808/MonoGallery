package com.sichuan.monogallery

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** File properties screen: name, file type, file size, creation time, and modification time. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePropertiesScreen(
    library: MonoLibrary,
    fileId: Long,
    onBack: () -> Unit,
) {
    val file = library.file(fileId)
    val info = file?.let { library.fileInfo(it.id) }
    PropertiesScaffold(title = "属性", onBack = onBack) {
        PropertyRow("名称", file?.fullName ?: "")
        PropertyRow("文件类型", file?.extension?.takeIf { it.isNotBlank() }?.uppercase() ?: "未知")
        PropertyRow("文件大小", info?.size?.let { formatBytes(it) } ?: "—")
        PropertyRow("创建时间", info?.createdMillis?.let { formatTime(it) } ?: "—")
        PropertyRow("修改时间", info?.modifiedMillis?.let { formatTime(it) } ?: "—")
        PropertyRow("文件路径", library.filePath(fileId) ?: "—")
    }
}

/** Folder properties screen: name, item count, folder size, and creation time. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPropertiesScreen(
    library: MonoLibrary,
    folderId: Long,
    onBack: () -> Unit,
) {
    val folder = library.folder(folderId)
    val info = folder?.let { library.folderInfo(it.id) }
    PropertiesScaffold(title = "属性", onBack = onBack) {
        PropertyRow("名称", folder?.name ?: "")
        PropertyRow("项目数量", "${library.itemCount(folderId)} 项")
        PropertyRow("文件夹大小", info?.size?.let { formatBytes(it) } ?: "—")
        PropertyRow("创建时间", info?.createdMillis?.let { formatTime(it) } ?: "—")
        PropertyRow("文件夹路径", library.folderPath(folderId) ?: "—")
    }
}

/** Shared scaffold for the properties screens: a titled top bar with a back button wrapping [content]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PropertiesScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        containerColor = ColorBackground,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            content()
        }
    }
}

/** A single label/value row in the properties list, with the value made selectable. */
@Composable
private fun PropertyRow(label: String, value: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
                color = ColorTextSecondary,
                modifier = Modifier.width(96.dp),
            )
            SelectionContainer {
                Text(
                    text = value,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorTextPrimary,
                )
            }
        }
        HorizontalDivider(color = ColorSearchField, thickness = 1.dp)
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0)
    else -> "$bytes B"
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(millis))
