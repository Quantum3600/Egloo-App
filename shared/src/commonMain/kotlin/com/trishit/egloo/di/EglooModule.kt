package com.trishit.egloo.di

import com.russhwolf.settings.Settings
import com.trishit.egloo.data.api.createHttpClient
import com.trishit.egloo.domain.viewmodels.*
import com.trishit.egloo.data.repositories.*
import com.trishit.egloo.platform.createSettings
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
    single<Settings> { createSettings() }

    single { 
        createHttpClient(
            baseUrl = "https://egloo-backend.onrender.com",
            tokenProvider = { get<AuthRepository>().getToken() },
            refreshTokenProvider = { get<AuthRepository>().refreshToken() }
        )
    }

    // ── Repositories ──────────────────────────────────────────────────────────
    single<AuthRepository> { KtorAuthRepository(get(), get()) }
    single<DigestRepository> { KtorDigestRepository(get()) }
    single<ChatRepository> { KtorChatRepository(get()) }
    single<TopicsRepository> { KtorTopicsRepository(get()) }
    single<SourcesRepository> { KtorSourcesRepository(get()) }
    single<AvailableSourcesRepository> { KtorAvailableSourcesRepository(get()) }
    single<SavedRepository> { KtorSavedRepository(get()) }
    single<PdfRepository> { KtorPdfRepository(get()) }
    single<BrainRepository> { KtorBrainRepository(get()) }
    single<IngestRepository> { KtorIngestRepository(get()) }
    single<HealthRepository> { KtorHealthRepository(get()) }
    single<NotificationRepository> { KtorNotificationRepository(get()) }
    singleOf(::SettingsRepositoryImpl) bind SettingsRepository::class

    // ── ViewModels ────────────────────────────────────────────────────────────
    factoryOf(::AuthViewModel)
    factoryOf(::HomeViewModel)
    factory { ChatViewModel(get(), get()) }
    factoryOf(::TopicsViewModel)
    factoryOf(::SourcesViewModel)
    factoryOf(::SettingsViewModel)
    factoryOf(::SavedViewModel)
    factory { PdfViewModel(get(), get()) }
    factoryOf(::BrainViewModel)
    single { IngestViewModel(get(), get()) }  // Changed to single to prevent multiple health monitoring loops
    factoryOf(::NotificationViewModel)
}
