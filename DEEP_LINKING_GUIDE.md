# Deep Linking Implementation Guide

## Overview

This document explains the complete deep linking setup for Egloo across Android and iOS platforms. Deep linking allows the app to handle OAuth redirects via custom URL schemes (e.g., `egloo://auth?status=success&source=gmail`).

---

## Architecture

### Shared Code (commonMain)

**File**: `shared/src/commonMain/kotlin/com/trishit/egloo/platform/DeepLinkHandler.kt`

- **Purpose**: Centralized deep link event handler used by ViewModels
- **Implementation**: Uses Kotlin coroutines `SharedFlow` to emit auth results
- **Key Components**:
  - `DeepLinkHandler.authResultFlow` — A hot flow that emits `AuthDeepLinkResult`
  - `emitAuthResult(status, source)` — Called by platform-specific code to emit events
  - `AuthDeepLinkResult` — Data class containing `status` (success/error) and `source` (gmail/slack/google_drive)

### Platform-Specific Integration

#### Android

**Files Changed**:
1. `androidApp/src/main/AndroidManifest.xml`
   - Added intent filter for `egloo://auth` deep links
   - Enables Android to recognize and route these URLs to MainActivity

2. `androidApp/src/main/kotlin/com/trishit/egloo/MainActivity.kt`
   - Added `handleIntent()` method to parse deep link URIs
   - Called in `onCreate()` and `onNewIntent()` (for already-running app)
   - Extracts `status` and `source` query parameters
   - Emits result via `DeepLinkHandler.emitAuthResult()` in a coroutine

#### iOS

**Files Changed**:
1. `iosApp/iosApp/Info.plist`
   - Added `CFBundleURLTypes` with `CFBundleURLSchemes` entry for `egloo`
   - Registers the app to handle custom URL scheme on iOS

2. `iosApp/iosApp/ContentView.swift`
   - Added `.onOpenURL` modifier to `ComposeView()`
   - Parses URL components to extract `status` and `source` parameters
   - Calls `DeepLinkHandler.emitAuthResult()` to emit the result

### ViewModel Integration

**File**: `shared/src/commonMain/kotlin/com/trishit/egloo/domain/viewmodels/ViewModels.kt`

**Changes to SourcesViewModel**:
- Added `authMessage` and `authMessageType` to `SourcesUiState`
- Added enum `AuthMessageType` (SUCCESS, ERROR)
- In `init {}`: Listens to `DeepLinkHandler.authResultFlow`
- Implemented `handleAuthResult()` to:
  - Display success/error message
  - Auto-refresh sources list on success
  - Clear message after 3 seconds
- Added `clearAuthMessage()` for manual dismissal

---

## OAuth Flow with Deep Linking

### Step-by-step Flow

1. **User taps "Connect Gmail"** in Sources screen
   - `SourcesViewModel.connectSource(GMAIL)` is called
   - SourcesRepository calls backend `/sources/connect/gmail` endpoint
   - Backend returns OAuth URL (e.g., `https://accounts.google.com/oauth/authorize?redirect_uri=egloo://auth?source=gmail`)

2. **App opens browser**
   - `SourcesRepository` calls `platformOpenUrl(oauthUrl)`
   - Browser navigates to Google's OAuth page

3. **User authenticates and grants permissions**
   - Google redirects to `egloo://auth?status=success&source=gmail`

4. **OS routes deep link to app**
   - **Android**: Intent sent to MainActivity, `handleIntent()` parses it
   - **iOS**: `onOpenURL` capturing the redirect

5. **App processes deep link**
   - `DeepLinkHandler.emitAuthResult(status, source)` emits result via SharedFlow

6. **SourcesViewModel reacts**
   - Catches the result in `authResultFlow.collect()`
   - Calls `handleAuthResult()` to:
     - Update UI with success/error message
     - Auto-dismiss message after 3 seconds
     - Refresh sources list from backend
     - Clear `connectingType` to hide loading indicator

7. **UI reflects changes**
   - Sources screen shows confirmation banner
   - New source appears in the list
   - Button state returns to normal

---

## Implementation Details

### DeepLinkHandler Usage in ViewModels

```kotlin
init {
    scope.launch {
        DeepLinkHandler.authResultFlow.collect { result ->
            handleAuthResult(result)
        }
    }
}

private fun handleAuthResult(result: DeepLinkHandler.AuthDeepLinkResult) {
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
    
    // Clear after 3 seconds
    scope.launch {
        delay(3000)
        _uiState.update { it.copy(authMessage = null, authMessageType = null) }
    }
    
    // Refresh sources on success
    if (result.status == "success") {
        scope.launch {
            delay(1000)
            sourcesRepo.getConnectedSources().collect { sources ->
                _uiState.update { it.copy(sources = sources) }
            }
        }
    }
}
```

### Android Intent Handling

```kotlin
override fun onNewIntent(intent: android.content.Intent?) {
    super.onNewIntent(intent)
    handleIntent(intent)
}

private fun handleIntent(intent: android.content.Intent?) {
    val uri = intent?.data
    if (uri != null && uri.scheme == "egloo" && uri.host == "auth") {
        val status = uri.getQueryParameter("status")
        val source = uri.getQueryParameter("source")
        
        lifecycleScope.launch {
            DeepLinkHandler.emitAuthResult(status, source)
        }
    }
}
```

