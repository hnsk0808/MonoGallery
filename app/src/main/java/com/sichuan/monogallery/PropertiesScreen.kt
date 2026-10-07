package com.sichuan.monogallery

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Value shown for a property that is not available. */
private const val UNKNOWN = "—"

/** File properties screen: name, file type, file size, creation time, and modification time. */
@Composable
fun FilePropertiesScreen(
    library: MonoLibrary,
    fileId: Long,
    onBack: () -> Unit,
) {
    val file = library.file(fileId)
    val info = file?.let { library.fileInfo(it.id) }
    PropertiesScreen(onBack = onBack) {
        PropertyRow("名称", file?.fullName ?: "")
        PropertyRow("文件类型", file?.extension?.takeIf { it.isNotBlank() }?.uppercase() ?: "未知")
        PropertyRow("文件大小", formatBytes(info?.size))
        PropertyRow("创建时间", formatTimestamp(info?.createdMillis))
        PropertyRow("修改时间", formatTimestamp(info?.modifiedMillis))
        PropertyRow("文件路径", library.filePath(fileId) ?: UNKNOWN)
    }
}

/** Folder properties screen: name, item count, folder size, and creation time. */
@Composable
fun FolderPropertiesScreen(
    library: MonoLibrary,
    folderId: Long,
    onBack: () -> Unit,
) {
    val folder = library.folder(folderId)
    val info = folder?.let { library.folderInfo(it.id) }
    PropertiesScreen(onBack = onBack) {
        PropertyRow("名称", folder?.name ?: "")
        PropertyRow("项目数量", "${library.itemCount(folderId)} 项")
        PropertyRow("文件夹大小", formatBytes(info?.size))
        PropertyRow("创建时间", formatTimestamp(info?.createdMillis))
        PropertyRow("文件夹路径", library.folderPath(folderId) ?: UNKNOWN)
    }
}

/**
 * Properties of one image outside the library (the 本地图片 page): name, file type, file size,
 * creation time, modification time and the full path on disk.
 *
 * The rows match the library's file properties exactly; only the source of the values differs, since
 * there is no library entry to ask.
 */
@Composable
fun LocalImagePropertiesScreen(
    file: File,
    onBack: () -> Unit,
) {
    PropertiesScreen(onBack = onBack) {
        PropertyRow("名称", file.name)
        PropertyRow("文件类型", file.extension.takeIf { it.isNotBlank() }?.uppercase() ?: "未知")
        PropertyRow("文件大小", formatBytes(file.length()))
        PropertyRow("创建时间", formatTimestamp(creationTimeMillis(file)))
        PropertyRow("修改时间", formatTimestamp(file.lastModified()))
        PropertyRow("文件路径", file.absolutePath)
    }
}

/**
 * Properties of the 本地图片 folder: name, item count, total size and creation time.
 *
 * It deliberately has no path row — this folder does not exist on disk, it is the whole storage
 * minus the library — and it has no creation time of its own, so it reports the time of the oldest
 * image it holds.
 */
@Composable
fun LocalImagesFolderPropertiesScreen(
    state: LocalImagesState,
    onBack: () -> Unit,
) {
    PropertiesScreen(onBack = onBack) {
        PropertyRow("名称", LocalImagesFolderName)
        PropertyRow("项目数量", "${state.images.size} 项")
        PropertyRow("文件夹大小", formatBytes(state.totalSize()))
        PropertyRow("创建时间", formatTimestamp(state.earliestMillis()))
    }
}

/** Shared frame of the properties screens: the standard scaffold with a list of [content] rows. */
@Composable
private fun PropertiesScreen(
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    MonoScaffold(title = "属性", onBack = onBack) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            content = content,
        )
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

private fun formatBytes(bytes: Long?): String = when {
    bytes == null -> UNKNOWN
    bytes >= 1024L * 1024L -> String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0)
    else -> "$bytes B"
}

private fun formatTimestamp(millis: Long?): String =
    millis?.let { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it)) } ?: UNKNOWN
