# How Egloo Fetches Sources (Gmail, Slack, Google Drive)

> **IMPORTANT**: See [SOURCES_AVAILABLE_VS_CONNECTED.md](./SOURCES_AVAILABLE_VS_CONNECTED.md) for a critical distinction between:
> - **Available Sources** — the list of all supported sources (Gmail, Slack, etc.) returned by backend
> - **Connected Sources** — which of those sources the current user has linked to their account
>
> This guide focuses on the implementation, but both flows are important to understand!

---

## Overview

The app uses a **Repository Pattern** with Dependency Injection to fetch sources. Currently using dummy data, but the architecture is ready for real backend integration.

### Current Flow (Prototype)
```
SourcesScreen
    ↓
SourcesViewModel
    ↓
DummySourcesRepository (fake in-memory data)
    ↓
MutableStateFlow<List<ConnectedSource>> (emits changes)
    ↓
UI updates with connected sources
```

### Future Flow (With Backend)
```
SourcesScreen
    ↓
SourcesViewModel
    ↓
KtorSourcesRepository (real API calls)
    ↓
Http request to GET /sources
    ↓
Backend returns ConnectedSourceDto[]
    ↓
Convert to domain models
    ↓
StateFlow emits updates
    ↓
UI updates
```

---

## Current Architecture (Dummy Data)

### 1. Model Definition

**File**: `shared/src/commonMain/kotlin/com/trishit/egloo/domain/models/Models.kt`

```kotlin
@Serializable
data class ConnectedSource(
    val id: String,                    // unique source identifier
    val type: SourceType,              // GMAIL, SLACK, GOOGLE_DRIVE, etc.
    val accountName: String,           // "john@gmail.com" or "Team Workspace"
    val isConnected: Boolean,          // true if OAuth successful
    val lastSyncedAt: String? = null,  // "2 hours ago"
    val itemCount: Int = 0             // how many items synced from this source
)

enum class SourceType(val displayName: String) {
    GMAIL("Gmail"),
    SLACK("Slack"),
    DRIVE("Google Drive"),
    NOTION("Notion"),
    PDF("PDF Document"),
    GOOGLE_DRIVE("Google Drive"),
    MANUAL("Manual Entry")
}
```

### 2. Repository Interface

**File**: `shared/src/commonMain/kotlin/com/trishit/egloo/data/repositories/Repositories.kt`

```kotlin
interface SourcesRepository {
    fun getConnectedSources(): Flow<List<ConnectedSource>>
    suspend fun connectSource(type: SourceType)
    suspend fun disconnectSource(id: String)
}
```

**Key points**:
- `getConnectedSources()` returns a **Flow** (reactive stream) — automatically updates when sources change
- `connectSource()` — initiates OAuth authentication
- `disconnectSource()` — removes a source

### 3. Dummy Implementation

**File**: `shared/src/commonMain/kotlin/com/trishit/egloo/data/repositories/DummyRepositories.kt`

```kotlin
class DummySourcesRepository : SourcesRepository {
    private val _sources = MutableStateFlow(DummyData.connectedSources)

    override fun getConnectedSources(): Flow<List<ConnectedSource>> = _sources.asStateFlow()

    override suspend fun connectSource(type: SourceType) {
        delay(1500) // simulate OAuth
        val now = currentTimeMillis().toString()
        _sources.value = _sources.value.map { source ->
            if (source.type == type) {
                source.copy(
                    isConnected = true,
                    lastSyncedAt = now,
                    itemCount = 42  // fake data
                )
            } else source
        }
    }

    override suspend fun disconnectSource(id: String) {
        _sources.value = _sources.value.map { source ->
            if (source.id == id) {
                source.copy(isConnected = false, lastSyncedAt = null, itemCount = 0)
            } else source
        }
    }
}
```

**Mock data source**: `shared/src/commonMain/kotlin/com/trishit/egloo/data/dummy/DummyData.kt`

