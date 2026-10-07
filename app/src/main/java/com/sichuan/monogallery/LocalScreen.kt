package com.sichuan.monogallery

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

/**
 * 本地 tab page: the single card for the images the library does not own, which opens the 本地图片
 * page.
 *
 * The page builds its own [MonoScaffold] rather than sharing one with the other tabs: the tabs are
 * independent pages, so one of them growing its own layout (actions, content, extra state) never
 * touches the others.
 *
 * The scan behind the card is kicked off here rather than at app start, so the storage is only
 * walked once the user actually opens this tab; the card reports the scan's progress and result.
 *
 * The "⋮" menu holds the same refresh and sort entries as a library folder. Its sort choice is
 * [localSort] — the same preference the 本地图片 page reads — because those images are the list this
 * tab leads to.
 *
 * No back button: the tab pages are siblings switched by the bar, not stacked on top of each other,
 * so there is no parent to return to (the system back button goes back to the home page instead).
 */
@Composable
fun LocalScreen(
    localImages: LocalImagesState,
    localSort: SortPreference,
    onSelectTab: (HomeTab) -> Unit,
    onOpenLocalImages: () -> Unit,
    onOpenLocalImagesProperties: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(localImages) { localImages.scanOnce() }

    MonoScaffold(
        title = HomeTab.Local.label,
        onBack = null,
        actions = {
            MoreMenu(
                onRefresh = { scope.launch { localImages.scan() } },
                sortMode = localSort.sortMode,
                onSortModeChange = { localSort.updateSortMode(it) },
            )
        },
        bottomBar = { HomeBottomBar(selected = HomeTab.Local, onSelect = onSelectTab) },
    ) { innerPadding ->
        MonoGrid(modifier = Modifier.padding(innerPadding)) {
            item {
                PickerFolderCard(
                    name = LocalImagesFolderName,
                    countText = if (localImages.hasScanned) "${localImages.images.size} 项" else "扫描中…",
                    onClick = onOpenLocalImages,
                    onProperties = onOpenLocalImagesProperties,
                )
            }
        }
    }
}
