# Sources Fetching — Architecture Diagrams

## Complete App Architecture for Sources Feature

```
┌─────────────────────────────────────────────────────────────────────┐
│                          ANDROID/iOS/WEB                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │                      PRESENTATION LAYER                      │  │
│  │  ┌────────────────────────────────────────────────────────┐ │  │
│  │  │ SourcesScreen (Compose)                                │ │  │
│  │  │ ┌──────────────────────────────────────────────────┐  │ │  │
│  │  │ │ LazyColumn {                                     │  │ │  │
│  │  │ │   items(state.sources) { source ->              │  │ │  │
│  │  │ │     SourceRow(                                   │  │ │  │
│  │  │ │       source,                                    │  │ │  │
│  │  │ │       onConnect = { vm.connectSource(...) }     │  │ │  │
│  │  │ │       onDisconnect = { vm.disconnectSource(...) }│  │ │  │
│  │  │ │     )                                            │  │ │  │
│  │  │ │   }                                              │  │ │  │
│  │  │ │ }                                                │  │ │  │
│  │  │ └──────────────────────────────────────────────────┘  │ │  │
│  │  └────────────────────────────────────────────────────────┘ │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                            ↓ collectAsState()                      │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │                      DOMAIN LAYER                            │  │
│  │  ┌────────────────────────────────────────────────────────┐ │  │
│  │  │ SourcesViewModel                                       │ │  │
│  │  │ ┌─── State ──────────────────────────────────────────┐│ │  │
│  │  │ │ uiState: StateFlow<SourcesUiState>                ││ │  │
│  │  │ │ ├─ sources: List<ConnectedSource>                 ││ │  │
│  │  │ │ ├─ connectingType: SourceType?  (spinner)         ││ │  │
│  │  │ │ ├─ authMessage: String?        (success toast)    ││ │  │
│  │  │ │ └─ authMessageType: AuthMessageType?              ││ │  │
│  │  │ └────────────────────────────────────────────────────┘│ │  │
│  │  │                                                        │ │  │
│  │  │ ┌─── Methods ────────────────────────────────────────┐│ │  │
│  │  │ │ init {                                             ││ │  │
│  │  │ │   // Listen to sources from repo                  ││ │  │
│  │  │ │   sourcesRepo.getConnectedSources().collect {...} ││ │  │
│  │  │ │   // Listen to OAuth deep links                   ││ │  │
│  │  │ │   DeepLinkHandler.authResultFlow.collect {...}    ││ │  │
│  │  │ │ }                                                  ││ │  │
│  │  │ │                                                    ││ │  │
│  │  │ │ fun connectSource(type: SourceType)               ││ │  │
│  │  │ │ fun disconnectSource(id: String)                  ││ │  │
│  │  │ │ fun handleAuthResult(result)                      ││ │  │
│  │  │ └────────────────────────────────────────────────────┘│ │  │
│  │  └────────────────────────────────────────────────────────┘ │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                          ↓ inject()                                 │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │                      DATA LAYER                              │  │
│  │  ┌────────────────────────────────────────────────────────┐ │  │
│  │  │ SourcesRepository Interface                           │ │  │
│  │  │ ┌──────────────────────────────────────────────────┐  │ │  │
│  │  │ │ fun getConnectedSources(): Flow<...>            │  │ │  │
│  │  │ │ suspend fun connectSource(type: SourceType)      │  │ │  │
│  │  │ │ suspend fun disconnectSource(id: String)         │  │ │  │
│  │  │ └──────────────────────────────────────────────────┘  │ │  │
│  │  └────────────────────────────────────────────────────────┘ │  │
│  │                                                               │  │
│  │  ┌──────────────────────┬──────────────────────────────────┐ │  │
│  │  │                      │                                  │ │  │
│  │  ↓ Current              ↓ Future                            │ │  │
│  │  │                      │                                  │ │  │
│  │  DummySourcesRepository KtorSourcesRepository             │ │  │
│  │  ├─ _sources:          ├─ client: HttpClient              │ │  │
│  │  │  MutableStateFlow    ├─ getConnectedSources():          │ │  │
│  │  ├─ return fake data    │  - GET /sources                  │ │  │
│  │  ├─ delay 1.5s          │  - parse JSON                    │ │  │
│  │  └─ update in-memory    ├─ connectSource():                │ │  │
│  │     list                │  - POST /sources/connect         │ │  │
│  │                         │  - open browser via platform     │ │  │
│  │                         └─ wait for OAuth callback         │ │  │
│  │                                                             │ │  │
│  │  └─────────────────────────────────────────────────────────┘ │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
                           ↓ (Backend Only)
┌─────────────────────────────────────────────────────────────────────┐
│                       BACKEND SERVER                                 │
│                                                                     │
│  Database: PostgreSQL                                              │
│  ┌───────────────────────────────────────────────────────────┐    │
│  │ users                           connected_sources         │    │
│  │ ├─ id (pk)                      ├─ id (pk)               │    │
│  │ ├─ email                        ├─ user_id (fk)          │    │
│  │ └─ created_at          <───────┤ ├─ source_type (Gmail)  │    │
│  │                                 │ ├─ account_name         │    │
│  │ knowledge_items                 │ ├─ is_connected: true   │    │
│  │ ├─ id (pk)                      │ ├─ oauth_token (hashed) │    │
│  │ ├─ source_id (fk) ─────────────→├─ last_synced_at        │    │
│  │ ├─ title                        │ └─ item_count: 127      │    │
│  │ ├─ content                      │                         │    │
│  │ └─ embeddings (vector)          └───────────────────────────┘   │
│  │                                                                   │
│  └───────────────────────────────────────────────────────────┘    │
│                                                                     │
│  API Endpoints:                                                    │
│  ├─ GET  /api/v1/sources              → returns connected sources │
│  ├─ POST /api/v1/sources/connect      → OAuth flow               │
│  │         request: { type: "GMAIL" }                             │
│  │         response: { oauthUrl: "..." }                          │
│  ├─ POST /api/v1/sources/callback     → OAuth callback (internal) │
│  │         Validates OAuth code & stores token securely           │
│  └─ DELETE /api/v1/sources/{id}       → disconnect source        │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Data Flow: User Opens App

```
┌─────────────────────────┐
│  App Starts             │
│  MainActivity.onCreate()│
└──────────────┬──────────┘
               │
               ↓
