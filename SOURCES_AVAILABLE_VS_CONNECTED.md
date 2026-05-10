# How Backend Returns Available Sources

This clarifies the difference between **Available Sources** (what backend returns) and **Connected Sources** (user-specific).

---

## The Two Concepts

### Available Sources (Global)
- List of ALL sources that the app supports
- Configured on the backend (hardcoded or in database)
- Same for every user
- Examples: Gmail, Slack, Google Drive, Notion, PDF, Manual Entry
- Backend endpoint: `GET /sources/available`

### Connected Sources (User-Specific)
- Which of the available sources *this user* has connected to
- For each connected source: account name, item count, last sync time, oauth token (stored server-side)
- Different for every user
- Backend endpoint: `GET /sources`

---

## Data Flow

### Step 1: App Startup

```
┌─────────────────────────┐
│ User opens app          │
└──────────────┬──────────┘
               │
               ↓
┌──────────────────────────────────────┐
│ SourcesViewModel.init {}             │
│ - Calls: sourcesRepo.getAvailableSources()
│ - Calls: sourcesRepo.getConnectedSources()
└──────────────┬───────────────────────┘
               │
     ┌─────────┴─────────┐
     │                   │
     ↓                   ↓
┌──────────────────┐  ┌─────────────────────────┐
│ GET /sources/    │  │ GET /sources            │
│ available        │  │ (authenticated)         │
│                  │  │                         │
│ Backend returns: │  │ Backend returns:        │
│ [                │  │ [                       │
│  {               │  │  {                      │
│   id: "gmail",   │  │   id: "gmail_user123",  │
│   name: "Gmail", │  │   type: "GMAIL",        │
│   icon: "...",   │  │   accountName: "john...",
│   description:   │  │   isConnected: true,    │
│   "Read emails"  │  │   itemCount: 127,       │
│  },              │  │   lastSyncedAt: "now"   │
│  {               │  │  },                     │
│   id: "slack",   │  │  {                      │
│   name: "Slack", │  │   id: "slack_user123",  │
│   ...            │  │   type: "SLACK",        │
│  },              │  │   accountName: "Acme...",
│  {               │  │   isConnected: true,    │
│   id: "drive",   │  │   itemCount: 89,        │
│   ...            │  │   lastSyncedAt: "30m"   │
│  }               │  │  },                     │
│ ]                │  │  {                      │
│                  │  │   id: "drive_user123",  │
│                  │  │   type: "GOOGLE_DRIVE", │
│                  │  │   accountName: "john...",
│                  │  │   isConnected: false,   │
│                  │  │   itemCount: 0          │
│                  │  │  }                      │
│                  │  │ ]                       │
└──────────────────┘  └─────────────────────────┘
     │                   │
     └─────────┬─────────┘
               ↓
┌─────────────────────────────────────────┐
│ SourcesScreen merges data:              │
│ - Available sources from first call     │
│ - User's connected status from second   │
│ - Shows "Connect" or "Disconnect"       │
│   button based on isConnected flag      │
└─────────────────────────────────────────┘
```

---

## Backend Response Models

### GET /sources/available

**Purpose**: Return the list of all supported sources (same for every user)

```json
GET /api/v1/sources/available
Authorization: Bearer <token>

Response: [
  {
    "id": "gmail",
    "name": "Gmail",
    "displayName": "Gmail",
    "icon": "https://...",
    "description": "Read your emails and attachments",
    "requiresAuth": true,
    "scopes": ["emails", "attachments"]
  },
  {
    "id": "slack",
    "name": "Slack",
    "displayName": "Slack",
    "icon": "https://...",
    "description": "Sync messages and files from Slack",
    "requiresAuth": true,
    "scopes": ["messages", "files", "channels"]
  },
  {
    "id": "google_drive",
    "name": "Google Drive",
    "displayName": "Google Drive",
    "icon": "https://...",
    "description": "Index documents and PDFs",
    "requiresAuth": true,
    "scopes": ["files", "folders"]
  },
  {
    "id": "notion",
    "name": "Notion",
    "displayName": "Notion",
    "icon": "https://...",
    "description": "Connect to your Notion workspace",
    "requiresAuth": true,
    "scopes": ["databases", "pages"]
  },
  {
    "id": "pdf_upload",
    "name": "PDF Upload",
    "displayName": "Upload PDFs",
    "icon": "https://...",
    "description": "Upload PDF documents manually",
    "requiresAuth": false,
    "scopes": []
  }
]
```

