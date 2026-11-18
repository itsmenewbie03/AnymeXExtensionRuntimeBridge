package com.anymex.desktop

import android.content.SharedPreferences
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat

/** Applies a saved value, or the value supplied through setDefaultValue when
 * the source has not persisted this preference yet. */
internal fun hydratePreference(preference: Preference, preferences: SharedPreferences) {
    val key = preference.key ?: return
    when (preference) {
        is ListPreference -> {
            val fallback = preference.defaultValue?.toString() ?: preference.value
            preference.value = preferences.getString(key, fallback)
        }
        is MultiSelectListPreference -> {
            val fallback = stringSet(preference.defaultValue) ?: preference.values
            preference.values = preferences.getStringSet(key, fallback)?.toMutableSet()
        }
        is SwitchPreferenceCompat -> {
            val fallback = preference.defaultValue as? Boolean ?: preference.isChecked
            preference.isChecked = preferences.getBoolean(key, fallback)
        }
        is EditTextPreference -> {
            val fallback = preference.defaultValue?.toString() ?: preference.text
            preference.text = preferences.getString(key, fallback)
        }
        is CheckBoxPreference -> {
            val fallback = preference.defaultValue as? Boolean ?: preference.isChecked
            preference.isChecked = preferences.getBoolean(key, fallback)
        }
    }
}

private fun stringSet(value: Any?): Set<String>? = when (value) {
    is Iterable<*> -> value.map { it.toString() }.toSet()
    is Array<*> -> value.map { it.toString() }.toSet()
    else -> null
}
