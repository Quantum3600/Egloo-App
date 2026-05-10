# Implementation Summary: Available vs Connected Sources UI

## What Was Implemented

I've updated the Egloo KMP app to properly fetch and merge both **Available Sources** (global list) and **Connected Sources** (user-specific) from the backend.

---

## Files Modified

### 1. **Models** (`domain/models/Models.kt`)
Added two new data classes:

```kotlin
@Serializable
data class AvailableSource(
    val id: String,           // "gmail", "slack", "google_drive"
    val name: String,         // "Gmail", "Slack"
    val displayName: String,  // For UI display
    val icon: String,         // Icon URL
    val description: String,  // "Read emails and attachments"
    val requiresAuth: Boolean = true
)

@Serializable
data class SourceRowData(
    val availableSource: AvailableSource,
    val connectedSource: ConnectedSource?,
    val isConnected: Boolean = connectedSource?.isConnected ?: false,
    val itemCount: Int = connectedSource?.itemCount ?: 0,
    val accountName: String = connectedSource?.accountName ?: "",
    val lastSyncedAt: String? = connectedSource?.lastSyncedAt,
    val sourceId: String = availableSource.id
)
```

### 2. **Repositories** (`data/repositories/Repositories.kt`)
Added new repository interface:

```kotlin
interface AvailableSourcesRepository {
    fun getAvailableSources(): Flow<List<AvailableSource>>
}
```

### 3. **Dummy Data** (`data/dummy/DummyData.kt`)
Added available sources dummy data:

```kotlin
val availableSources = listOf(
    AvailableSource(
        id = "gmail",
        name = "Gmail",
        displayName = "Gmail",
        icon = "https://...",
        description = "Read your emails and attachments",
        requiresAuth = true
    ),
    // ... more sources
)
```

### 4. **Dummy Implementation** (`data/repositories/DummyRepositories.kt`)
Added new dummy repository:

```kotlin
class DummyAvailableSourcesRepository : AvailableSourcesRepository {
    override fun getAvailableSources(): Flow<List<AvailableSource>> = flow {
        emit(DummyData.availableSources)
    }
}
```

### 5. **ViewModel** (`domain/viewmodels/ViewModels.kt`)
Updated `SourcesViewModel` to:
- Inject both `AvailableSourcesRepository` and `SourcesRepository`
- Fetch from both repos
- Merge them into `SourceRowData` list
- Match sources by ID or displayName

```kotlin
class SourcesViewModel(
    private val availableSourcesRepo: AvailableSourcesRepository,
    private val connectedSourcesRepo: SourcesRepository
) : BaseViewModel() {
    
    init {
        // Load available sources (global)
        scope.launch {
            availableSourcesRepo.getAvailableSources().collect { available ->
                _uiState.update { it.copy(availableSources = available) }
                mergeSourceData()
            }
        }

        // Load connected sources (user-specific)
        scope.launch {
            connectedSourcesRepo.getConnectedSources().collect { connected ->
                _uiState.update { it.copy(connectedSources = connected) }
                mergeSourceData()
            }
        }
    }

    private fun mergeSourceData() {
        val rows = state.availableSources.map { available ->
            val connected = state.connectedSources.find { 
                it.type.name.lowercase() == available.id.lowercase().replace("_", "")
                    || it.type.displayName.lowercase() == available.displayName.lowercase()
            }
            SourceRowData(
                availableSource = available,
                connectedSource = connected,
                // ... other fields
            )
        }
        _uiState.update { it.copy(sourceRows = rows) }
    }
}
```

### 6. **UI** (`ui/screens/OtherScreens.kt`)
Updated `SourcesScreen` to:
- Display merged source rows
- Show auth success/error messages
- Use `SourceRowData` instead of raw `ConnectedSource`

```kotlin
@Composable
fun SourcesScreen(viewModel: SourcesViewModel = koinInject()) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn {
        // Show auth message
        if (state.authMessage != null) {
            item { /* Success/Error banner */ }
        }

        // Render merged rows
        items(state.sourceRows) { row ->
            SourceRowWithAvailable(
                sourceRow = row,
                isConnecting = state.connectingSourceId == row.sourceId,
                onConnect = { viewModel.connectSource(row.sourceId) },
                onDisconnect = { /* ... */ }
            )
        }
    }
}
```