```kotlin
val connectedSources = listOf(
    ConnectedSource(
        id = "gmail_1",
        type = SourceType.GMAIL,
        accountName = "john@acmecorp.com",
        isConnected = true,
        lastSyncedAt = "2 hours ago",
        itemCount = 127
    ),
    ConnectedSource(
        id = "slack_1",
        type = SourceType.SLACK,
        accountName = "Acme Corp #general",
        isConnected = true,
        lastSyncedAt = "30 min ago",
        itemCount = 89
    ),
    ConnectedSource(
        id = "drive_1",
        type = SourceType.GOOGLE_DRIVE,
        accountName = "john@acmecorp.com",
        isConnected = false,  // not connected yet
        lastSyncedAt = null,
        itemCount = 0
    ),
    // ... more sources
)
```

### 4. ViewModel Integration

**File**: `shared/src/commonMain/kotlin/com/trishit/egloo/domain/viewmodels/ViewModels.kt`

```kotlin
data class SourcesUiState(
    val sources: List<ConnectedSource> = emptyList(),
    val connectingType: SourceType? = null,  // shows spinner during OAuth
    val authMessage: String? = null,         // success/error feedback
    val authMessageType: AuthMessageType? = null,
)

class SourcesViewModel(private val sourcesRepo: SourcesRepository) : BaseViewModel() {
    private val _uiState = MutableStateFlow(SourcesUiState())
    val uiState: StateFlow<SourcesUiState> = _uiState.asStateFlow()

    init {
        // Listen for sources changes from repository
        scope.launch {
            sourcesRepo.getConnectedSources().collect { sources ->
                _uiState.update { it.copy(sources = sources) }
            }
        }

        // Listen for OAuth deep link results (see DEEP_LINKING_GUIDE.md)
        scope.launch {
            DeepLinkHandler.authResultFlow.collect { result ->
                handleAuthResult(result)
            }
        }
    }

    fun connectSource(type: SourceType) {
        _uiState.update { it.copy(connectingType = type) }
        scope.launch {
            sourcesRepo.connectSource(type)
            _uiState.update { it.copy(connectingType = null) }
        }
    }

    fun disconnectSource(id: String) {
        scope.launch { sourcesRepo.disconnectSource(id) }
    }

    private fun handleAuthResult(result: DeepLinkHandler.AuthDeepLinkResult) {
        // When OAuth succeeds, show success message
        val messageType = if (result.status == "success") AuthMessageType.SUCCESS else AuthMessageType.ERROR
        val message = if (result.status == "success") {
            "Successfully connected ${result.source}!"
        } else {
            "Failed to connect ${result.source}. Please try again."
        }

        _uiState.update {
            it.copy(
                authMessage = message,
                authMessageType = messageType,
                connectingType = null
            )
        }

        // Auto-dismiss after 3 seconds
        scope.launch {
            delay(3000)
            _uiState.update { it.copy(authMessage = null, authMessageType = null) }
        }

        // Refresh sources list on success
        if (result.status == "success") {
            scope.launch {
                delay(1000)
                sourcesRepo.getConnectedSources().collect { sources ->
                    _uiState.update { it.copy(sources = sources) }
                }
            }
        }
    }
}
```

### 5. UI Display

**File**: `shared/src/commonMain/kotlin/com/trishit/egloo/ui/screens/OtherScreens.kt`

```kotlin
@Composable
fun SourcesScreen(viewModel: SourcesViewModel = koinInject()) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Sources", style = MaterialTheme.typography.displaySmall)
                Text("Connect your tools so Pingo can read them",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Render each source in the list
        items(state.sources) { source ->
            SourceRow(
                source = source,
                isConnecting = state.connectingType == source.type,
                onConnect = { viewModel.connectSource(source.type) },
                onDisconnect = { viewModel.disconnectSource(source.id) },
            )
        }
    }
}

@Composable
private fun SourceRow(
    source: ConnectedSource,
    isConnecting: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SourceDot(source.type, Modifier.size(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = source.type.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (source.isConnected) {
                        "${source.itemCount} items · synced ${source.lastSyncedAt ?: "never"}"
                    } else {
                        source.accountName
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            when {
                isConnecting -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                source.isConnected -> OutlinedButton(onClick = onDisconnect) {
                    Text("Disconnect", style = MaterialTheme.typography.labelMedium)
                }
                else -> Button(onClick = onConnect) {
                    Text("Connect", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
```

