package com.sichuan.monogallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                MonoGalleryApp()
            }
        }
    }
}

/** 应用入口：装配导航路由与各页面。 */
@Composable
fun MonoGalleryApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val library = remember { MonoLibrary(context.applicationContext) }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                library = library,
                onOpenFolder = { navController.navigate("folder/${it.id}") },
                onOpenFile = { navController.navigate("file/${it.id}") },
                onNewFolder = { navController.navigate("new_folder") },
                onNewFile = { library.createFile("新建文件") },
                onAddToFolder = { ids -> navController.navigate("folder_picker/${ids.joinToString(",")}") },
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
                fileIds = ids,
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
                onOpenFile = { navController.navigate("file/${it.id}") },
                onAddToFolder = { ids -> navController.navigate("folder_picker/${ids.joinToString(",")}") },
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
        composable("new_folder") {
            NewFolderScreen(
                onBack = { navController.popBackStack() },
                onCreate = { name ->
                    library.createFolder(name)
                    navController.popBackStack()
                },
            )
        }
    }
}
