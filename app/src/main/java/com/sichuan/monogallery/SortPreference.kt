package com.sichuan.monogallery

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** SharedPreferences file holding the app's UI preferences. */
private const val PREFS_NAME = "monogallery_settings"

/** Preference key of the library's saved [SortMode] name, used when the caller names no key. */
private const val KEY_SORT_MODE = "sort_mode"

/** Preference key of the 本地图片 sort mode: those images are their own list with their own order. */
internal const val LOCAL_IMAGES_SORT_KEY = "local_images_sort_mode"

/** Sort mode used when nothing has been saved yet. */
private val DEFAULT_SORT_MODE = SortMode.NAME

/**
 * Persisted UI preferences, currently the [SortMode] picked in the "⋮" menu.
 *
 * One instance covers one list: the library screens share the default [key], and 本地图片 gets its own
 * instance with [LOCAL_IMAGES_SORT_KEY]. The choice is therefore remembered per list — putting the
 * local images in date order does not reorder the library's folders and vice versa — while still
 * surviving moving between folders, screens and app restarts instead of falling back to
 * [SortMode.NAME]. The mode is observable so menus and grids recompose when it changes.
 */
class SortPreference(context: Context, private val key: String = KEY_SORT_MODE) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** The active sort mode, restored from disk on construction. */
    var sortMode: SortMode by mutableStateOf(loadSortMode())
        private set

    /** Switches to [mode], refreshing the UI and persisting the choice for later launches. */
    fun updateSortMode(mode: SortMode) {
        if (mode == sortMode) return
        sortMode = mode
        prefs.edit().putString(key, mode.name).apply()
    }

    /** Reads the saved mode, falling back to the default when the value is missing or unknown. */
    private fun loadSortMode(): SortMode {
        val saved = prefs.getString(key, null) ?: return DEFAULT_SORT_MODE
        return SortMode.entries.firstOrNull { it.name == saved } ?: DEFAULT_SORT_MODE
    }
}