**Note**: `onNewIntent()` is critical for already-running apps. If the app is backgrounded and the user authorizes, Android calls `onNewIntent()` instead of `onCreate()`.

### iOS URL Handling

```swift
.onOpenURL { url in
    if url.scheme == "egloo" && url.host == "auth" {
        let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
        let status = components?.queryItems?.first(where: { $0.name == "status" })?.value
        let source = components?.queryItems?.first(where: { $0.name == "source" })?.value
        
        if let status = status, let source = source {
            DeepLinkHandler.shared.emitAuthResult(status: status, source: source) { _ in }
        }
    }
}
```

---

## Testing the Implementation

### Android

1. **Install the app**:
   ```bash
   ./gradlew :androidApp:installDebug
   ```

2. **Simulate deep link**:
   ```bash
   adb shell am start -W -a android.intent.action.VIEW -d "egloo://auth?status=success&source=gmail" com.trishit.egloo
   ```

3. **Expected behavior**:
   - App opens (or comes to foreground)
   - SourcesViewModel receives the event
   - Success message displays for 3 seconds
   - Gmail appears as "Connected" in sources list

### iOS

1. **Build from Xcode**
2. **Simulate deep link in Safari**:
   - Paste `egloo://auth?status=success&source=slack` in address bar
   - Tap "Open"
3. **Expected behavior**:
   - App opens (or comes to foreground)
   - Similar behavior as Android

### Web (Future)

When adding web support:
- Use query parameters: `?oauth_token=...&source=...`
- Parse via `window.location.search` in JavaScript
- Call Kotlin from JS to emit result

---

## Backend Integration Requirements

The `SourcesRepository` (or future `KtorSourcesRepository`) must:

1. **connectSource(type: SourceType)** should:
   - Call backend endpoint: `POST /sources/connect/{type}`
   - The response should contain an OAuth URL
   - Call `platformOpenUrl(oauthUrl)` to open the browser
   - Return immediately (don't wait for user to authorize)

2. **Backend must return redirect URL**:
   - Example: `https://accounts.google.com/oauth/authorize?redirect_uri=https://yourbackend.com/oauth/google/callback`
   - Backend intercepts the callback and redirects to: `egloo://auth?status=success&source=gmail&token=...`
   - App receives the deep link and handles it

3. **Optional**: Backend could send a server-side notification:
   - When user authorizes, backend stores token
   - Backend notifies app via push notification
   - App polls `/sources` endpoint to confirm connection
   - This is more robust than relying solely on deep links

---

## Future Enhancements

### 1. Token Handling
Currently, the deep link just indicates success/failure. For enhanced security:
- Backend could include an encrypted token in the redirect: `egloo://auth?status=success&token=...`
- App decrypts and stores locally
- Prevents need for additional API call

### 2. Error Details
Extend `DeepLinkHandler.AuthDeepLinkResult` to include:
```kotlin
data class AuthDeepLinkResult(
    val status: String,
    val source: String,
    val errorMessage: String? = null,
    val retryCount: Int = 0
)
```

### 3. Deep Link for Other Flows
Generalize `DeepLinkHandler` to handle:
- Password reset: `egloo://password-reset?token=...`
- Email verification: `egloo://verify?code=...`
- Content sharing: `egloo://topic/{topicId}`

### 4. Web Implementation
Add WebJS view to handle query params:
```kotlin
// In wasmJsMain
actual fun handleWebDeepLink() {
    val params = window.location.search
    if (params.contains("status=")) {
        // Parse and emit
    }
}
```

---

## Troubleshooting

### Android

**Problem**: App doesn't open when deep link is tapped
- **Solution**: Verify `android:exported="true"` on MainActivity
- **Solution**: Check intent filter has correct action, category, and data scheme

**Problem**: `handleIntent()` not called
- **Solution**: Ensure `onNewIntent()` is implemented (for backgrounded apps)
- **Solution**: Check `intent?.data` is not null

**Problem**: Query parameters are null
- **Solution**: Verify URL format: `egloo://auth?status=success&source=gmail`
- **Solution**: Use `getQueryParameter()` not `getQueryParameters()`

### iOS

**Problem**: `onOpenURL` not triggered
- **Solution**: Check `CFBundleURLSchemes` includes `egloo` in `Info.plist`
- **Solution**: Rebuild and restart the app
- **Solution**: Try re-opening link from Safari (not web view)

**Problem**: Query parameters are nil
- **Solution**: Use `URLComponents` to safely parse query items
- **Solution**: Verify URL is properly formed: `egloo://auth?status=success&source=gmail`

### Both Platforms

**Problem**: DeepLinkHandler flow not receiving events
- **Solution**: Verify `DeepLinkHandler.emitAuthResult()` is actually called (add logging)
- **Solution**: Ensure SourcesViewModel `init {}` runs before deep link arrives
- **Solution**: Check for coroutine cancellation (scope might be disposed)

---

## Summary

Deep linking is now fully integrated into Egloo. The architecture:
- ✅ Shared code handles all logic via `DeepLinkHandler`
- ✅ Android captures deep links in `MainActivity` and `onNewIntent()`
- ✅ iOS captures deep links via `.onOpenURL` modifier
- ✅ SourcesViewModel reacts to auth results with visual feedback
- ✅ Automatic sources list refresh on successful connection
- ✅ Error messages displayed to user on failure

The implementation is production-ready and follows KMP best practices (shared-first, platform actuals only where necessary).

