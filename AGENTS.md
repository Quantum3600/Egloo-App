# AGENTS.md — Egloo KMP Project

> **Purpose**: This file provides complete context for AI coding agents (Cursor, Claude, GitHub Copilot, etc.) working on the Egloo codebase. It documents architecture, structure, conventions, current state, and future roadmap so agents can make consistent, informed changes.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Project Structure](#project-structure)
4. [Design System & Theme](#design-system--theme)
5. [Screens & Navigation](#screens--navigation)
6. [Data Layer](#data-layer)
7. [Platform-Specific Code](#platform-specific-code)
8. [Build System](#build-system)
9. [Conventions & Patterns](#conventions--patterns)
10. [Current State](#current-state)
11. [Future Roadmap](#future-roadmap)
12. [Common Tasks](#common-tasks)
13. [Troubleshooting](#troubleshooting)

---

## Project Overview

**Egloo** is a Kotlin Multiplatform (KMP) personal knowledge management app that ingests data from Gmail, Slack, and Google Drive, uses LLM-powered clustering to organize information, and provides a conversational AI interface (Pingo the Penguin) for querying knowledge.

**Targets**:
- **Android** (minSdk 26, targetSdk 35)
- **iOS** (iOS 15+, via Xcode framework)
- **Desktop** (Windows, macOS, Linux via JVM)
- **Web** (Kotlin/Wasm, runs in all modern browsers with WasmGC)

**Current stage**: Prototype with dummy data. No backend integration yet.

**Tech stack**:
- Kotlin 2.1.0
- Compose Multiplatform 1.7.3
- Android Gradle Plugin 9.0.0-rc01
- Decompose 3.2.0 (navigation)
- Koin 4.0.0 (DI)
- Ktor 3.0.3 (HTTP client, not yet used)

---

## Architecture

### High-Level

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  - Compose UI (screens, components)                         │
│  - Navigation (Decompose)                                   │
│  - ViewModels (StateFlow)                                   │
└─────────────────────────────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                           │
│  - Models (data classes)                                    │
│  - Repository interfaces (contracts)                        │
│  - ViewModels (business logic, state management)            │
└─────────────────────────────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                       Data Layer                            │
│  - Repository implementations (DummyXRepository today,      │
│    KtorXRepository when backend is ready)                   │
│  - API clients (Ktor, not yet wired)                        │
│  - Local storage (DataStore via Multiplatform Settings)    │
└─────────────────────────────────────────────────────────────┘
```

### Key Principles

1. **Shared-first**: All business logic, UI, and state management live in `:shared` commonMain.
2. **Clean separation**: ViewModels never import Compose. Screens never import repositories directly.
3. **Interface-driven**: Repositories are defined as interfaces. Dummy and real implementations are swapped via Koin without touching screen code.
4. **Platform actuals for OS integration**: `platformOpenUrl()`, Ktor engines, and file I/O use expect/actual.
5. **No Android/iOS-specific UI**: All screens are Compose Multiplatform. Platform targets only provide entry points.

---

## Project Structure

```
egloo/
├── androidApp/                 ← Android application module (AGP 9.0)
│   ├── build.gradle.kts
│   └── src/main/
│       ├── kotlin/com/egloo/android/
│       │   ├── EglooApp.kt                ← Application class, Koin init
│       │   └── MainActivity.kt            ← Single activity, edge-to-edge
│       ├── res/
│       │   ├── values/
│       │   │   ├── colors.xml             ← Pre-Compose window background
│       │   │   └── themes.xml             ← Window chrome (transparent bars)
│       │   └── mipmap-*/                  ← Launcher icons
│       └── AndroidManifest.xml
│
├── desktopApp/                 ← JVM desktop module (Win/Mac/Linux)
│   ├── build.gradle.kts
│   └── src/jvmMain/kotlin/com/egloo/desktop/
│       └── main.kt                        ← ComposeWindowStyler entry point
│
├── wasmApp/                    ← Kotlin/Wasm web module
│   ├── build.gradle.kts
│   └── src/wasmJsMain/
│       ├── kotlin/com/egloo/wasm/
│       │   └── main.kt                    ← CanvasBasedWindow entry point
│       └── resources/
│           └── index.html                 ← Browser shell
│
├── shared/                     ← KMP module — all shared code
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/com/egloo/
│       │   ├── data/
│       │   │   ├── api/
│       │   │   │   └── ApiGuidelines.kt       ← Full Ktor migration guide
│       │   │   ├── dummy/
│       │   │   │   └── DummyData.kt           ← Fake data for prototype
│       │   │   └── repositories/
│       │   │       ├── Repositories.kt        ← Interfaces
│       │   │       └── DummyRepositories.kt   ← Fake implementations
│       │   ├── di/
│       │   │   └── EglooModule.kt             ← Koin bindings
│       │   ├── domain/
│       │   │   ├── models/
│       │   │   │   └── Models.kt              ← All data classes
│       │   │   └── viewmodels/
│       │   │       └── ViewModels.kt          ← StateFlow ViewModels
│       │   ├── navigation/
│       │   │   ├── RootComponent.kt           ← Decompose destinations
│       │   │   └── RootContent.kt             ← Compose root + nav UI
│       │   ├── platform/
│       │   │   └── PlatformOpenUrl.kt         ← expect/actual for URLs
│       │   └── ui/
│       │       ├── components/
│       │       │   └── Components.kt          ← Reusable UI elements
│       │       ├── screens/
│       │       │   ├── HomeScreen.kt
│       │       │   ├── ChatScreen.kt
│       │       │   ├── OtherScreens.kt        ← Topics, Sources, Settings
│       │       │   └── OnboardingScreen.kt
│       │       └── theme/
│       │           ├── EglooColors.kt         ← Brand palette
│       │           └── EglooTheme.kt          ← MaterialTheme wrappers
│       ├── androidMain/kotlin/com/egloo/
│       │   └── platform/
│       │       └── PlatformOpenUrl.android.kt
│       ├── iosMain/kotlin/com/egloo/
│       │   ├── ios/
│       │   │   └── MainViewController.kt      ← iOS entry point
│       │   └── platform/
│       │       └── PlatformOpenUrl.ios.kt
│       ├── desktopMain/kotlin/com/egloo/
│       │   └── platform/
│       │       └── PlatformOpenUrl.desktop.kt
│       └── wasmJsMain/kotlin/com/egloo/
│           └── platform/
│               └── PlatformOpenUrl.wasmJs.kt
│
├── gradle/
│   └── libs.versions.toml          ← Version catalog
├── build.gradle.kts                ← Root build file
├── settings.gradle.kts             ← Module includes
├── gradle.properties               ← JVM flags, AGP config
├── README.md                       ← User-facing docs
└── AGENTS.md                       ← This file
```

---

## Design System & Theme

### Brand Identity

**App name**: Egloo  
**Mascot**: Pingo the Penguin  
**Concept**: Pingo stores your knowledge in an igloo (safe, cold storage metaphor).

### Color Palette

Defined in `shared/src/commonMain/kotlin/com/egloo/ui/theme/EglooColors.kt`:

| Token              | Hex       | Usage                                  |
|--------------------|-----------|----------------------------------------|
| `TealPrimary`      | `#1D9E75` | Primary brand color (Pingo's scarf)    |
| `TealDark`         | `#0F6E56` | Darker variant                         |
| `TealLighter`      | `#9FE1CB` | Light tint                             |
| `TealSurface`      | `#E1F5EE` | Surface background (light theme)       |
| `BlueAccent`       | `#378ADD` | Secondary color (Arctic blue)          |
| `BlueDark`         | `#185FA5` | Darker variant                         |
| `BlueSurface`      | `#E6F1FB` | Surface background                     |
| `NightDeep`        | `#0E1A2E` | Dark theme background (igloo night)    |
| `NightMid`         | `#162336` | Mid-tone                               |
| `NightSurface`     | `#1E2F42` | Surface in dark mode                   |
| `NightCard`        | `#243548` | Card elevation                         |
| `SnowWhite`        | `#DAF0FA` | Light theme background                 |
| `SnowPure`         | `#F0F8FF` | Pure white                             |
| `BeakAmber`        | `#F5A623` | Accent/CTA (Pingo's beak)              |
| `GmailRed`         | `#EA4335` | Source badge                           |
| `SlackPurple`      | `#611f69` | Source badge                           |
| `DriveBlue`        | `#1967D2` | Source badge                           |

### Typography

Defined in `EglooTheme.kt`, follows Material 3 scale:

| Style             | Font Weight | Size  | Usage                                 |
|-------------------|-------------|-------|---------------------------------------|
| `displayLarge`    | 500         | 52sp  | Onboarding hero text                  |
| `displayMedium`   | 500         | 38sp  | Large headings                        |
| `displaySmall`    | 500         | 28sp  | "egloo" wordmark, greetings           |
| `headlineLarge`   | 500         | 22sp  | Screen titles                         |
| `headlineMedium`  | 500         | 18sp  | Section headers                       |
| `headlineSmall`   | 500         | 16sp  | Subsection headers                    |
| `titleLarge`      | 500         | 16sp  | Card titles                           |
| `titleMedium`     | 500         | 14sp  | List item titles                      |
| `bodyLarge`       | 400         | 14sp  | Default body text                     |
| `bodyMedium`      | 400         | 13sp  | Secondary body text                   |
| `labelLarge`      | 500         | 12sp  | Buttons                               |
| `labelMedium`     | 400         | 11sp  | Captions, metadata                    |

All fonts use system default (`system-ui, sans-serif` equivalent).

### Theme Modes

- **Dark (default)**: `EglooDarkColorScheme` — uses `NightDeep`, `TealPrimary`, `SnowPure` on dark.
- **Light**: `EglooLightColorScheme` — uses `SnowPure`, `TealDark`, `NightDeep` on light.

The app defaults to dark mode. Settings screen allows toggle.

### Component Tokens

Key reusable components in `ui/components/Components.kt`:

- **SourceBadge**: Colored pill with source label (Gmail, Slack, Drive, etc.)
- **SourceDot**: Small 8dp circle, colored by source type
- **KnowledgeCard**: Main content card with source dot, title, summary, project tag
- **PingoAvatar**: Circular "P" avatar (teal background)
- **ActionItemRow**: Amber-tinted row with dot and text
- **SectionHeader**: Title + subtitle pair
- **PingoMessageBubble**: Teal bubble with Pingo avatar + message
- **ProjectTag**: Rounded pill with project name

---

## Screens & Navigation

### Navigation Model

**Library**: Decompose 3.2.0  
**Pattern**: Single-activity (Android), single-window (desktop/web), single-view-controller (iOS)

**Destinations** (defined in `RootComponent.kt`):
- `Onboarding` — 4-page swipeable intro (only shown on first launch)
- `Home` — Daily digest, Pingo greeting, action items
- `Chat` — Ask Pingo anything (conversational search)
- `Topics` — Auto-generated topic clusters
- `Sources` — Connect/disconnect Gmail, Slack, Drive
- `Settings` — Theme, notifications, sync frequency

**Bottom navigation** (mobile): 5 tabs — Home, Chat, Topics, Sources, Settings  
**Side rail** (desktop): Same 5 items in a vertical nav rail  
**Onboarding**: Full-screen, no chrome

### Screen Details

#### 1. **OnboardingScreen**

**File**: `ui/screens/OnboardingScreen.kt`

**Pages**:
1. Welcome — "Meet Pingo"
2. How it works — 3-step illustration
3. Privacy — "Your data stays yours"
4. Ready — CTA to continue

**State**: Uses `remember { mutableStateOf(page) }` to track current page.  
**Navigation**: "Continue" advances, "Let's go" → `navigateTo(Destination.Home)`.

**UI elements**: Large emoji placeholder for Pingo, page dots, primary button, skip button (page 0 only).

#### 2. **HomeScreen**

**File**: `ui/screens/HomeScreen.kt`  
**ViewModel**: `HomeViewModel`  
**State**: `HomeUiState(isLoading, digest, error)`

**Layout**:
- Greeting (date + "Good morning")
- Pingo message bubble
- 3 stat chips (items read, topics, actions)
- Digest sections (each has title, subtitle, items, optional action bullets)
- Each item is a `KnowledgeCard`

**ViewModel behaviour**:
- `init` calls `loadDigest()`
- Emits `DigestResult.Loading` → `Success(digest)` → `Error(msg)`
- UI shows loading spinner, digest content, or error + retry button

#### 3. **ChatScreen**

**File**: `ui/screens/ChatScreen.kt`  
**ViewModel**: `ChatViewModel`  
**State**: `ChatUiState(messages, inputText, isSending)`

**Layout**:
- Header (Pingo avatar, "Ask Pingo", clear button)
- `LazyColumn` of `ChatMessage` bubbles (user right-aligned, Pingo left-aligned)
- Sticky input bar at bottom (TextField + send button)
- Empty state (centered Pingo avatar, suggestion chips)

**Message types**:
- `ChatMessage.User` → blue bubble, right-aligned
- `ChatMessage.Pingo` → grey bubble, left-aligned, optional source chips below

**Streaming**: `isStreaming = true` shows thinking dots; when done, replaced with final text.

#### 4. **TopicsScreen**

**File**: `ui/screens/OtherScreens.kt` (line ~1)  
**ViewModel**: `TopicsViewModel`  
**State**: `TopicsUiState(isLoading, topics, selectedTopic)`

**Layout**:
- Header ("Topics", count subtitle)
- `LazyVerticalGrid` of `TopicCard` (aspect ratio 1.1, colored by `TopicColor`)
- Each card shows source dots, title, item count, last updated
- Tap opens `TopicDetailSheet` (ModalBottomSheet with full topic info)

#### 5. **SourcesScreen**

**File**: `ui/screens/OtherScreens.kt` (line ~100)  
**ViewModel**: `SourcesViewModel`  
**State**: `SourcesUiState(sources, connectingType)`

**Layout**:
- Header ("Sources", subtitle)
- List of `SourceRow` (Gmail, Slack, Drive, Notion, PDF)
- Each row: source dot, name, sync status, Connect/Disconnect button
- When connecting: spinner replaces button

**OAuth flow** (future):
1. Tap "Connect Gmail"
2. `viewModel.connectSource(GMAIL)`
3. Backend returns OAuth URL
4. `platformOpenUrl(url)` opens browser
5. User authenticates
6. Redirect back to app (deep link on Android/iOS, same-tab on web)
7. Backend stores token, app polls `/sources` for updated state

#### 6. **SettingsScreen**

**File**: `ui/screens/OtherScreens.kt` (line ~200)  
**ViewModel**: `SettingsViewModel`  
**State**: `SettingsUiState(settings, isSaved)`

**Sections**:
- **Appearance**: Dark theme toggle
- **Pingo**: Morning greetings toggle
- **Sync**: Digest notifications toggle, sync frequency chips (1h, 3h, 6h, 12h, 24h)

**State persistence**: Uses `SettingsRepository` → `Multiplatform Settings` (DataStore equivalent).

When a setting changes, `viewModel.updateSettings()` saves, shows "Settings saved" banner for 1.5s.

---

## Data Layer

### Repository Pattern

**Interfaces** (in `data/repositories/Repositories.kt`):
- `DigestRepository` → `getDailyDigest(): Flow<DigestResult>`
- `ChatRepository` → `getChatHistory()`, `sendMessage(text)`, `clearHistory()`
- `TopicsRepository` → `getTopics()`, `getTopicById(id)`
- `SourcesRepository` → `getConnectedSources()`, `connectSource(type)`, `disconnectSource(id)`
- `SettingsRepository` → `getSettings()`, `updateSettings(settings)`

**Current implementations** (in `DummyRepositories.kt`):
- `DummyDigestRepository` → emits fake `dummyDigest` after 900ms delay
- `DummyChatRepository` → stores messages in `MutableStateFlow`, generates fake Pingo answers based on keywords
- `DummyTopicsRepository` → emits `dummyTopics` after 400ms delay
- `DummySourcesRepository` → stores sources in `MutableStateFlow`, simulates OAuth with 1.5s delay
- `DummySettingsRepository` → stores settings in `MutableStateFlow`

**Future implementations** (to be created):
- `KtorDigestRepository`, `KtorChatRepository`, etc.
- All will implement the same interfaces
- Swap in `EglooModule.kt` with zero screen changes

### Dummy Data

All fake data is in `data/dummy/DummyData.kt`:

- `dummySources` — 5 sources (3 connected, 2 not)
- `dummyItems` — 8 knowledge items (emails, Slack messages, Drive docs)
- `dummyDigest` — greeting, Pingo message, 3 sections, action items
- `dummyChatMessages` — 4 messages (2 user, 2 Pingo)
- `dummyTopics` — 6 topics (Project Alpha, Infrastructure, Hiring, etc.)
- `dummySavedItems` — filtered subset of `dummyItems`
- `dummySettings` — default app settings

### Models

All data classes in `domain/models/Models.kt`:

**Enums**:
- `SourceType` (GMAIL, SLACK, GOOGLE_DRIVE, NOTION, PDF, MANUAL)
- `TopicColor` (TEAL, BLUE, AMBER, CORAL, PURPLE)

**Data classes**:
- `ConnectedSource` — id, type, accountName, isConnected, lastSyncedAt, itemCount
- `KnowledgeItem` — id, title, summary, sourceType, sourceLabel, createdAt, projectTag, isActionItem, isSaved
- `DigestSection` — title, subtitle, items, actionItems
- `DailyDigest` — greeting, dateLabel, pingoMessage, sections, totalItemCount
- `ChatSource` — label, type (for source chips below Pingo bubbles)
- `ChatMessage` — sealed class with `User` and `Pingo` subtypes
- `Topic` — id, title, summary, itemCount, sources, lastUpdatedAt, color
- `AppSettings` — darkTheme, pingoGreetingsEnabled, digestNotificationsEnabled, syncFrequencyHours, userName

### Dependency Injection

**Library**: Koin 4.0.0  
**Module**: `di/EglooModule.kt`

Current bindings:
```kotlin
singleOf(::DummyDigestRepository)  bind DigestRepository::class
singleOf(::DummyChatRepository)    bind ChatRepository::class
singleOf(::DummyTopicsRepository)  bind TopicsRepository::class
singleOf(::DummySourcesRepository) bind SourcesRepository::class
singleOf(::DummySettingsRepository) bind SettingsRepository::class

factoryOf(::HomeViewModel)
factoryOf(::ChatViewModel)
factoryOf(::TopicsViewModel)
factoryOf(::SourcesViewModel)
factoryOf(::SettingsViewModel)
```

**To migrate to backend**:
1. Create `KtorXRepository` classes implementing the same interfaces
2. Change `singleOf(::DummyDigestRepository)` → `singleOf(::KtorDigestRepository)`
3. Add `single { createHttpClient(...) }`
4. Zero screen code changes

---

## Platform-Specific Code

### Expect/Actual Pattern

**Purpose**: Call platform APIs (browser, Android Intent, iOS UIKit, Desktop AWT) from shared code.

**Current usage**: `platformOpenUrl(url: String)`

**Location**:
- `commonMain`: `platform/PlatformOpenUrl.kt` (expect declaration)
- `androidMain`: `platform/PlatformOpenUrl.android.kt` (uses `Intent.ACTION_VIEW`)
- `iosMain`: `platform/PlatformOpenUrl.ios.kt` (uses `UIApplication.openURL`)
- `desktopMain`: `platform/PlatformOpenUrl.desktop.kt` (uses `Desktop.browse`)
- `wasmJsMain`: `platform/PlatformOpenUrl.wasmJs.kt` (uses `window.open`)

**Usage**: Called by `SourcesViewModel` when backend returns OAuth URL.

### Platform Entry Points

#### Android

**File**: `androidApp/src/main/kotlin/com/egloo/android/MainActivity.kt`

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        startKoin { ... }
        val root = DefaultRootComponent(defaultComponentContext(), isFirstLaunch())
        setContent { RootContent(root) }
    }
}
```

**Koin init**: `EglooApp.kt` also calls `startKoin` to catch the Application lifecycle.  
**First launch**: Uses `SharedPreferences` key `"onboarding_done"`.

#### iOS

**File**: `shared/src/iosMain/kotlin/com/egloo/ios/MainViewController.kt`

```kotlin
fun MainViewController(): UIViewController {
    startKoin { ... }
    val root = DefaultRootComponent(DefaultComponentContext(ApplicationLifecycle()), iosIsFirstLaunch())
    return ComposeUIViewController { RootContent(root) }
}
```

**Usage**: Called from Swift in `iOSApp.swift`:
```swift
import Shared
struct ContentView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
}
```

**First launch**: Uses `NSUserDefaults` key `"onboarding_done"`.

#### Desktop

**File**: `desktopApp/src/jvmMain/kotlin/com/egloo/desktop/main.kt`

```kotlin
fun main() = application {
    startKoin { ... }
    val root = DefaultRootComponent(DefaultComponentContext(LifecycleRegistry()), isFirstLaunch())
    Window(..., transparent = true) {
        WindowStyle(isDarkTheme = true, backdropType = WindowBackdrop.Mica, ...)
        DesktopRootContent(root)
    }
}
```

**ComposeWindowStyler**: Requires `transparent = true` on `Window()` so the OS backdrop shows through.  
**Mica**: Windows 11 translucent wallpaper-tinted backdrop. Falls back gracefully on Win10/Linux.  
**macOS**: No Mica (library doesn't support macOS vibrancy), but window still works correctly.  
**First launch**: Uses Java `Preferences.userRoot()` key `"onboarding_done"`.

#### Web

**File**: `wasmApp/src/wasmJsMain/kotlin/com/egloo/wasm/main.kt`

```kotlin
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin { ... }
    val root = DefaultRootComponent(DefaultComponentContext(LifecycleRegistry()), wasmIsFirstLaunch())
    CanvasBasedWindow(canvasElementId = "ComposeTarget") { RootContent(root) }
}
```

**CanvasBasedWindow**: Renders into `<canvas id="ComposeTarget">` in `index.html`.  
**First launch**: Uses browser `localStorage` key `"egloo_onboarding_done"`.

---

## Build System

### Gradle Version Catalog

**File**: `gradle/libs.versions.toml`

Key versions:
- `agp = "9.0.0-rc01"`
- `kotlin = "2.1.0"`
- `compose-multiplatform = "1.7.3"`
- `decompose = "3.2.0"`
- `koin = "4.0.0"`
- `ktor = "3.0.3"`
- `coil = "3.0.4"`
- `compose-window-styler = "0.5.3"`

**Important**: `compose-window-styler` group is `com.mayakapps.compose:window-styler` (NOT `dev.datlag.kcef`).

### Module Dependencies

```
:shared
  └─ Consumed by: androidApp, desktopApp, wasmApp
  └─ Targets: androidTarget, jvm(desktop), iosX64/Arm64/SimulatorArm64, wasmJs

:androidApp
  └─ Depends on: :shared
  └─ Type: Android application module (AGP 9.0)

:desktopApp
  └─ Depends on: :shared
  └─ Type: Kotlin JVM application (Compose Desktop)

:wasmApp
  └─ Depends on: :shared
  └─ Type: Kotlin/Wasm application

:iosApp (not a Gradle module)
  └─ Xcode project that links against :shared.framework
```

### Gradle Tasks

**Android**:
- `./gradlew :androidApp:installDebug` — install debug APK
- `./gradlew :androidApp:assembleRelease` — build release APK

**Desktop**:
- `./gradlew :desktopApp:run` — run locally
- `./gradlew :desktopApp:packageDistributionForCurrentOS` — DMG/MSI/DEB

**Web**:
- `./gradlew :wasmApp:wasmJsBrowserDevelopmentRun` — dev server at `localhost:8080`
- `./gradlew :wasmApp:wasmJsBrowserDistribution` — production bundle

**iOS**:
- Build `:shared` framework: `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`
- Then run from Xcode

### Configuration Files

**gradle.properties**:
- `org.gradle.jvmargs=-Xmx4g` — heap size for large KMP builds
- `android.useAndroidX=true`
- `kotlin.mpp.enableCInteropCommonization=true`
- `org.jetbrains.compose.experimental.uikit.enabled=true` — required for iOS Compose

**settings.gradle.kts**:
- Includes `:shared`, `:androidApp`, `:desktopApp`, `:wasmApp`
- Repositories: `google()`, `mavenCentral()`, `gradlePluginPortal()`

---

## Conventions & Patterns

### Code Style

- **Kotlin official style** (`kotlin.code.style=official` in `gradle.properties`)
- **Package structure**: `com.egloo.<layer>.<feature>`
- **File naming**: PascalCase for classes, camelCase for functions
- **Compose naming**: Composables are PascalCase functions (e.g. `HomeScreen()`)

### State Management

- **ViewModels**: Extend `BaseViewModel` (wraps `CoroutineScope`).
- **State**: Exposed as `StateFlow<XUiState>`, collected as `State` in Compose via `collectAsState()`.
- **Events**: Methods on ViewModel (e.g. `sendMessage()`, `loadDigest()`).
- **No ViewModel in Compose**: Screens inject ViewModels via Koin `koinInject()`.

### Error Handling

- **Repository layer**: Emit `Result` wrappers (e.g. `DigestResult.Loading | Success | Error`).
- **ViewModel layer**: Update `uiState` with error messages.
- **UI layer**: Show error states (retry button, error text).

### Async Operations

- **Pattern**: `viewModel.scope.launch { repo.getData().collect { ... } }`
- **Delays**: Use `delay()` in dummy repositories to simulate network latency.
- **Cancellation**: `scope.cancel()` in `BaseViewModel.dispose()`.

### Naming Conventions

**Files**:
- Screens: `XScreen.kt` (e.g. `HomeScreen.kt`)
- ViewModels: `XViewModel.kt`
- Repositories: `XRepository.kt`, `DummyXRepository.kt`, `KtorXRepository.kt`
- Components: `Components.kt` (groups related composables)

**Classes**:
- Data: `XData`, `XDto`, `XResponse`
- State: `XUiState`
- Events: `XEvent` (if using sealed classes for events)

**Functions**:
- Composables: `PascalCase` (e.g. `SourceBadge()`)
- ViewModels: `camelCase` (e.g. `sendMessage()`)
- Repositories: `camelCase` (e.g. `getDailyDigest()`)

### File Length

- **Screens**: 100-300 lines (split if >400)
- **Components**: <200 lines per file
- **ViewModels**: <150 lines (one ViewModel per file)
- **Models**: 200-400 lines (all models in one file is OK for small projects)

---

## Current State

### What Is written but not tested

✅ Full navigation on all 4 platforms (Android, iOS, Desktop, Web)  
✅ Onboarding flow with page dots and CTA  
✅ Home screen with daily digest, Pingo message, action items  
✅ Chat screen with user/Pingo bubbles, source chips, streaming simulation  
✅ Topics screen with grid layout, topic detail sheet  
✅ Sources screen with connect/disconnect simulation  
✅ Settings screen with toggles and sync frequency chips  
✅ Dark/light theme support (dark is default)  
✅ Edge-to-edge on Android  
✅ ComposeWindowStyler on Desktop (Mica on Win11)  
✅ Kotlin/Wasm build on Web (runs in all modern browsers)  
✅ Decompose navigation with back-stack support  
✅ Koin DI with ViewModel injection  
✅ Dummy data for all screens

### What's Missing

❌ Backend integration (no real API calls)  
❌ OAuth flow (Sources screen buttons simulate, don't actually connect)  
❌ Local database (no SQLDelight or Room)  
❌ Persistent storage beyond first-launch flag  
❌ Search functionality (Chat screen answers are keyword-based stubs)  
❌ Image loading (Coil is declared but no images are loaded)  
❌ Push notifications  
❌ Deep linking (intent filters exist but not wired)  
❌ Saved items screen (model exists, no UI)  
❌ Real LLM streaming (Chat screen simulates with delay)  
❌ Analytics/crash reporting  
❌ Unit tests  
❌ CI/CD pipeline

---

## Future Roadmap

### Phase 1: Backend Integration (Weeks 7-10)

**Goal**: Replace dummy repositories with real Ktor-based implementations.

**Tasks**:
1. Create `KtorDigestRepository`, `KtorChatRepository`, etc.
2. Define DTOs in `data/api/Dtos.kt`
3. Create `HttpClientFactory` with platform engines
4. Add mapper functions `DtoToDomain`
5. Implement OAuth flow (see `data/api/ApiGuidelines.kt`)
6. Wire up server-sent events for streaming chat
7. Update `EglooModule.kt` to bind Ktor repos
8. Test on all platforms

**API Endpoints** (from `ApiGuidelines.kt`):
- `GET /digest/today` → `DailyDigestDto`
- `GET /chat/history` → `List<ChatMessageDto>`
- `POST /chat/message` (SSE stream) → `ChatChunkDto`
- `GET /topics` → `List<TopicDto>`
- `GET /sources` → `List<ConnectedSourceDto>`
- `POST /sources/connect` → `{ oauthUrl }`
- `DELETE /sources/{id}`
- `GET /settings`, `PUT /settings`

### Phase 2: Local Storage (Weeks 10-12)

**Goal**: Add SQLDelight for local caching and Multiplatform Settings for preferences.

**Tasks**:
1. Add SQLDelight plugin to `shared/build.gradle.kts`
2. Define `.sq` schema files (KnowledgeItems, Sources, Topics)
3. Create `DatabaseRepository` to manage cache
4. Update repositories to read from cache first, then network
5. Persist settings using Multiplatform Settings
6. Implement offline-first pattern (cache-then-network)

### Phase 3: Advanced Features (Weeks 12-16)

**Saved Items Screen**:
- New destination in `RootComponent.kt`
- Filter `KnowledgeItem` by `isSaved = true`
- UI similar to Home screen but no digest sections

**Search**:
- Add search bar to Home/Topics screens
- Filter locally or call `/search?q=...` endpoint

**Notifications**:
- Desktop: System tray notifications
- Mobile: Push via Firebase Cloud Messaging
- Web: Browser notifications API

**Deep Linking**:
- Android: Handle `egloo://oauth` redirect
- iOS: Universal Links
- Web: Query param parsing (`?oauth_token=...`)

**Image Loading**:
- Use Coil to load Pingo avatar from URL
- Cache images in SQLDelight or file storage

### Phase 4: Polish & Production (Weeks 16-20)

**Testing**:
- Unit tests for ViewModels (verify state transitions)
- Unit tests for repositories (verify API mapping)
- UI tests for key flows (onboarding → home → chat)

**CI/CD**:
- GitHub Actions for Android/Desktop builds
- Xcode Cloud for iOS builds
- Deploy web to Vercel/Netlify

**Analytics**:
- Track screen views, button clicks
- Use platform-neutral library (Segment, PostHog)

**Crash Reporting**:
- Sentry or Crashlytics

**App Store Submission**:
- Google Play Store (Android)
- App Store (iOS via Xcode)
- Microsoft Store (Windows desktop)
- Mac App Store (macOS desktop)

### Phase 5: Advanced AI Features (Future)

- Real-time LLM streaming via WebSockets
- Multi-turn conversation memory in Chat
- Auto-tagging of knowledge items
- Smart notifications (Pingo proactively surfaces relevant info)
- Voice input for Chat (speech-to-text)
- Export digest as PDF/email

---

## Common Tasks

### Add a New Screen

1. **Create the screen file**:
    - `shared/src/commonMain/kotlin/com/egloo/ui/screens/NewScreen.kt`
   ```kotlin
   @Composable
   fun NewScreen(viewModel: NewViewModel = koinInject()) {
       val state by viewModel.uiState.collectAsState()
       // UI here
   }
   ```

2. **Create the ViewModel**:
    - Add to `domain/viewmodels/ViewModels.kt` or create `NewViewModel.kt`
   ```kotlin
   class NewViewModel(private val repo: NewRepository) : BaseViewModel() {
       private val _uiState = MutableStateFlow(NewUiState())
       val uiState: StateFlow<NewUiState> = _uiState.asStateFlow()
   }
   ```

3. **Add destination**:
    - In `navigation/RootComponent.kt`:
   ```kotlin
   @Serializable data object NewDestination : Destination
   sealed class Child {
       class NewChild(val component: ComponentContext) : Child()
   }
   ```

4. **Wire navigation**:
    - In `RootContent.kt`:
   ```kotlin
   when (child.instance) {
       is RootComponent.Child.NewChild -> NewScreen()
   }
   ```

5. **Add to bottom bar**:
    - Update `navItems` in `RootContent.kt`

6. **Register ViewModel in Koin**:
    - `di/EglooModule.kt`:
   ```kotlin
   factoryOf(::NewViewModel)
   ```

### Add a New Repository

1. **Define interface**:
    - In `data/repositories/Repositories.kt`:
   ```kotlin
   interface NewRepository {
       fun getData(): Flow<List<NewItem>>
   }
   ```

2. **Create dummy implementation**:
    - In `data/repositories/DummyRepositories.kt`:
   ```kotlin
   class DummyNewRepository : NewRepository {
       override fun getData() = flow {
           delay(500)
           emit(listOf(NewItem(...)))
       }
   }
   ```

3. **Bind in Koin**:
    - `di/EglooModule.kt`:
   ```kotlin
   singleOf(::DummyNewRepository) bind NewRepository::class
   ```

4. **When backend is ready**:
    - Create `KtorNewRepository` implementing `NewRepository`
    - Swap binding in Koin: `singleOf(::KtorNewRepository) bind NewRepository::class`

### Modify the Theme

**Colors**:
- Edit `ui/theme/EglooColors.kt`
- Add new tokens to `object EglooColors`
- Use in `EglooDarkColorScheme` / `EglooLightColorScheme`

**Typography**:
- Edit `ui/theme/EglooTheme.kt`
- Modify `EglooTypography` scale
- Changes apply globally via `MaterialTheme.typography`

**Applying to a component**:
```kotlin
Text(
    "Hello",
    style = MaterialTheme.typography.headlineMedium,
    color = MaterialTheme.colorScheme.primary,
)
```

### Add a Platform-Specific Function

1. **Declare expect**:
    - `platform/MyPlatformFunc.kt` in `commonMain`
   ```kotlin
   expect fun myPlatformFunc(): String
   ```

2. **Implement actuals**:
    - `androidMain/platform/MyPlatformFunc.android.kt`:
   ```kotlin
   actual fun myPlatformFunc() = "Android"
   ```
    - Repeat for `iosMain`, `desktopMain`, `wasmJsMain`

3. **Use in shared code**:
   ```kotlin
   val platform = myPlatformFunc()
   ```

### Debug on Each Platform

**Android**:
- Run configuration → `androidApp`
- Logcat shows `println()` output

**iOS**:
- Build `:shared` framework first
- Run from Xcode
- Console shows `println()` output

**Desktop**:
- Run configuration → `desktopApp`
- Terminal shows `println()` output

**Web**:
- `./gradlew :wasmApp:wasmJsBrowserDevelopmentRun`
- Browser DevTools console shows `console.log()` (Kotlin `println` maps to this)

---

## Troubleshooting

### Build Errors

**"Duplicate class found"**:
- Clean build: `./gradlew clean`
- Check for duplicate dependencies in `build.gradle.kts`

**"Missing platform declaration"**:
- Ensure `expect` declarations have `actual` implementations in all source sets
- Check `sourceSets { ... }` block includes the platform

**"Compose compiler mismatch"**:
- Verify Kotlin and Compose versions are compatible
- Check `libs.versions.toml` for correct versions

**"Gradle daemon out of memory"**:
- Increase heap in `gradle.properties`: `org.gradle.jvmargs=-Xmx6g`

### Runtime Errors

**"No Koin context"**:
- Ensure `startKoin { modules(eglooModule) }` is called in each platform entry point
- Check it's called before creating `RootComponent`

**"Lifecycle not attached"**:
- Decompose requires `ComponentContext` with a lifecycle
- Pass `defaultComponentContext()` (Android), `DefaultComponentContext(LifecycleRegistry())` (Desktop/Web)

**"Canvas not found" (web)**:
- Ensure `index.html` has `<canvas id="ComposeTarget">`
- Check `canvasElementId` matches in `main.kt`

**"Window transparent not working" (desktop)**:
- Requires `transparent = true` on `Window()`
- Compose root must use `Color.Transparent` background
- ComposeWindowStyler must be called inside `Window { }` lambda

### UI Issues

**Text not visible**:
- Check `color = MaterialTheme.colorScheme.onBackground`
- Verify theme is applied (`EglooTheme { ... }`)

**Navigation not working**:
- Check `navigateTo()` is wired correctly
- Verify destination is in `childFactory` switch

**ViewModel not injecting**:
- Ensure ViewModel is registered in `EglooModule.kt`
- Use `koinInject()` in Composable, not constructor injection

**Images not loading**:
- Coil is declared but not actively used yet
- For now, use placeholders (colored boxes, emojis)

### Platform-Specific

**Android: "Manifest merger failed"**:
- Check `AndroidManifest.xml` for duplicate entries
- Verify `package` attribute matches `namespace` in `build.gradle.kts`

**iOS: "Framework not found"**:
- Build `:shared` framework before running in Xcode
- Check Xcode Build Phases → Link Binary includes `Shared.framework`

**Desktop: "Main class not found"**:
- Verify `mainClass = "com.egloo.desktop.MainKt"` in `build.gradle.kts`
- Check `main()` function is top-level in `main.kt`

**Web: "Wasm module failed to load"**:
- Check browser console for CORS errors
- Ensure `egloo.js` and `egloo.wasm` are in same directory as `index.html`

---

## AI Agent Guidelines

When making changes to this codebase:

1. **Read existing code first**: Check the file structure, existing patterns, naming conventions.
2. **Follow the architecture**: UI → ViewModel → Repository → Data. Don't skip layers.
3. **Use interfaces**: Never call concrete repository classes from ViewModels. Always use the interface.
4. **Platform actuals**: If adding OS-specific code, create `expect` in `commonMain` and `actual` in platform source sets.
5. **Theme consistency**: Use `MaterialTheme.colorScheme.*` and `MaterialTheme.typography.*`, not hardcoded values.
6. **Navigation**: Add new screens via Decompose destinations, not ad-hoc routing.
7. **Testing**: When you add logic, consider how it would be tested (even if tests don't exist yet).
8. **Documentation**: Update this file when you add new screens, repositories, or change architecture.

---

## Questions?

- **Architecture**: See `Architecture` section above
- **How to add X**: See `Common Tasks` section
- **Build failing**: See `Troubleshooting` section
- **Backend migration**: Read `data/api/ApiGuidelines.kt` (full step-by-step guide)
- **Design tokens**: See `Design System & Theme` section

---

**Last updated**: 2026-05-02  
**Project status**: Prototype (dummy data, no backend)  
**Next milestone**: Phase 1 — Backend Integration