┌──────────────────────────────────────┐
│ Koin DI Container Initialized        │
│ - Loads EglooModule                  │
│ - Binds SourcesRepository            │
│   (currently: DummySourcesRepository)│
└──────────────┬───────────────────────┘
               │
               ↓
┌──────────────────────────────────────┐
│ RootComponent Created                │
│ - Navigation router                  │
│ - Decompose lifecycle                │
└──────────────┬───────────────────────┘
               │
               ↓
┌──────────────────────────────────────┐
│ User navigates to Sources tab        │
└──────────────┬───────────────────────┘
               │
               ↓
┌──────────────────────────────────────┐
│ SourcesScreen Composable Renders     │
│ - Calls koinInject<SourcesViewModel>()
└──────────────┬───────────────────────┘
               │
               ↓
┌──────────────────────────────────────┐
│ SourcesViewModel.init {}             │
│ - scope.launch {                     │
│     sourcesRepo                      │
│       .getConnectedSources()          │
│       .collect { sources ->           │
│           _uiState.update { ... }    │
│       }                               │
│   }                                  │
└──────────────┬───────────────────────┘
               │
               ↓
┌──────────────────────────────────────┐
│ DummySourcesRepository.getConnected()│
│ - Returns _sources.asStateFlow()     │
│ - _sources initialized with:         │
│   [Gmail, Slack, Drive, ...]         │
└──────────────┬───────────────────────┘
               │
               ↓
┌──────────────────────────────────────┐
│ Flow emits initial list              │
│ - Gmail (connected, 127 items)       │
│ - Slack (connected, 89 items)        │
│ - Drive (not connected)              │
│ - Notion (not connected)             │
└──────────────┬───────────────────────┘
               │
               ↓
┌──────────────────────────────────────┐
│ SourcesViewModel collects items      │
│ - Updates _uiState.sources           │
│ - New state emitted via StateFlow    │
└──────────────┬───────────────────────┘
               │
               ↓
┌──────────────────────────────────────┐
│ SourcesScreen collects uiState       │
│ - state.sources now = 4 sources      │
│ - Triggers recomposition             │
└──────────────┬───────────────────────┘
               │
               ↓
┌──────────────────────────────────────┐
│ LazyColumn renders                   │
│ items(state.sources) { source ->     │
│   SourceRow(...)                     │
│ }                                    │
│                                      │
│ ✓ Gmail source row displayed         │
│ ✓ Slack source row displayed         │
│ ✓ Drive source row displayed         │
│ ✓ Notion source row displayed        │
└──────────────────────────────────────┘
```

---

## Data Flow: User Connects Gmail

```
┌──────────────────────────────────────────┐
│ User taps "Connect" on Gmail row         │
└──────────────────┬───────────────────────┘
                   │
                   ↓
