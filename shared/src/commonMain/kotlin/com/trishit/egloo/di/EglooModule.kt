package com.trishit.egloo.di

import coil3.SingletonImageLoader.get
import com.russhwolf.settings.Settings
import com.trishit.egloo.data.api.createHttpClient
import com.trishit.egloo.domain.viewmodels.*
import com.trishit.egloo.data.repositories.*
import com.trishit.egloo.platform.createSettings
import kotlinx.coroutines.NonCancellable.get
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.coroutines.EmptyCoroutineContext.get

// ─────────────────────────────────────────────────────────────────────────────
// EglooModule
//
// To migrate to real backend:
//   Replace "DummyXRepository()" with "KtorXRepository(get())" and add:
//   single { createHttpClient(BASE_URL) { get<TokenStore>().getToken() } }
// ─────────────────────────────────────────────────────────────────────────────

val eglooModule = module {

    // ── Infrastructure ────────────────────────────────────────────────────────
    val baseUrl = "https://dragging-endurable-sublet.ngrok-free.dev"
    single<Settings> { createSettings() }

    single { 
        createHttpClient(
            baseUrl = baseUrl,
            tokenProvider = { get<AuthRepository>().getToken() },
            refreshTokenProvider = { get<AuthRepository>().refreshToken() }
        )
    }

    // ── Repositories ──────────────────────────────────────────────────────────
    single<AuthRepository> { KtorAuthRepository(get(), get(), baseUrl) }
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
    factory { ChatViewModel(get()) }
    factoryOf(::TopicsViewModel)
    factoryOf(::SourcesViewModel)
    factoryOf(::SettingsViewModel)
    factoryOf(::SavedViewModel)
    factory { PdfViewModel(get(), get(), get()) }
    factoryOf(::BrainViewModel)
    single { IngestViewModel(get(), get()) }  // Changed to single to prevent multiple health monitoring loops
    factoryOf(::NotificationViewModel)
}