---

## Data Flow Visualization

### Step 1: App Launch
```
MainActivity/MainViewController
    ↓
Koin injects repositories
    ↓
SourcesViewModel.init{}
    ↓
sourcesRepo.getConnectedSources().collect { sources -> ... }
    ↓
DummySourcesRepository._sources = MutableStateFlow(connectedSources)
    ↓
Flow emits initial list
    ↓
SourcesScreen collects state
    ↓
LazyColumn renders SourceRow for each source
```

### Step 2: User Taps "Connect Gmail"
```
SourceRow("Connect" button)
    ↓
SourcesScreen calls: viewModel.connectSource(GMAIL)
    ↓
SourcesViewModel:
  - Sets connectingType = GMAIL (shows spinner)
  - Calls sourcesRepo.connectSource(GMAIL)
    ↓
DummySourcesRepository:
  - delay(1500) — simulate network latency
  - Updates _sources map: find GMAIL, set isConnected=true, itemCount=42
  - _sources.emit() triggers update
    ↓
SourcesViewModel collects updated sources
  - Sets connectingType = null (hide spinner)
    ↓
SourcesScreen re-renders
  - SourceRow now shows "Disconnect" button
  - Shows "42 items · synced now"
```

### Step 3: User Taps "Connect Gmail" (with Real Backend)
```
User taps button
    ↓
viewModel.connectSource(GMAIL)
    ↓
KtorSourcesRepository:
  - POST /sources/connect { "type": "GMAIL" }
  - Backend returns { "oauthUrl": "https://accounts.google.com/oauth/..." }
    ↓
Repository calls: platformOpenUrl(oauthUrl)
    ↓
Android/iOS opens browser or WebView
    ↓
User authenticates with Gmail
    ↓
Google redirects to: egloo://auth?status=success&source=gmail
    ↓
DeepLinkHandler.emitAuthResult("success", "gmail")
    ↓
SourcesViewModel.handleAuthResult()
  - Shows "Successfully connected Gmail!"
  - Polls GET /sources to refresh the list
  - Auto-dismisses message after 3s
    ↓
Updated sources display
```

---

## Backend Integration (When Ready)

### What the Backend Needs to Provide

**Endpoints**:

```
GET /sources
Response: [
  {
    "id": "gmail_1",
    "type": "GMAIL",
    "accountName": "john@example.com",
    "isConnected": true,
    "lastSyncedAt": "2024-05-07T14:30:00Z",
    "itemCount": 127
  },
  {
    "id": "slack_1",
    "type": "SLACK",
    "accountName": "Acme Corp",
    "isConnected": true,
    "lastSyncedAt": "2024-05-07T14:15:00Z",
    "itemCount": 89
  },
  ...
]

POST /sources/connect
Request: { "type": "GMAIL" }
Response: { "oauthUrl": "https://accounts.google.com/oauth/authorize?redirect_uri=egloo://auth?source=gmail" }

DELETE /sources/{id}
Response: 204 No Content

POST /sources/{id}/sync (optional)
Response: { "status": "syncing", "progress": 45 }
```

### Implementation Steps

1. **Create DTO classes** (ApiGuidelines.kt has examples):

```kotlin
@Serializable
data class ConnectedSourceDto(
    val id: String,
    val type: String,
    val accountName: String,
    val isConnected: Boolean,
    val lastSyncedAt: String?,
    val itemCount: Int
)

@Serializable
data class OAuthResponseDto(
    val oauthUrl: String
)
```

2. **Create extension function to convert DTO → Domain model**:

```kotlin
fun ConnectedSourceDto.toDomain() = ConnectedSource(
    id = id,
    type = SourceType.valueOf(type),
    accountName = accountName,
    isConnected = isConnected,
    lastSyncedAt = lastSyncedAt,
    itemCount = itemCount
)
```

3. **Create KtorSourcesRepository**:

