package com.sichuan.monogallery

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * The four destinations of the home bottom navigation bar, in display order. The enum itself is the
 * ordered list the bar renders and the single source of the tab route names, so adding a tab means
 * adding one entry here plus its `composable` in the nav graph.
 */
enum class HomeTab(val route: String, val label: String, val icon: ImageVector) {
    /** Mono: the home screen, i.e. the root directory listing. */
    Mono("home", "Mono", Icons.Filled.Home),

    /** Local: files on this device. */
    Local("local", "本地", Icons.Filled.Storage),

    /** Tools: the tool entries (compression and friends). */
    Tools("tools", "工具", Icons.Filled.Build),

    /** Settings: app settings. */
    Settings("settings", "设置", Icons.Filled.Settings),
}

/**
 * Bottom navigation bar of the home level: Mono (home), 本地, 工具, 设置.
 *
 * Each destination owns its own bar instance and passes its own [selected] tab — the current
 * destination is derived from the route (see the tab routes on [HomeTab]) instead of being held as
 * state, so the right tab is highlighted after any navigation, including a restart. [onSelect] is
 * expected to switch the destination; the bar itself only reports the tap.
 */
@Composable
fun HomeBottomBar(
    selected: HomeTab,
    onSelect: (HomeTab) -> Unit,
) {
    NavigationBar(
        // Flat white bar, matching the app background and the other bottom bars (no tonal tint).
        containerColor = ColorCard,
        tonalElevation = 0.dp,
    ) {
        HomeTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                // No content description: the item already carries its label, and a description on
                // the icon would replace that label in the merged semantics.
                icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
            )
        }
    }
}
