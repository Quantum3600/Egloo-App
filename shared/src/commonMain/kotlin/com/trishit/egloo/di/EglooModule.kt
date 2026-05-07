package com.trishit.egloo.di

import com.russhwolf.settings.Settings
import com.trishit.egloo.data.api.createHttpClient
import com.trishit.egloo.domain.viewmodels.*
import com.trishit.egloo.data.repositories.*
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

// ─────────────────────────────────────────────────────────────────────────────
// EglooModule
//
// To migrate to real backend:
//   Replace "DummyXRepository()" with "KtorXRepository(get())" and add:
//   single { createHttpClient(BASE_URL) { get<TokenStore>().getToken() } }
// ─────────────────────────────────────────────────────────────────────────────

val eglooModule = module {

    // ── Infrastructure ────────────────────────────────────────────────────────
    single<Settings> { 
        object : Settings {
            private val map = mutableMapOf<String, Any>()
            override val keys: Set<String> get() = map.keys
            override val size: Int get() = map.size
            override fun clear() = map.clear()
            override fun remove(key: String) { map.remove(key) }
            override fun hasKey(key: String): Boolean = map.containsKey(key)
            override fun putString(key: String, value: String) { map[key] = value }
            override fun getString(key: String, defaultValue: String): String = map[key] as? String ?: defaultValue
            override fun getStringOrNull(key: String): String? = map[key] as? String
            override fun putInt(key: String, value: Int) { map[key] = value }
            override fun getInt(key: String, defaultValue: Int): Int = map[key] as? Int ?: defaultValue
            override fun getIntOrNull(key: String): Int? = map[key] as? Int
            override fun putLong(key: String, value: Long) { map[key] = value }
            override fun getLong(key: String, defaultValue: Long): Long = map[key] as? Long ?: defaultValue
            override fun getLongOrNull(key: String): Long? = map[key] as? Long
            override fun putFloat(key: String, value: Float) { map[key] = value }
            override fun getFloat(key: String, defaultValue: Float): Float = map[key] as? Float ?: defaultValue
            override fun getFloatOrNull(key: String): Float? = map[key] as? Float
            override fun putDouble(key: String, value: Double) { map[key] = value }
            override fun getDouble(key: String, defaultValue: Double): Double = map[key] as? Double ?: defaultValue
            override fun getDoubleOrNull(key: String): Double? = map[key] as? Double
            override fun putBoolean(key: String, value: Boolean) { map[key] = value }
            override fun getBoolean(key: String, defaultValue: Boolean): Boolean = map[key] as? Boolean ?: defaultValue
            override fun getBooleanOrNull(key: String): Boolean? = map[key] as? Boolean
        }
    }

    single { 
        createHttpClient("https://egloo-backend.onrender.com") { 
            get<AuthRepository>().getToken() 
        } 
    }

    // ── Repositories ──────────────────────────────────────────────────────────
    single<AuthRepository> { KtorAuthRepository(get(), get()) }
    single<DigestRepository> { KtorDigestRepository(get()) }
    single<ChatRepository> { KtorChatRepository(get()) }
    single<TopicsRepository> { KtorTopicsRepository(get()) }
    single<SourcesRepository> { KtorSourcesRepository(get()) }
    single<AvailableSourcesRepository> { KtorAvailableSourcesRepository(get()) }
    single<SavedRepository> { KtorSavedRepository(get()) }
    singleOf(::SettingsRepositoryImpl) bind SettingsRepository::class

    // ── ViewModels ────────────────────────────────────────────────────────────
    factoryOf(::AuthViewModel)
    factoryOf(::HomeViewModel)
    factoryOf(::ChatViewModel)
    factoryOf(::TopicsViewModel)
    factoryOf(::SourcesViewModel)
    factoryOf(::SettingsViewModel)
    factoryOf(::SavedViewModel)
}