### GET /sources

**Purpose**: Return the user's connected sources (user-specific)

```json
GET /api/v1/sources
Authorization: Bearer <token>

Response: [
  {
    "id": "gmail_user123",
    "type": "GMAIL",
    "sourceId": "gmail",           // ← maps to available source
    "accountName": "john@gmail.com",
    "isConnected": true,
    "oauthProviderAccount": "john@gmail.com",
    "itemCount": 127,
    "lastSyncedAt": "2024-05-07T14:30:00Z",
    "nextSyncAt": "2024-05-07T15:30:00Z",
    "syncStatus": "idle"           // or "syncing", "error"
  },
  {
    "id": "slack_user123",
    "type": "SLACK",
    "sourceId": "slack",
    "accountName": "Acme Corp Workspace",
    "isConnected": true,
    "oauthProviderAccount": "john@acme.com",
    "itemCount": 89,
    "lastSyncedAt": "2024-05-07T14:15:00Z",
    "nextSyncAt": "2024-05-07T15:15:00Z",
    "syncStatus": "idle"
  },
  {
    "id": "drive_user123",
    "type": "GOOGLE_DRIVE",
    "sourceId": "google_drive",
    "accountName": "john@gmail.com",
    "isConnected": false,          // ← User hasn't connected this yet
    "oauthProviderAccount": null,
    "itemCount": 0,
    "lastSyncedAt": null,
    "nextSyncAt": null,
    "syncStatus": "disconnected"
  },
  {
    "id": "notion_user123",
    "type": "NOTION",
    "sourceId": "notion",
    "accountName": null,
    "isConnected": false,
    "oauthProviderAccount": null,
    "itemCount": 0,
    "lastSyncedAt": null,
    "nextSyncAt": null,
    "syncStatus": "disconnected"
  },
  {
    "id": "pdf_user123",
    "type": "PDF",
    "sourceId": "pdf_upload",
    "accountName": "Uploaded Documents",
    "isConnected": true,          // ← Can upload without OAuth
    "oauthProviderAccount": null,
    "itemCount": 3,
    "lastSyncedAt": "2024-05-06T10:00:00Z",
    "nextSyncAt": null,
    "syncStatus": "idle"
  }
]
```

**Key observations**:
- Backend returns ALL sources (both connected and disconnected)
- Each entry has a `sourceId` that maps back to the available source
- `isConnected: false` means "user hasn't authenticated yet" but the source exists
- For disconnected sources: `itemCount=0`, `lastSyncedAt=null`, `oauthProviderAccount=null`

---

## Correct Data Flow

### When App Starts

```
SourcesScreen init
    ↓
SourcesViewModel.init {
    scope.launch {
        // Fetch available sources (list of what's possible)
        availableSourcesRepo.getAvailableSources().collect { available ->
            _uiState.update { it.copy(availableSources = available) }
        }
        
        // Fetch user's connected sources (which ones they've linked)
        sourcesRepo.getConnectedSources().collect { connected ->
            _uiState.update { it.copy(connectedSources = connected) }
        }
    }
}
    ↓
┌─ Available Sources from Backend ──────┬─ Connected Sources from Backend ────┐
│ Gmail                                 │ Gmail (john@gmail.com, 127 items)   │
│ Slack                                 │ Slack (Acme Corp, 89 items)         │
│ Google Drive                          │ Google Drive (not connected)         │
│ Notion                                │ Notion (not connected)              │
│ PDF Upload                            │ PDF (3 items uploaded)              │
└───────────────────────────────────────┴─────────────────────────────────────┘
    ↓
SourcesScreen renders:
├─ Gmail
│  ├─ Account: john@gmail.com (from connected)
│  ├─ Items: 127
│  ├─ Status: Connected ✓
│  └─ Button: [Disconnect]
├─ Slack
│  ├─ Account: Acme Corp Workspace
│  ├─ Items: 89
│  ├─ Status: Connected ✓
│  └─ Button: [Disconnect]
├─ Google Drive
│  ├─ Account: (from connected list - not connected yet)
│  ├─ Items: 0
│  ├─ Status: Connect to read documents
│  └─ Button: [Connect]
├─ Notion
│  ├─ Account: (available but not connected)
│  ├─ Items: 0
│  ├─ Status: Connect to sync your workspace
│  └─ Button: [Connect]
└─ PDF Upload
   ├─ Account: Uploaded Documents
   ├─ Items: 3
   ├─ Status: Connected ✓
   └─ Button: [Upload More]
```

