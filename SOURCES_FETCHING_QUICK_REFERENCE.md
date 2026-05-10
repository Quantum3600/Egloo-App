# Sources Fetching — Quick Reference

## TL;DR — How It Works

### Right Now (Prototype)
```
User opens app
    ↓
SourcesScreen appears
    ↓
SourcesViewModel calls sourcesRepo.getConnectedSources()
    ↓
DummySourcesRepository returns hardcoded list:
  - Gmail (john@acme.com) — 127 items, connected
  - Slack (Acme Corp) — 89 items, connected
  - Google Drive — not connected
    ↓
Screen renders the list with Connect/Disconnect buttons
```

### When Backend is Ready
```
Replace DummySourcesRepository with KtorSourcesRepository
    ↓
It makes real HTTP request: GET /sources
    ↓
Backend returns actual connected sources from database
    ↓
ViewModel processes response and updates UI
    ↓
That's it! Zero code changes to screens or ViewModels
```

---

## The 3-Layer Architecture

### Layer 1: UI (Compose)
- **SourcesScreen.kt** — Renders list of sources
- Calls `viewModel.connectSource()` or `viewModel.disconnectSource()`
- Displays loading spinner while connecting
- Shows success/error messages

### Layer 2: ViewModel (State Management)
- **SourcesViewModel.kt** — Manages UI state
- Injects `SourcesRepository` via Koin
- Calls repository methods to fetch/update sources
- Listens to `DeepLinkHandler` for OAuth results
- Never calls backend directly — always through repository

### Layer 3: Data (Repository)
- **SourcesRepository interface** — Defines the contract
- **DummySourcesRepository** — Fake implementation (current)
- **KtorSourcesRepository** — Real implementation (future)
- Only this layer talks to the backend

---

## Current Data (DummyData.kt)

```
Sources List:
├── Gmail
│   ├── ID: "gmail_1"
│   ├── Account: "john@acmecorp.com"
│   ├── Connected: true
│   ├── Items: 127
│   └── Last sync: "2 hours ago"
├── Slack
│   ├── ID: "slack_1"
│   ├── Account: "Acme Corp #general"
│   ├── Connected: true
│   ├── Items: 89
│   └── Last sync: "30 min ago"
├── Google Drive
│   ├── ID: "drive_1"
│   ├── Connected: false
│   └── Items: 0
└── Notion
    └── Not connected
```

---

## Connecting a Source (OAuth Flow)

### Current Flow (Dummy)
```
1. User taps "Connect Gmail"
2. SourcesViewModel.connectSource(GMAIL)
3. Shows spinner: connectingType = GMAIL
4. DummyRepository.connectSource() simulates:
   - delay(1500) — fake network wait
   - Updates _sources.value to mark Gmail as connected
   - Sets itemCount = 42
5. ViewModel hides spinner
6. UI updates to show "42 items · synced now"
7. "Disconnect" button appears
```

### Real Flow (With Backend)
```
1. User taps "Connect Gmail"
2. SourcesViewModel.connectSource(GMAIL) shows spinner
3. KtorRepository.connectSource(GMAIL):
   - POST /sources/connect { "type": "GMAIL" }
   - Backend returns { "oauthUrl": "https://accounts.google.com/..." }
4. App calls platformOpenUrl(oauthUrl)
   - Browser opens (Android/iOS)
   - User logs into Gmail
   - Grants permissions
5. Gmail redirects to: egloo://auth?status=success&source=gmail
6. DeepLinkHandler captures the deep link (see DEEP_LINKING_GUIDE.md)
7. SourcesViewModel.handleAuthResult():
   - Shows "Successfully connected Gmail!"
   - Calls sourcesRepo.getConnectedSources() to refresh
   - Message auto-dismisses after 3 seconds
8. UI updates with actual data from backend
```

---

## Key Files

| File | Purpose |
|------|---------|
| `Models.kt` | `ConnectedSource` data class + `SourceType` enum |
| `Repositories.kt` | `SourcesRepository` interface |
| `DummyRepositories.kt` | Fake implementation returning dummy data |
| `ViewModels.kt` | `SourcesViewModel` — handles state + API calls |
| `OtherScreens.kt` | `SourcesScreen` + `SourceRow` UI components |
| `EglooModule.kt` | Koin binding: which repository to inject |

