package com.sichuan.monogallery

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** SharedPreferences file holding the app's UI preferences. */
private const val PREFS_NAME = "monogallery_settings"

/** Preference key of the saved [SortMode] name. */
private const val KEY_SORT_MODE = "sort_mode"

/** Sort mode used when nothing has been saved yet. */
private val DEFAULT_SORT_MODE = SortMode.NAME

/**
 * Persisted UI preferences, currently the [SortMode] picked in the "⋮" menu.
 *
 * The choice is deliberately app-wide: it applies to every folder screen and to the home screen,
 * and it is written to disk, so moving between folders — or restarting the app — keeps the order
 * the user picked instead of falling back to [SortMode.NAME]. The mode is observable so menus and
 * grids recompose when it changes.
 */
class SortPreference(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** The active sort mode, restored from disk on construction. */
    var sortMode: SortMode by mutableStateOf(loadSortMode())
        private set

    /** Switches to [mode], refreshing the UI and persisting the choice for later launches. */
    fun updateSortMode(mode: SortMode) {
        if (mode == sortMode) return
        sortMode = mode
        prefs.edit().putString(KEY_SORT_MODE, mode.name).apply()
    }

    /** Reads the saved mode, falling back to the default when the value is missing or unknown. */
    private fun loadSortMode(): SortMode {
        val saved = prefs.getString(KEY_SORT_MODE, null) ?: return DEFAULT_SORT_MODE
        return SortMode.entries.firstOrNull { it.name == saved } ?: DEFAULT_SORT_MODE
    }
}