---

## Updated ViewModels

### SourcesUiState

```kotlin
data class SourcesUiState(
    // Backend returns available sources (global list)
    val availableSources: List<AvailableSource> = emptyList(),
    
    // Backend returns user's connected sources
    val connectedSources: List<ConnectedSource> = emptyList(),
    
    // Merge them for UI
    val sourceRows: List<SourceRowData> = emptyList(),
    
    // UI state
    val connectingType: SourceType? = null,
    val authMessage: String? = null,
    val authMessageType: AuthMessageType? = null,
)

// The row data combines both
data class SourceRowData(
    val source: AvailableSource,           // from /sources/available
    val connectedStatus: ConnectedSource?, // from /sources (null if not connected)
    val isConnected: Boolean = connectedStatus != null,
    val itemCount: Int = connectedStatus?.itemCount ?: 0,
    val accountName: String = connectedStatus?.accountName ?: "",
    val lastSyncedAt: String? = connectedStatus?.lastSyncedAt,
)
```

### SourcesViewModel

```kotlin
class SourcesViewModel(
    private val availableSourcesRepo: AvailableSourcesRepository,
    private val connectedSourcesRepo: ConnectedSourcesRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(SourcesUiState())
    val uiState: StateFlow<SourcesUiState> = _uiState.asStateFlow()

    init {
        // Load available sources (what's supported)
        scope.launch {
            availableSourcesRepo.getAvailableSources().collect { available ->
                _uiState.update { it.copy(availableSources = available) }
                mergeSourceData()
            }
        }

        // Load connected sources (what user linked)
        scope.launch {
            connectedSourcesRepo.getConnectedSources().collect { connected ->
                _uiState.update { it.copy(connectedSources = connected) }
                mergeSourceData()
            }
        }

        // Listen for OAuth deep link results
        scope.launch {
            DeepLinkHandler.authResultFlow.collect { result ->
                handleAuthResult(result)
            }
        }
    }

    private fun mergeSourceData() {
        val state = _uiState.value
        val rows = state.availableSources.map { available ->
            val connected = state.connectedSources.find { it.sourceId == available.id }
            SourceRowData(
                source = available,
                connectedStatus = connected,
                isConnected = connected?.isConnected ?: false,
                itemCount = connected?.itemCount ?: 0,
                accountName = connected?.accountName ?: "",
                lastSyncedAt = connected?.lastSyncedAt,
            )
        }
        _uiState.update { it.copy(sourceRows = rows) }
    }

    fun connectSource(sourceId: String) {
        _uiState.update { it.copy(connectingType = SourceType.valueOf(sourceId.uppercase())) }
        scope.launch {
            connectedSourcesRepo.connectSource(sourceId) // POST /sources/connect
            _uiState.update { it.copy(connectingType = null) }
        }
    }

    private fun handleAuthResult(result: DeepLinkHandler.AuthDeepLinkResult) {
        // ... update UI and refresh sources list
        scope.launch {
            delay(1000)
            // Re-fetch connected sources (they now include the newly connected one)
            connectedSourcesRepo.getConnectedSources().collect { sources ->
                _uiState.update { it.copy(connectedSources = sources) }
                mergeSourceData()
            }
        }
    }
}
```

---

## Backend Implementation (Pseudocode)

### GET /sources/available