```kotlin
class KtorSourcesRepository(private val client: HttpClient) : SourcesRepository {

    override fun getConnectedSources(): Flow<List<ConnectedSource>> = flow {
        try {
            val dtos = client.get("/sources").body<List<ConnectedSourceDto>>()
            emit(dtos.map { it.toDomain() })
        } catch (e: Exception) {
            emit(emptyList()) // or handle error
        }
    }

    override suspend fun connectSource(type: SourceType) {
        try {
            val response = client.post("/sources/connect") {
                setBody(mapOf("type" to type.name))
            }.body<OAuthResponseDto>()
            
            // Open OAuth URL in browser
            platformOpenUrl(response.oauthUrl)
        } catch (e: Exception) {
            // Handle error
        }
    }

    override suspend fun disconnectSource(id: String) {
        client.delete("/sources/$id")
    }
}
```

4. **Update Koin binding** in `EglooModule.kt`:

```kotlin
// Before:
singleOf(::DummySourcesRepository) bind SourcesRepository::class

// After:
singleOf(::KtorSourcesRepository) bind SourcesRepository::class
single { createHttpClient(BASE_URL) { get<TokenStore>().getToken() } }
```

**That's it!** No screen code changes needed.

---

## Complete Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        SourcesScreen (UI)                       │
│  - Displays list of ConnectedSource items                       │
│  - Renders "Connect" or "Disconnect" buttons                    │
│  - Shows loading spinner during OAuth                           │
│  - Shows success/error messages                                 │
└────────────────────────────┬────────────────────────────────────┘
                             │ collectAsState()
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                    SourcesViewModel                              │
│  - Manages SourcesUiState (sources, connectingType, messages)   │
│  - Listens to sourcesRepo.getConnectedSources() Flow            │
│  - Listens to DeepLinkHandler.authResultFlow for OAuth results  │
│  - Handles API calls: connectSource(), disconnectSource()       │
└────────────────────────────┬────────────────────────────────────┘
                             │ inject()
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                 SourcesRepository Interface                      │
│  - getConnectedSources(): Flow<List<ConnectedSource>>           │
│  - connectSource(type: SourceType)                              │
│  - disconnectSource(id: String)                                 │
└────────────────────────────┬────────────────────────────────────┘
                             │ 
        ┌────────────────────┴────────────────────┐
        │                                         │
        ↓ (Dummy)                                 ↓ (Real Backend)
┌──────────────────────┐              ┌──────────────────────────┐
│ DummySourcesRepository          │ KtorSourcesRepository      │
│                      │              │                          │
│ - _sources: StateFlow│              │ - client: HttpClient     │
│ - Returns fake data  │              │ - API calls to /sources  │
│ - Simulates delays   │              │ - Parses JSON responses  │
│   (1.5s for OAuth)   │              │ - Converts DTOs→Domain   │
└──────────────────────┘              └──────────────────────────┘
        │                                         │
        ↓                                         ↓
┌──────────────────────┐              ┌──────────────────────────┐
│   DummyData.kt       │              │   Backend Server          │
│ - connectedSources[] │              │   (FastAPI, Go, etc.)    │
│ - Mock Gmail source  │              │   - GET /sources          │
│ - Mock Slack source  │              │   - POST /sources/connect │
│ - Mock Drive source  │              │   - DELETE /sources/{id}  │
└──────────────────────┘              └──────────────────────────┘
```

---

## Summary

**How sources are fetched**:
1. **App startup**: SourcesViewModel injects SourcesRepository via Koin
2. **Repository pattern**: SourcesViewModel never directly calls backend — always goes through repository interface
3. **Flow-based**: Repository returns a `StateFlow` of sources; ViewModel collects changes reactively
4. **Currently dummy**: DummySourcesRepository returns hardcoded mock data with simulated delays
5. **When backend ready**: Swap to `KtorSourcesRepository` which makes real HTTP calls
6. **OAuth integration**: Uses deep linking (see DEEP_LINKING_GUIDE.md) to handle OAuth redirects
7. **No code changes needed**: UI layers (Screen, ViewModel) untouched when switching implementations

The architecture is **production-ready** and follows Kotlin Multiplatform best practices!