┌──────────────────────────────────────────┐
│ SourceRow.Button.onClick()               │
│ - Calls: viewModel.connectSource(GMAIL)  │
└──────────────────┬───────────────────────┘
                   │
                   ↓
┌──────────────────────────────────────────┐
│ SourcesViewModel.connectSource()          │
│ 1. _uiState.update {                     │
│      it.copy(connectingType = GMAIL)     │
│    }  ← shows spinner on Gmail row       │
│                                          │
│ 2. scope.launch {                        │
│      sourcesRepo.connectSource(GMAIL)    │
│    }                                     │
└──────────────────┬───────────────────────┘
                   │
                   ↓
┌──────────────────────────────────────────┐
│ (DUMMY PATH:)                            │
│ DummySourcesRepository.connectSource()   │
│                                          │
│ 1. delay(1500)  ← simulate OAuth        │
│                                          │
│ 2. _sources.value = _sources.value.map()│
│      Find GMAIL source                   │
│      Update: isConnected = true          │
│      Update: itemCount = 42              │
│      Update: lastSyncedAt = now          │
│                                          │
│ 3. StateFlow emits updated list          │
└──────────────────┬───────────────────────┘
                   │
                   ↓
┌──────────────────────────────────────────┐
│ SourcesViewModel collects updated sources│
│ - state.sources = [...updated...]        │
│ - Gmail source now shows:                │
│   - itemCount: 42                        │
│   - isConnected: true                    │
│   - "Disconnect" button                  │
└──────────────────┬───────────────────────┘
                   │
                   ↓
┌──────────────────────────────────────────┐
│ SourcesViewModel.connectSource() resumes │
│ - _uiState.update {                      │
│     it.copy(connectingType = null)       │
│   }  ← hides spinner                     │
└──────────────────┬───────────────────────┘
                   │
                   ↓
┌──────────────────────────────────────────┐
│ SourcesScreen recomposes                 │
│                                          │
│ BEFORE:                                  │
│ Gmail row: [●] Gmail | Disconnect button│
│           Loading spinner                │
│                                          │
│ AFTER:                                  │
│ Gmail row: [●] Gmail | Disconnect button│
│           "42 items · synced now"        │
└──────────────────────────────────────────┘
```

---

## Data Flow: Real Backend Connection (OAuth)

```
┌──────────────────────────────┐
│ User taps "Connect Gmail"    │
└──────────────┬───────────────┘
               │
               ↓
┌──────────────────────────────────────┐
│ SourcesViewModel.connectSource()      │
│ - Show spinner: connectingType=GMAIL  │
│ - Call: repo.connectSource(GMAIL)     │
└──────────────┬──────────────────────┘
               │
               ↓ (REAL PATH with Backend)
┌──────────────────────────────────────────────┐
│ KtorSourcesRepository.connectSource()         │
│ - POST /sources/connect { type: "GMAIL" }    │
└──────────────┬──────────────────────────────┘
               │
               ↓
┌──────────────────────────────────────────────┐
│ BACKEND /sources/connect route               │
│ - Generates OAuth secret                     │
│ - Builds Google OAuth URL:                   │
│   https://accounts.google.com/oauth/...      │
│   ?client_id=...                             │
│   &redirect_uri=egloo://auth?...             │
│ - Returns: { oauthUrl: "..." }               │
└──────────────┬──────────────────────────────┘
               │
               ↓
┌──────────────────────────────────────────────┐
│ KtorRepository receives response             │
│ - Calls: platformOpenUrl(oauthUrl)           │
└──────────────┬──────────────────────────────┘
               │
               ↓
        ┌──────┴────────┐
        │               │
        ↓ (Android)     ↓ (iOS)
    ┌────────────┐  ┌────────────┐
    │ Intent.    │  │ UIApp      │
    │ ACTION_VIEW│  │ openURL()  │
    └────┬───────┘  └────┬───────┘
         │               │
         └───────┬───────┘
                 │
                 ↓
        ┌──────────────────────┐
        │ Browser Opens        │
        │ Google Login Page    │
        └──────┬───────────────┘
               │
               ↓
        ┌──────────────────────┐
        │ User Logs Into Gmail │
        │ Grants Permissions   │
        └──────┬───────────────┘
               │
               ↓
        ┌──────────────────────────────────┐
        │ Google Redirects To:             │
        │ egloo://auth?                    │
        │  status=success&                 │
        │  source=gmail                    │
        └──────┬───────────────────────────┘
               │
               ↓