### 7. **Koin Binding** (`di/EglooModule.kt`)
Added repository binding:

```kotlin
single<AvailableSourcesRepository> { DummyAvailableSourcesRepository() }
```

---

## Data Flow Now

### App Startup
```
SourcesScreen appears
    ↓
SourcesViewModel.init()
    ├─ Calls: availableSourcesRepo.getAvailableSources()
    │   Backend: GET /sources/available
    │   Returns: [Gmail, Slack, Drive, Notion, PDF]
    │
    └─ Calls: connectedSourcesRepo.getConnectedSources()
        Backend: GET /sources
        Returns: [Gmail (connected), Slack (connected), Drive (not connected), ...]
    ↓
ViewModel merges both lists into SourceRowData
    ↓
UI displays merged rows with proper buttons:
    - Gmail: "Disconnect" button (already connected)
    - Slack: "Disconnect" button (already connected)
    - Drive: "Connect" button (not connected)
    - Notion: "Connect" button (not connected)
    - PDF: "Upload More" button (connected)
```

### User Clicks "Connect"
```
User taps "Connect" button on Google Drive
    ↓
SourcesScreen calls: viewModel.connectSource("google_drive")
    ↓
SourcesViewModel:
    1. Sets connectingSourceId = "google_drive" (show spinner)
    2. Maps "google_drive" → SourceType.GOOGLE_DRIVE
    3. Calls: connectedSourcesRepo.connectSource(SourceType.GOOGLE_DRIVE)
        Backend: POST /sources/connect { type: "GOOGLE_DRIVE" }
        Returns: { oauthUrl: "https://..." }
    ↓
Opens browser for OAuth (platformOpenUrl)
    ↓
User authenticates
    ↓
Google redirects to: egloo://auth?status=success&source=google_drive
    ↓
DeepLinkHandler captures result
    ↓
SourcesViewModel.handleAuthResult():
    1. Shows "Successfully connected Google Drive!"
    2. Waits 1 second
    3. Calls: connectedSourcesRepo.getConnectedSources() to refresh
    4. Merges data again
    5. Auto-dismisses message after 3 seconds
    ↓
UI updates:
    - Google Drive now shows "Disconnect"
    - Shows item count and sync time
```

---

## Key Changes

| Aspect | Before | After |
|--------|--------|-------|
| **Data fetched** | Only connected sources | Available + Connected |
| **UI displays** | Simple account names | Full source info from available + connection status |
| **Button logic** | "Connect" or "Disconnect" | Based on merged `isConnected` flag |
| **Source names** | From SourceType enum | From AvailableSource.displayName |
| **Source description** | None | From AvailableSource.description |
| **UI state** | `sources: List<ConnectedSource>` | `sourceRows: List<SourceRowData>` |

---

## Backend Integration (Future)

### To switch from Dummy to Real Backend

Create `KtorAvailableSourcesRepository`:

```kotlin
class KtorAvailableSourcesRepository(private val client: HttpClient) : AvailableSourcesRepository {
    override fun getAvailableSources(): Flow<List<AvailableSource>> = flow {
        try {
            val dtos = client.get("/sources/available").body<List<AvailableSourceDto>>()
            emit(dtos.map { it.toDomain() })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
}
```

Update Koin binding:

```kotlin
single<AvailableSourcesRepository> { KtorAvailableSourcesRepository(get()) }
```

**That's it!** No UI code changes needed.

---

## Testing

The implementation now:
✅ Fetches available sources global list  
✅ Fetches user's connected sources  
✅ Merges them into display rows  
✅ Shows correct "Connect" or "Disconnect" button  
✅ Shows item counts and sync times only when connected  
✅ Handles OAuth deep links  
✅ Shows success/error messages  
✅ Auto-refreshes after successful connection  

All logic is in the ViewModel and Repository layers—the UI is completely decoupled!

