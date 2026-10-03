package com.sichuan.monogallery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {

    /** 是否已获得读写 /storage/emulated/0 的权限，驱动 Compose 界面重建。 */
    private var storageAccess by mutableStateOf(hasStorageAccess())

    /** Android 10（API 29）的运行时存储权限。 */
    private val requestLegacyStorage = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { storageAccess = hasStorageAccess() }

    /** Android 11+（API 30+）的「所有文件访问权限」设置页。 */
    private val requestAllFilesAccess = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { storageAccess = hasStorageAccess() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (!storageAccess) requestStorageAccess()
        setContent {
            MaterialTheme {
                MonoGalleryApp(storageAccess = storageAccess, onRequestAccess = ::requestStorageAccess)
            }
        }
    }

    private fun hasStorageAccess(): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
            Environment.isExternalStorageManager()
        else ->
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                requestAllFilesAccess.launch(
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:$packageName"),
                    ),
                )
            } catch (_: Exception) {
                // 部分设备没有针对单个应用的入口，回退到通用的「所有文件访问」设置页
                try {
                    requestAllFilesAccess.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                } catch (_: Exception) {
                    // 无可用设置页时保持当前状态，用户可再次点击按钮重试
                }
            }
        } else {
            requestLegacyStorage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
}

/** 应用入口：装配导航路由与各页面。 */
@Composable
fun MonoGalleryApp(storageAccess: Boolean, onRequestAccess: () -> Unit) {
    if (!storageAccess) {
        StorageAccessScreen(onRequestAccess = onRequestAccess)
        return
    }

    val navController = rememberNavController()
    val context = LocalContext.current
    val library = remember { MonoLibrary(context.applicationContext) }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                library = library,
                onOpenFolder = { navController.navigate("folder/${it.id}") },
                onOpenFile = { navController.navigate("file/${it.id}") },
                onNewFolder = { navController.navigate("new_folder/-1") },
                onNewFile = { library.createFile("新建文件") },
                onRefresh = { library.refresh() },
                onAddToFolder = { ids -> navController.navigate("folder_picker/${ids.joinToString(",")}") },
                onCompress = { ids -> navController.navigate("compress/${ids.joinToString(",")}/-1") },
                onOpenFolderProperties = { navController.navigate("folder_props/$it") },
                onOpenFileProperties = { navController.navigate("file_props/$it") },
            )
        }
        composable("folder_picker/{ids}") { entry ->
            val ids = entry.arguments?.getString("ids")
                ?.split(",")
                ?.mapNotNull { it.toLongOrNull() }
                ?.toSet()
                ?: emptySet()
            FolderPickerScreen(
                library = library,
                ids = ids,
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() },
            )
        }
        composable("compress/{ids}/{parentId}") { entry ->
            val ids = entry.arguments?.getString("ids")
                ?.split(",")
                ?.mapNotNull { it.toLongOrNull() }
                ?.toSet()
                ?: emptySet()
            val parentId = entry.arguments?.getString("parentId")?.toLongOrNull()?.takeIf { it >= 0 }
            CompressScreen(
                library = library,
                ids = ids,
                parentFolderId = parentId,
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() },
            )
        }
        composable("folder/{folderId}") { entry ->
            val folderId = entry.arguments?.getString("folderId")?.toLongOrNull() ?: return@composable
            FolderContentScreen(
                library = library,
                folderId = folderId,
                onBack = { navController.popBackStack() },
                onOpenFolder = { navController.navigate("folder/${it.id}") },
                onOpenFile = { navController.navigate("file/${it.id}") },
                onNewFolder = { navController.navigate("new_folder/$folderId") },
                onNewFile = { library.createFile("新建文件", folderId = folderId) },
                onRefresh = { library.refresh() },
                onAddToFolder = { ids -> navController.navigate("folder_picker/${ids.joinToString(",")}") },
                onCompress = { ids -> navController.navigate("compress/${ids.joinToString(",")}/$folderId") },
                onOpenFolderProperties = { navController.navigate("folder_props/$it") },
                onOpenFileProperties = { navController.navigate("file_props/$it") },
            )
        }
        composable("file/{fileId}") { entry ->
            val fileId = entry.arguments?.getString("fileId")?.toLongOrNull() ?: return@composable
            FileViewScreen(
                library = library,
                fileId = fileId,
                onBack = { navController.popBackStack() },
            )
        }
        composable("file_props/{fileId}") { entry ->
            val fileId = entry.arguments?.getString("fileId")?.toLongOrNull() ?: return@composable
            FilePropertiesScreen(
                library = library,
                fileId = fileId,
                onBack = { navController.popBackStack() },
            )
        }
        composable("folder_props/{folderId}") { entry ->
            val folderId = entry.arguments?.getString("folderId")?.toLongOrNull() ?: return@composable
            FolderPropertiesScreen(
                library = library,
                folderId = folderId,
                onBack = { navController.popBackStack() },
            )
        }
        composable("new_folder/{parentId}") { entry ->
            val parentId = entry.arguments?.getString("parentId")?.toLongOrNull()?.takeIf { it >= 0 }
            NewFolderScreen(
                onBack = { navController.popBackStack() },
                onCreate = { name ->
                    library.createFolder(name, parentId)
                    navController.popBackStack()
                },
            )
        }
    }
}

/** 未获得存储权限时的引导页：解释用途并提供授予权限的入口。 */
@Composable
private fun StorageAccessScreen(onRequestAccess: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("需要存储权限", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "MonoGallery 需要访问 /storage/emulated/0 才能读写文件。",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRequestAccess) {
                Text("授予存储权限")
            }
        }
    }
}
