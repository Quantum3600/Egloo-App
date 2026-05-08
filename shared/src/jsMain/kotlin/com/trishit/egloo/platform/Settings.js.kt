package com.trishit.egloo.platform

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings
import kotlinx.browser.localStorage

actual fun createSettings(): Settings = StorageSettings(localStorage)
