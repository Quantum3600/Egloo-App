package com.trishit.egloo.platform

import com.russhwolf.settings.Settings

/**
 * Creates a platform-specific Settings instance.
 * - Android: EncryptedSharedPreferences
 * - iOS: Keychain
 * - JVM/Desktop: Preferences
 * - Wasm/JS: LocalStorage
 */
expect fun createSettings(): Settings
