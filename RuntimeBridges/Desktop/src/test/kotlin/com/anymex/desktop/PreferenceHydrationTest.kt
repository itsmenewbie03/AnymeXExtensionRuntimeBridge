package com.anymex.desktop

import android.content.DesktopSharedPreferences
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.SwitchPreferenceCompat
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class PreferenceHydrationTest {
    @TempDir
    lateinit var home: Path

    @Test
    fun `uses declared defaults when no settings file exists`() {
        val oldHome = System.getProperty("user.home")
        System.setProperty("user.home", home.toString())
        try {
            val stored = DesktopSharedPreferences("source_defaults")

            val switch = SwitchPreferenceCompat(null).apply {
                key = "switch"
                setDefaultValue(true)
            }
            val checkBox = CheckBoxPreference(null).apply {
                key = "checkbox"
                setDefaultValue(true)
            }
            val list = ListPreference(null).apply {
                key = "list"
                setDefaultValue("ass")
            }
            val multi = MultiSelectListPreference(null).apply {
                key = "multi"
                setDefaultValue(setOf("a", "b"))
            }
            val text = EditTextPreference(null).apply {
                key = "text"
                setDefaultValue("default text")
            }

            listOf(switch, checkBox, list, multi, text).forEach { hydratePreference(it, stored) }

            assertTrue(switch.isChecked)
            assertTrue(checkBox.isChecked)
            assertEquals("ass", list.value)
            assertEquals(setOf("a", "b"), multi.values)
            assertEquals("default text", text.text)
        } finally {
            System.setProperty("user.home", oldHome)
        }
    }

    @Test
    fun `saved values override declared defaults`() {
        val oldHome = System.getProperty("user.home")
        System.setProperty("user.home", home.toString())
        try {
            val stored = DesktopSharedPreferences("source_saved")
            assertTrue(stored.edit().putBoolean("switch", false).commit())
            val preference = SwitchPreferenceCompat(null).apply {
                key = "switch"
                setDefaultValue(true)
            }

            hydratePreference(preference, stored)

            assertFalse(preference.isChecked)
        } finally {
            System.setProperty("user.home", oldHome)
        }
    }

    @Test
    fun `corrupt settings are not silently overwritten`() {
        val oldHome = System.getProperty("user.home")
        System.setProperty("user.home", home.toString())
        try {
            val file = home.resolve("Documents/AnymeX/ExtensionSettings/source_corrupt.json")
            file.parent.createDirectories()
            file.writeText("{not valid json")

            val stored = DesktopSharedPreferences("source_corrupt")

            assertFalse(stored.edit().putBoolean("enabled", true).commit())
            assertEquals("{not valid json", file.readText())
        } finally {
            System.setProperty("user.home", oldHome)
        }
    }
}
