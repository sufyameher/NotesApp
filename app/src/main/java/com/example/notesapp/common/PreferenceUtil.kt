package com.example.notesapp.common

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.example.notesapp.App

object PreferenceUtil {

    private val prefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(App.context)
    }

    const val SORT_BY_KEY = "sort_by"
    const val SORT_ORDER_KEY = "sort_order"
    const val VIEW_MODE_KEY = "view_mode"

    var sortBy: SortBy
        get() = prefs.enumPref(SORT_BY_KEY, SortBy.DATE_CREATED)
        set(v) = prefs.setEnumPref(SORT_BY_KEY, v)

    var sortOrder: SortOrder
        get() = prefs.enumPref(SORT_ORDER_KEY, SortOrder.ASCENDING)
        set(v) = prefs.setEnumPref(SORT_ORDER_KEY, v)

    var viewMode: ViewMode
        get() = prefs.enumPref(VIEW_MODE_KEY, ViewMode.LIST)
        set(v) = prefs.setEnumPref(VIEW_MODE_KEY, v)

    fun registerChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.registerOnSharedPreferenceChangeListener(l)

    fun unregisterChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.unregisterOnSharedPreferenceChangeListener(l)
}

inline fun <reified T : Enum<T>> SharedPreferences.enumPref(key: String, default: T): T =
    getString(key, null)?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

fun <T : Enum<T>> SharedPreferences.setEnumPref(key: String, value: T) =
    edit().putString(key, value.name).apply()
