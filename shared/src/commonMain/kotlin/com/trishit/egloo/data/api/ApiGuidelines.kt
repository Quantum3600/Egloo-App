package com.trishit.egloo.data.api

// =============================================================================
// EGLOO — KTOR API INTEGRATION GUIDE
// =============================================================================
//
// This file documents exactly how to wire up the real backend when it's ready.
// Every DummyXRepository maps 1-to-1 to a KtorXRepository.
// Swap in Koin (see EglooModule.kt) — no screen code changes needed.
//
// =============================================================================

// STEP 1: Configure the Ktor client
// ---------------------------------
// In shared/src/commonMain, create HttpClientFactory.kt:
//
//   fun createHttpClient(baseUrl: String, tokenProvider: () -> String?) = HttpClient {
//
//       install(ContentNegotiation) {
//           json(Json { ignoreUnknownKeys = true; isLenient = true })
//       }
//
//       install(Auth) {
//           bearer {
//               loadTokens { BearerTokens(tokenProvider() ?: "", "") }
//               refreshTokens { /* call your /auth/refresh endpoint */ }
//           }
//       }
//
//       install(HttpTimeout) {
//           requestTimeoutMillis = 30_000
//           connectTimeoutMillis = 10_000
//       }
//
//       install(Logging) {
//           level = if (BuildConfig.DEBUG) LogLevel.BODY else LogLevel.NONE
//       }
//   }
//
// Engine is platform-specific (see androidMain / iosMain / desktopMain).
//
// In androidMain/kotlin/com/egloo/data/api/HttpClientFactory.android.kt:
//   actual fun platformEngine() = Android
//
// In iosMain/kotlin/com/egloo/data/api/HttpClientFactory.ios.kt:
//   actual fun platformEngine() = Darwin
//
// In desktopMain/kotlin/com/egloo/data/api/HttpClientFactory.desktop.kt:
//   actual fun platformEngine() = CIO


// STEP 2: Expected API endpoints
// ------------------------------
// These are the endpoints your backend must expose. All return JSON.
//
//   GET  /digest/today
//        Response: DailyDigestDto
//
//   GET  /chat/history
//        Response: List<ChatMessageDto>
//
//   POST /chat/message
//        Body:    { "text": "..." }
//        Response: Server-Sent Events stream of ChatChunkDto
//                  (each chunk has { "delta": "...", "done": false })
//                  Final chunk: { "delta": "", "done": true, "sources": [...] }
//
//   GET  /topics
//        Response: List<TopicDto>
//
//   GET  /topics/{id}
//        Response: TopicDto
//
//   GET  /sources
//        Response: List<ConnectedSourceDto>
//
//   POST /sources/connect
//        Body:    { "type": "GMAIL" }
//        Response: { "oauthUrl": "https://..." }  → open in browser/WebView
//
//   DELETE /sources/{id}
//        Response: 204 No Content
//
//   GET  /settings
//        Response: AppSettingsDto
//
//   PUT  /settings
//        Body:    AppSettingsDto
//        Response: AppSettingsDto


// STEP 3: Create DTO classes
// --------------------------
// Create shared/src/commonMain/kotlin/com/egloo/data/api/Dtos.kt
//
//   @Serializable data class DailyDigestDto(
//       val greeting: String,
//       val dateLabel: String,
//       val pingoMessage: String,
//       val sections: List<DigestSectionDto>,
//       val totalItemCount: Int,
//   )
//   ... etc.
//
// Add mappers:  DailyDigestDto.toDomain() : DailyDigest
// Keep DTO <-> domain mapping OUT of the ViewModel.


// STEP 4: Implement KtorXRepository
// ----------------------------------
// Example for DigestRepository:
//
//   class KtorDigestRepository(private val client: HttpClient) : DigestRepository {
//
//       override fun getDailyDigest(): Flow<DigestResult> = flow {
//           emit(DigestResult.Loading)
//           try {
//               val dto = client.get("/digest/today").body<DailyDigestDto>()
//               emit(DigestResult.Success(dto.toDomain()))
//           } catch (e: Exception) {
//               emit(DigestResult.Error(e.message ?: "Unknown error"))
//           }
//       }
//   }
//
// For streaming chat (SSE), use:
//
//   client.preparePost("/chat/message") { setBody(MessageRequest(text)) }
//       .execute { response ->
//           val channel = response.bodyAsChannel()
//           while (!channel.isClosedForRead) {
//               val line = channel.readUTF8Line() ?: break
//               if (line.startsWith("data:")) {
//                   val chunk = json.decodeFromString<ChatChunkDto>(line.removePrefix("data:").trim())
//                   // emit partial text into _messages StateFlow
//               }
//           }
//       }


// STEP 5: Swap in Koin
// ---------------------
// In EglooModule.kt, change:
//
//   single<DigestRepository> { DummyDigestRepository() }
//   ↓
//   single<DigestRepository> { KtorDigestRepository(get()) }
//   single { createHttpClient(BASE_URL) { get<TokenStore>().getToken() } }
//
// That's the entire migration. ViewModels and screens are untouched.


// STEP 6: OAuth flow for Sources
// --------------------------------
// When the user taps "Connect Gmail":
//   1. Call POST /sources/connect { "type": "GMAIL" }
//   2. Backend returns { "oauthUrl": "..." }
//   3. Open that URL in the platform browser (expect platformOpenUrl() expect/actual)
//   4. Backend handles the OAuth callback and stores the token server-side
//   5. App polls GET /sources or listens to a WebSocket for status update
//
// The expect/actual for opening a URL:
//
//   // commonMain
//   expect fun platformOpenUrl(url: String)
//
//   // androidMain
//   actual fun platformOpenUrl(url: String) {
//       // use Intent.ACTION_VIEW
//   }
//
//   // desktopMain
//   actual fun platformOpenUrl(url: String) {
//       Desktop.getDesktop().browse(URI(url))
//   }
//
//   // iosMain
//   actual fun platformOpenUrl(url: String) {
//       UIApplication.sharedApplication.openURL(NSURL.URLWithString(url)!!)
//   }

// =============================================================================
// END OF GUIDE
// =============================================================================

// Placeholder object so this file compiles as a Kotlin source file.
object ApiGuidelines