```python
# FastAPI example
@router.get("/sources/available")
async def get_available_sources(
    current_user: User = Depends(get_current_user)
):
    """
    Return all sources that this app supports.
    Same for every user.
    """
    return [
        {
            "id": "gmail",
            "name": "Gmail",
            "displayName": "Gmail",
            "icon": "https://example.com/gmail.png",
            "description": "Read your emails and attachments",
            "requiresAuth": True,
            "scopes": ["emails", "attachments"]
        },
        {
            "id": "slack",
            "name": "Slack",
            "displayName": "Slack",
            "icon": "https://example.com/slack.png",
            "description": "Sync messages and files",
            "requiresAuth": True,
            "scopes": ["messages", "files"]
        },
        # ... more sources
    ]
```

### GET /sources

```python
# FastAPI example
@router.get("/sources")
async def get_user_sources(
    current_user: User = Depends(get_current_user)
):
    """
    Return this user's source connections.
    Includes both connected and disconnected sources.
    For disconnected sources, it still returns the entry
    so the frontend knows which ones are available to connect.
    """
    # Get all sources this user might have
    user_sources = db.query(UserSource).filter_by(user_id=current_user.id).all()
    
    return [
        {
            "id": source.id,
            "type": source.type,  # "GMAIL", "SLACK", etc.
            "sourceId": source.source_id,  # maps to available source
            "accountName": source.account_name,  # "john@gmail.com"
            "isConnected": source.is_connected,
            "oauthProviderAccount": source.oauth_account,
            "itemCount": len(source.items),
            "lastSyncedAt": source.last_synced_at.isoformat() if source.last_synced_at else None,
            "nextSyncAt": source.next_sync_at.isoformat() if source.next_sync_at else None,
            "syncStatus": source.sync_status  # "idle", "syncing", "error", "disconnected"
        }
        for source in user_sources
    ]
```

---

## Corrected Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        SourcesScreen (UI)                       │
│  - Displays merged list of SourceRowData                        │
│  - Shows available sources with connection status               │
└────────────────────────────┬────────────────────────────────────┘
                             │ collectAsState()
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                    SourcesViewModel                              │
│  - Merges availableSources + connectedSources                   │
│  - Manages which are connected, itemCount, etc.                 │
│  - Listens to both repos + DeepLinkHandler                      │
└────────────────────────────┬────────────────────────────────────┘
                             │ inject()
                             ↓
        ┌────────────────────┴────────────────────┐
        │                                         │
        ↓                                         ↓
┌──────────────────────────┐         ┌──────────────────────────┐
│AvailableSourcesRepository│         │ConnectedSourcesRepository│
│                          │         │                          │
│- GET /sources/available  │         │- GET /sources            │
│- Returns: List<Available │         │- Returns: List<Connected│
│  Source>                 │         │  Source>                 │
│                          │         │                          │
│  Items are global        │         │ Items are user-specific  │
│  (same for all users)    │         │ (different per user)     │
└──────────────────────────┘         └──────────────────────────┘
        │                                    │
        └────────────────────┬───────────────┘
                             │
                             ↓
                    Backend Database
                    ┌─────────────────────┐
                    │ sources (global)    │
                    ├─ id: "gmail"        │
                    ├─ name: "Gmail"      │
                    ├─ icon: "..."        │
                    └─ ...                │
                    
                    ┌─────────────────────┐
                    │ user_sources        │
                    ├─ user_id (fk)       │
                    ├─ source_id (fk)     │
                    ├─ is_connected       │
                    ├─ oauth_token        │
                    ├─ account_name       │
                    └─ item_count         │
```

---

## Summary

**Backend returns TWO things**:

1. **Available Sources** (`GET /sources/available`)
   - List of all sources the app supports (Gmail, Slack, etc.)
   - Same for every user
   - Contains: name, icon, description, scopes
   - Frontend uses this to show what *can* be connected

2. **Connected Sources** (`GET /sources`)
   - User's personal source connections
   - Different for each user
   - Contains: which sources they've linked, account names, item counts, sync status
   - Frontend merges this with available sources to show:
     - Green checkmark if connected
     - "Connect" button if not connected
     - Item count and last sync time if connected

**Frontend merges both lists** to create the final UI where users can see what sources they have connected and what sources are available to connect.

