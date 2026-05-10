# Walkthrough - OAuth Flow & Auth Integration

This walkthrough summarizes the implementation of the real OAuth flow, secure token storage, and automatic token rotation for the Egloo KMP project.

## Implementation Summary

### 1. Real OAuth Flow (Main Priority)
The mock OAuth flow has been replaced with a real implementation in `KtorSourcesRepository`.
- **Trigger**: When a user clicks "Connect", the app calls the backend.
- **URL Extraction**: The repository handles both 200 OK (JSON with `oauthUrl`) and 3xx Redirects (302, 307, 308) to find the provider's login URL.
- **Redirection**: Uses platform-specific APIs to open the system browser.
- **Deep Links**: Handled via `DeepLinkHandler` to complete the flow once the browser redirects back to `egloo://auth`.

### 2. Secure Token Storage
Access and refresh tokens are now stored using the most secure method available on each platform:
- **Android**: [EncryptedSharedPreferences](file:///D:/projects/Egloo/Egloo-Kmp/shared/src/androidMain/kotlin/com/trishit/egloo/platform/Settings.android.kt)
- **iOS**: [KeychainSettings](file:///D:/projects/Egloo/Egloo-Kmp/shared/src/iosMain/kotlin/com/trishit/egloo/platform/Settings.ios.kt)
- **Desktop/JVM**: [PreferencesSettings](file:///D:/projects/Egloo/Egloo-Kmp/shared/src/jvmMain/kotlin/com/trishit/egloo/platform/Settings.jvm.kt)
- **Web (JS/Wasm)**: [StorageSettings](file:///D:/projects/Egloo/Egloo-Kmp/shared/src/wasmJsMain/kotlin/com/trishit/egloo/platform/Settings.wasmJs.kt) (also implemented in [jsMain](file:///D:/projects/Egloo/Egloo-Kmp/shared/src/jsMain/kotlin/com/trishit/egloo/platform/Settings.js.kt))

### 3. Auth Interceptor & Token Rotation
Seamless session management is implemented using Ktor's `Auth` plugin:
- **Automatic Refresh**: If a request fails with a 401, the client automatically calls `/api/v1/auth/refresh` using the stored refresh token.
- **Token Update**: On success, both access and refresh tokens are updated in secure storage, and the original request is retried.
- **Logout on Failure**: If the refresh token is also invalid, the user is automatically logged out.

### 4. Improved Job Polling
Ingestion job tracking now feels more "alive":
- **Interval**: Polling occurs every 2 seconds.
- **Precision**: Instead of polling the entire list, the app now polls specific `jobId`s for granular progress updates.

## Verification Results

### Automated Tests
- `AuthRepositoryTest`: Verified that login, token storage, and logout work as expected.
- `AuthRepositoryTest.testRefreshTokenUpdatesStorage`: Proved that the refresh logic correctly calls the backend and updates the `Settings`.

### Manual Verification
- **OAuth Flow**: Verified that `connectSource` correctly triggers `platformOpenUrl` with the URL returned by the backend.
- **DI Wiring**: Confirmed via `gradle_sync` that all platform-specific `createSettings` implementations are correctly bound.
