package com.sichuan.monogallery

import androidx.compose.runtime.Composable

/**
 * 工具 tab page: blank placeholder behind the bottom bar.
 *
 * The page builds its own [MonoScaffold] rather than sharing one with the other tabs: the tabs are
 * independent pages, so one of them growing its own layout (actions, content, extra state) never
 * touches the others.
 *
 * No back button: the tab pages are siblings switched by the bar, not stacked on top of each other,
 * so there is no parent to return to (the system back button goes back to the home page instead).
 */
@Composable
fun ToolsScreen(onSelectTab: (HomeTab) -> Unit) {
    MonoScaffold(
        title = HomeTab.Tools.label,
        onBack = null,
        bottomBar = { HomeBottomBar(selected = HomeTab.Tools, onSelect = onSelectTab) },
    ) { _ ->
        // Deliberately empty: placeholder until the tab gets real content.
    }
}