---

## How Backend Data Flows In

```
Backend Database
    ↓
GET /sources API endpoint
    ↓
Returns: [
  {
    "id": "gmail_1",
    "type": "GMAIL",
    "accountName": "john@example.com",
    "isConnected": true,
    "lastSyncedAt": "2024-05-07T14:30:00Z",
    "itemCount": 127
  },
  ...
]
    ↓
KtorRepository receives JSON
    ↓
Converts to ConnectedSourceDto
    ↓
Calls .toDomain() to convert to ConnectedSource
    ↓
ViewModel collects from Flow
    ↓
UI renders in LazyColumn
```

---

## Supported Sources

```kotlin
enum class SourceType {
    GMAIL          → Google Mail
    SLACK          → Slack channels/DMs
    GOOGLE_DRIVE   → Google Drive documents
    NOTION         → Notion workspaces
    PDF            → Uploaded PDF files
    MANUAL         → User-entered text/notes
}
```

---

## Testing the Dummy Implementation

### In Android Studio
1. Run the Android app
2. Navigate to the **Sources** tab
3. See the hardcoded list of gmail, slack, drive
4. Tap **"Connect"** on Google Drive
5. Watch spinner appear for ~1.5 seconds
6. Item count changes to 42, "Disconnect" button appears

### In Xcode (iOS)
1. Build & run the iOS app
2. Same experience as Android

### In Browser (Web)
1. `./gradlew :wasmApp:wasmJsBrowserDevelopmentRun`
2. Open `localhost:8080`
3. Navigate to Sources tab
4. Same behavior (no real OAuth yet, but state management works)

---

## Swapping to Real Backend (Checklist)

- [ ] Create `KtorSourcesRepository` class
- [ ] Create Ktor `HttpClient` factory
- [ ] Create `OAuthResponseDto`, `ConnectedSourceDto`, `ConnectedSourceDto.toDomain()`
- [ ] Implement `GET /sources` endpoint call
- [ ] Implement `POST /sources/connect` endpoint call
- [ ] Update Koin binding: `DummySourcesRepository` → `KtorSourcesRepository`
- [ ] Test with real backend
- [ ] ✨ Zero UI code changes!

---

## State Management Flow

```
SourcesUiState {
  sources: List<ConnectedSource>        ← from repo
  connectingType: SourceType?            ← shows spinner during OAuth
  authMessage: String?                   ← "Successfully connected!"
  authMessageType: AuthMessageType?      ← SUCCESS or ERROR
}

When sources change in repo:
  repo.getConnectedSources().collect { sources ->
    _uiState.update { it.copy(sources = sources) }
  }

When user taps Connect:
  _uiState.update { it.copy(connectingType = GMAIL) }  ← show spinner
  repo.connectSource(GMAIL)
  _uiState.update { it.copy(connectingType = null) }   ← hide spinner

When OAuth succeeds:
  DeepLinkHandler emits result
  _uiState.update { it.copy(authMessage = "Success!") }
  delay(3000)
  _uiState.update { it.copy(authMessage = null) }      ← dismiss message
```

---

## Related Guides

- **[DEEP_LINKING_GUIDE.md](./DEEP_LINKING_GUIDE.md)** — How OAuth redirects work
- **[AGENTS.md](./AGENTS.md)** — Overall project architecture
- **[ApiGuidelines.kt](./shared/src/commonMain/kotlin/com/trishit/egloo/data/api/ApiGuidelines.kt)** — Step-by-step backend integration

---

## Questions?

**Q: How does data persist when I close the app?**
A: Currently it doesn't — dummy data resets. When backend is ready, sources are fetched from database every time.

**Q: Can users have multiple Gmail accounts?**
A: Data model supports it (each `ConnectedSource` has unique `id`). Backend needs to handle multiple OAuth logins per user.

**Q: Where is the sync job supposed to run?**
A: On the backend! Each source has `lastSyncedAt` which is set by the backend. Frontend just displays it. Real syncing (fetching emails, Slack messages) happens server-side.

**Q: How often should we sync?**
A: Configurable via Settings screen (`syncFrequencyHours`). Backend listens to this and adjusts its sync schedule.

**Q: What if OAuth fails?**
A: Deep link will include `status=error`. SourcesViewModel shows error banner, user can retry.

