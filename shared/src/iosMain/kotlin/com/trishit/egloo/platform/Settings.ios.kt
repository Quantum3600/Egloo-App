package com.trishit.egloo.platform

import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings

actual fun createSettings(): Settings = KeychainSettings(service = "com.trishit.egloo")
