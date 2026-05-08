package com.trishit.egloo.platform

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences

actual fun createSettings(): Settings {
    val prefs = Preferences.userRoot().node("com/trishit/egloo")
    return PreferencesSettings(prefs)
}