┌──────────────────────────────────────────────┐
│ OS Routes Deep Link to App                   │
│ (See DEEP_LINKING_GUIDE.md)                  │
└──────────────┬──────────────────────────────┘
               │
               ↓
        ┌──────────────────────┐
        │ MainActivity.        │
        │ onNewIntent(intent)  │
        │                      │
        │ Parses:              │
        │ status=success       │
        │ source=gmail         │
        └──────┬───────────────┘
               │
               ↓
┌──────────────────────────────────────────────┐
│ DeepLinkHandler.emitAuthResult()             │
│ - Emits to authResultFlow                    │
│ - (status="success", source="gmail")         │
└──────────────┬──────────────────────────────┘
               │
               ↓
┌──────────────────────────────────────────────┐
│ SourcesViewModel.init { ... }:               │
│ - Listening to DeepLinkHandler.authResultFlow│
│ - Collects: AuthDeepLinkResult               │
│ - Calls: handleAuthResult(result)            │
└──────────────┬──────────────────────────────┘
               │
               ↓
┌──────────────────────────────────────────────┐
│ handleAuthResult():                          │
│ 1. Show banner: "Successfully connected!"   │
│ 2. Clear connectingType (hide spinner)       │
│ 3. scope.launch {                            │
│      delay(1000)                             │
│      sourcesRepo.getConnectedSources()       │
│        .collect { ... }  ← refresh           │
│    }                                         │
│ 4. scope.launch {                            │
│      delay(3000)                             │
│      Clear authMessage (dismiss banner)      │
│    }                                         │
└──────────────┬──────────────────────────────┘
               │
               ↓ (Backend fetches gmail emails)
┌──────────────────────────────────────────────┐
│ BACKEND (in background):                     │
│ 1. Received OAuth code in redirect           │
│ 2. Exchanged code for access token           │
│ 3. Stored token securely                     │
│ 4. Started background sync job:              │
│    - Fetch Gmail emails                      │
│    - Extract sender, subject, body           │
│    - Generate embeddings                     │
│    - Store in knowledge_items                │
│    - Update itemCount = 127                  │
└──────────────┬──────────────────────────────┘
               │
               ↓
┌──────────────────────────────────────────────┐
│ SourcesViewModel refreshes:                  │
│ - GET /sources from backend                  │
│ - Gmail source now shows:                    │
│   - isConnected: true  ✓                     │
│   - itemCount: 127   (real data!)            │
│   - lastSyncedAt: "now"                      │
│ - UI updates immediately                     │
└──────────────┬──────────────────────────────┘
               │
               ↓
┌──────────────────────────────────────────────┐
│ Screen Renders:                              │
│                                              │
│ Gmail row:                                   │
│ [●] Gmail                                    │
│ john@gmail.com · 127 items · synced now      │
│ [Disconnect]                                 │
│                                              │
│ + "Successfully connected!" banner (3s)     │
└──────────────────────────────────────────────┘
```

---

## Key Points

1. **Reactive**: Uses Kotlin Flows for automatic updates
2. **Layered**: UI never talks to backend directly
3. **Testable**: Can swap DummyRepository with KtorRepository
4. **Type-safe**: Kotlin handles data validation
5. **Coroutine-based**: Async/await patterns built-in
6. **Deep-link aware**: OAuth fully integrated

---

## File Dependencies

```
SourcesScreen.kt
  ↓ imports
  ├→ SourcesViewModel
  ├→ SourcesUiState
  └→ ConnectedSource (model)

SourcesViewModel.kt
  ↓ imports
  ├→ SourcesRepository (interface)
  ├→ DeepLinkHandler
  └→ SourceType

DummySourcesRepository.kt
  ↓ imports
  ├→ SourcesRepository (interface)
  ├→ DummyData
  └→ ConnectedSource

KtorSourcesRepository.kt (future)
  ↓ imports
  ├→ SourcesRepository (interface)
  ├→ HttpClient
  ├→ ConnectedSourceDto
  └→ platformOpenUrl()
```

---

## Summary

This architecture is **plug-and-play**:
- **Now**: Dummy data flows through the same path
- **Later**: Real backend data flows through the same path
- **Result**: UI code never changes!

