package com.sichuan.monogallery

import android.content.ActivityNotFoundException
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
import android.widget.Toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Shares a single file through the system share sheet. */
fun shareFile(context: Context, file: File, extension: String) {
    // Expose the file through FileProvider and grant the receiving app temporary read access to the URI
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeTypeFor(extension)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "分享文件"))
}

/**
 * Opens a file with an app picked by the user through the system "open with" chooser.
 * When no installed app can handle the file, a toast is shown instead of crashing.
 */
fun openFileWith(context: Context, file: File, extension: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeTypeFor(extension))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "打开方式"))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "没有可打开此文件的应用", Toast.LENGTH_SHORT).show()
    }
}

/** Shares a compressed folder (a `.zip` archive) through the system share sheet. */
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
 * Folder-sharing progress dialog: compresses the folder into a zip archive on a background
 * thread, then closes automatically and opens the system share sheet when done.
 * The compression cannot be canceled; on failure (a null result or an exception) the dialog
 * closes without sharing.
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
            // Must rethrow cancellation so structured concurrency is not broken
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

/** Looks up the MIME type for an [extension], falling back to a generic binary stream type. */
private fun mimeTypeFor(extension: String): String =
    MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
        ?: "application/octet-stream"
