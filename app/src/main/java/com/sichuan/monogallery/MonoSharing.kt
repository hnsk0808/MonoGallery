package com.sichuan.monogallery

import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 通过系统分享面板分享一个文件。 */
fun shareFile(context: Context, file: File, extension: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeTypeFor(extension)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "分享文件"))
}

/** 分享一个压缩后的文件夹（zip）。 */
fun shareZip(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "分享文件夹"))
}

/**
 * 文件夹分享进度弹窗：后台线程把文件夹压缩为 zip，完成后自动关闭并弹出系统分享面板。
 * 压缩期间不可取消；压缩失败（返回 null 或抛异常）时直接关闭，不分享。
 */
@Composable
fun FolderShareProgressDialog(
    library: MonoLibrary,
    folderId: Long,
    onDone: () -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(folderId) {
        val zip = try {
            withContext(Dispatchers.IO) { library.folderShareZip(folderId) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        onDone()
        zip?.let { shareZip(context, it) }
    }

    AlertDialog(
        onDismissRequest = {},
        title = { Text("分享文件夹") },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(16.dp))
                Text("正在压缩…")
            }
        },
        confirmButton = {},
    )
}

private fun mimeTypeFor(extension: String): String =
    MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
        ?: "application/octet-stream"
