package com.sichuan.monogallery

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 压缩：输入压缩名称（默认第一个卡片名），把选中的文件与文件夹压缩为单个 `.zip`。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressScreen(
    library: MonoLibrary,
    ids: Set<Long>,
    parentFolderId: Long?,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val fileIds = ids.filter { library.file(it) != null }.toSet()
    val folderIds = ids.filter { library.folder(it) != null }.toSet()
    var name by remember { mutableStateOf(library.firstSelectedName(ids)) }

    Scaffold(
        containerColor = ColorBackground,
        topBar = {
            TopAppBar(
                title = { Text("压缩") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            TextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("压缩名称") },
                placeholder = { Text("例如：随笔合集") },
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "将把选中的 ${ids.size} 个项目压缩为一个 .zip",
                fontSize = 13.sp,
                color = ColorTextSecondary,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val zip = library.compressItemsToZip(fileIds, folderIds, name, parentFolderId)
                    Toast.makeText(
                        context,
                        if (zip != null) "已压缩为 ${zip.name}" else "压缩失败",
                        Toast.LENGTH_SHORT,
                    ).show()
                    onDone()
                },
                enabled = name.isNotBlank() && ids.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("压缩")
            }
        }
    }
}
