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
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/** Main activity: owns the external-storage permission flow and hosts the top-level Compose UI. */
class MainActivity : ComponentActivity() {

    /** Whether read/write access to /storage/emulated/0 has been granted; drives recomposition of the Compose UI. */
    private var storageAccess by mutableStateOf(hasStorageAccess())

    /** Runtime storage permission request for Android 10 (API 29). */
    private val requestLegacyStorage = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { storageAccess = hasStorageAccess() }

    /** "All files access" settings-page launcher for Android 11+ (API 30+). */
    private val requestAllFilesAccess = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { storageAccess = hasStorageAccess() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Ask for storage access up front; the content screens only render once access is granted
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
                // Some devices have no per-app entry, so fall back to the generic "All files access" settings page
                try {
                    requestAllFilesAccess.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                } catch (_: Exception) {
                    // No usable settings page: stay in the current state; the user can tap the button again to retry
                }
            }
        } else {
            requestLegacyStorage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
}

/** App entry point: wires up the navigation routes and the individual screens. */
@Composable
fun MonoGalleryApp(storageAccess: Boolean, onRequestAccess: () -> Unit) {
    if (!storageAccess) {
        StorageAccessScreen(onRequestAccess = onRequestAccess)
        return
    }

    val navController = rememberNavController()
    val context = LocalContext.current
    val library = remember { MonoLibrary(context.applicationContext) }
    // App-wide, persisted sort mode: one instance shared by the home screen and every folder screen
    val sortPreference = remember { SortPreference(context.applicationContext) }
    // The images outside the library are scanned once per session and read by both the 本地 card and
    // the 本地图片 page, so they share one piece of state created here.
    val localImages = rememberLocalImages(excludedDir = library.storageRoot)
    // Separate sort preference: the local images keep their own order, so sorting them does not
    // reorder the library and vice versa.
    val localSortPreference = remember { SortPreference(context.applicationContext, LOCAL_IMAGES_SORT_KEY) }

    NavHost(navController = navController, startDestination = HomeTab.Mono.route) {
        composable(HomeTab.Mono.route) {
            HomeScreen(
                library = library,
                sortPreference = sortPreference,
                onOpenFolder = { navController.navigate("folder/${it.id}") },
                onOpenFile = { navController.navigate("file/${it.id}") },
                onNewFolder = { navController.navigate("new_folder/-1") },
                onNewFile = { library.createFile("新建文件") },
                onRefresh = { library.refresh() },
                onAddToFolder = { ids -> navController.navigate("folder_picker/${ids.joinToString(",")}") },
                onCompress = { ids -> navController.navigate("compress/${ids.joinToString(",")}/-1") },
                onOpenFolderProperties = { navController.navigate("folder_props/$it") },
                onOpenFileProperties = { navController.navigate("file_props/$it") },
                onSelectTab = { navController.switchTab(it) },
            )
        }
        // The remaining bottom-bar destinations: sibling pages of the home screen, switched by the
        // bar rather than stacked, which is what switchTab sets up.
        composable(HomeTab.Local.route) {
            LocalScreen(
                localImages = localImages,
                localSort = localSortPreference,
                onSelectTab = { navController.switchTab(it) },
                onOpenLocalImages = { navController.navigate(LocalImagesRoute) },
                onOpenLocalImagesProperties = { navController.navigate(LocalImagesFolderPropertiesRoute) },
            )
        }
        // The 本地图片 page: a page stacked on the 本地 tab, so it has a back button and no bottom
        // bar, matching the rule that the bottom bar only exists at the top level.
        composable(LocalImagesRoute) {
            LocalImagesScreen(
                state = localImages,
                library = library,
                sortPreference = localSortPreference,
                onBack = { navController.popBackStack() },
            )
        }
        // The properties of the 本地图片 folder. The properties of a single image are shown inside
        // the page instead, because an absolute path does not belong in a navigation route.
        composable(LocalImagesFolderPropertiesRoute) {
            LocalImagesFolderPropertiesScreen(
                state = localImages,
                onBack = { navController.popBackStack() },
            )
        }
        composable(HomeTab.Tools.route) {
            ToolsScreen(onSelectTab = { navController.switchTab(it) })
        }
        composable(HomeTab.Settings.route) {
            SettingsScreen(onSelectTab = { navController.switchTab(it) })
        }
        composable("folder_picker/{ids}") { entry ->
            val ids = entry.idSet()
            FolderPickerScreen(
                library = library,
                ids = ids,
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() },
            )
        }
        composable("compress/{ids}/{parentId}") { entry ->
            val ids = entry.idSet()
            val parentId = entry.parentIdArg()
            CompressScreen(
                library = library,
                ids = ids,
                parentFolderId = parentId,
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() },
            )
        }
        composable("folder/{folderId}") { entry ->
            val folderId = entry.longArg("folderId") ?: return@composable
            FolderContentScreen(
                library = library,
                sortPreference = sortPreference,
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
            val fileId = entry.longArg("fileId") ?: return@composable
            FileViewScreen(
                library = library,
                fileId = fileId,
                onBack = { navController.popBackStack() },
            )
        }
        composable("file_props/{fileId}") { entry ->
            val fileId = entry.longArg("fileId") ?: return@composable
            FilePropertiesScreen(
                library = library,
                fileId = fileId,
                onBack = { navController.popBackStack() },
            )
        }
        composable("folder_props/{folderId}") { entry ->
            val folderId = entry.longArg("folderId") ?: return@composable
            FolderPropertiesScreen(
                library = library,
                folderId = folderId,
                onBack = { navController.popBackStack() },
            )
        }
        composable("new_folder/{parentId}") { entry ->
            val parentId = entry.parentIdArg()
            NameFormScreen(
                title = "新建文件夹",
                label = "文件夹名称",
                placeholder = "例如：随笔",
                confirmText = "创建",
                onBack = { navController.popBackStack() },
                onConfirm = { name ->
                    library.createFolder(name, parentId)
                    navController.popBackStack()
                },
            )
        }
    }
}

/** Reads the comma-separated id list argument of the current route (e.g. "3,7" for multi-selection flows). */
private fun NavBackStackEntry.idSet(key: String = "ids"): Set<Long> =
    arguments?.getString(key)
        ?.split(",")
        ?.mapNotNull { it.toLongOrNull() }
        ?.toSet()
        ?: emptySet()

/**
 * Switches the bottom navigation bar to [tab], in the way a tab bar is supposed to behave.
 *
 * The bar's destinations are siblings, not a history: `popUpTo` the start destination (saving the
 * state of the tab being left) keeps at most one tab on the stack on top of the home page, so the
 * system back button returns to the home page instead of walking back through the tabs, and
 * `launchSingleTop` plus `restoreState` re-use the entry of a tab the user returns to. Tapping the
 * current tab is a no-op.
 */
private fun NavHostController.switchTab(tab: HomeTab) {
    if (currentDestination?.route == tab.route) return
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Reads a required long route argument; null means the argument is absent or malformed. */
private fun NavBackStackEntry.longArg(key: String): Long? = arguments?.getString(key)?.toLongOrNull()

/** Reads the optional parent-folder argument: a negative id means "root", i.e. null. */
private fun NavBackStackEntry.parentIdArg(): Long? = longArg("parentId")?.takeIf { it >= 0 }

/** Onboarding screen shown when storage permission is missing: explains the purpose and offers an entry to grant it. */
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
