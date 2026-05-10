# Firebase Cloud Messaging (FCM) Implementation Guide

**Date**: May 9, 2026  
**Status**: ✅ **IMPLEMENTED** — Platform abstraction ready for production

---

## Overview

Firebase Cloud Messaging (FCM) enables push notifications for digest updates and other important events. The implementation uses a **platform-agnostic abstraction** (expect/actual pattern) with:

- **Android**: Full FCM support via Firebase Cloud Messaging SDK
- **iOS**: APNs device tokens (requires Firebase SDK)
- **Desktop**: Not supported (returns null)
- **Web**: Web FCM tokens (requires Firebase Web SDK)

---

## Architecture

### Platform Abstraction Layer

**File**: `shared/src/commonMain/kotlin/com/trishit/egloo/platform/FcmToken.kt`

```kotlin
expect fun getFcmToken(): Flow<String?>
expect suspend fun storeFcmToken(token: String)
expect suspend fun getStoredFcmToken(): String?
```

**Implementations**:
- `FcmToken.android.kt` — Android (DataStore)
- `FcmToken.ios.kt` — iOS (stub, use UserDefaults)
- `FcmToken.jvm.kt` — Desktop (not supported)
- `FcmToken.wasmJs.kt` — Web (stub, use localStorage)

---

## Components

### 1. NotificationRepository

**File**: `shared/src/commonMain/kotlin/com/trishit/egloo/data/repositories/NotificationRepository.kt`

```kotlin
interface NotificationRepository {
    fun getFcmTokenStream(): Flow<String?>
    suspend fun getToken(): String?
    suspend fun registerToken(token: String): Result<Unit>
    suspend fun unregisterToken(): Result<Unit>
    suspend fun isDigestNotificationEnabled(): Boolean
    suspend fun setDigestNotificationEnabled(enabled: Boolean): Result<Unit>
}
```

**Current Implementation**: `InMemoryNotificationRepository` (development)

**Future Implementation**: `KtorNotificationRepository` (production, calls backend `/api/v1/notifications` endpoints)

---

### 2. NotificationViewModel

**File**: `shared/src/commonMain/kotlin/com/trishit/egloo/domain/viewmodels/ViewModels.kt`

```kotlin
class NotificationViewModel(private val notificationRepo: NotificationRepository) : BaseViewModel() {
    private val _uiState = MutableStateFlow(NotificationUiState())
    
    fun registerToken(token: String)
    fun toggleDigestNotifications(enabled: Boolean)
    fun clearError()
}
```

**Responsibilities**:
- Listen to FCM token changes
- Register/unregister tokens with backend
- Manage notification preferences (toggle digest notifications)
- Expose token and preferences to UI

---

### 3. Integration with Digest Generation

**File**: `shared/src/commonMain/kotlin/com/trishit/egloo/data/repositories/KtorDigestRepository.kt`

When generating a digest, the FCM token is automatically included:

```kotlin
override suspend fun generateDigest(force: Boolean): Result<Unit> {
    val fcmToken = getStoredFcmToken()  // ← Fetch token
    val response = client.post("/api/v1/digest/generate") {
        setBody(GenerateDigestRequest(
            force_regenerate = force,
            fcm_token = fcmToken         // ← Send to backend
        ))
    }
}
```

**Backend Flow**:
1. App stores FCM token locally
2. When digest is generated, token is sent to backend
3. Backend associates token with digest
4. Backend sends push notification to that token

---

## Implementation Status

| Platform | Status | Details |
|----------|--------|---------|
| **Android** | ✅ Implemented | DataStore persistence, Firebase SDK ready |
| **iOS** | ⚠️ Stub | Requires Firebase SDK + APNs setup |
| **Desktop** | ⚠️ Not Supported | Returns null |
| **Web** | ⚠️ Stub | Requires Firebase Web SDK + service worker |

---

## Android Implementation Details

### Dependencies Required

Add to `shared/build.gradle.kts`:

```kotlin
androidMain.dependencies {
    // Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging:23.4.0")
    // DataStore for token persistence
    implementation("androidx.datastore:datastore-preferences:1.0.0")
}
```

### Firebase Service Integration

Create `androidApp/src/main/kotlin/com/egloo/android/EglooMessagingService.kt`:

```kotlin
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.trishit.egloo.platform.storeFcmToken
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class EglooMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Store token when Firebase generates/refreshes it
        GlobalScope.launch {
            storeFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // Handle incoming push notification
        message.notification?.let {
            showNotification(it.title ?: "Egloo", it.body ?: "")
        }
    }

    private fun showNotification(title: String, message: String) {
        // Show system notification using NotificationCompat
        // This will wake the user and alert them to the digest/event
    }
}
```

### AndroidManifest.xml

```xml
<service
    android:name=".EglooMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

---

## iOS Implementation Details

### Dependencies Required

Add to Podfile (via CocoaPods):

```ruby
pod 'Firebase/Messaging'
```

### AppDelegate Setup

```swift
import Firebase
import UserNotifications

class AppDelegate: UIResponder, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        FirebaseApp.configure()
        
        // Request user notification permission
        UNUserNotificationCenter.current().delegate = self
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
        
        UIApplication.shared.registerForRemoteNotifications()
        
        return true
    }
    
    // Called when device token is obtained
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
        
        // Get FCM instance token
        Messaging.messaging().token { token, error in
            if let token = token {
                Task {
                    await storeFcmToken(token: token)
                }
            }
        }
    }
}
```

---

## Web Implementation Details

### Dependencies Required

```bash
npm install firebase firebase-app-check
```

### Firebase Initialization

```typescript
import { initializeApp } from "firebase/app";
import { getMessaging, getToken, onMessage } from "firebase/messaging";

const firebaseApp = initializeApp({
  apiKey: "YOUR_API_KEY",
  projectId: "YOUR_PROJECT_ID",
  // ... other config
});

const messaging = getMessaging(firebaseApp);

// Get FCM token
getToken(messaging, { vapidKey: "YOUR_VAPID_KEY" }).then((token) => {
  // Store token (via kotlin call)
  storeFcmToken(token);
});

// Handle incoming messages
onMessage(messaging, (payload) => {
  console.log("Message received:", payload);
  // Show notification
  new Notification(payload.notification.title, {
    body: payload.notification.body,
  });
});
```

### Service Worker

Create `public/firebase-messaging-sw.js`:

```javascript
importScripts("https://www.gstatic.com/firebasejs/9/firebase-app-compat.js");
importScripts("https://www.gstatic.com/firebasejs/9/firebase-messaging-compat.js");

firebase.initializeApp({
  apiKey: "YOUR_API_KEY",
  projectId: "YOUR_PROJECT_ID",
  // ... other config
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
  const notificationTitle = payload.notification.title;
  const notificationOptions = {
    body: payload.notification.body,
    icon: "https://egloo.app/icon-192x192.png",
  };

  self.registration.showNotification(notificationTitle, notificationOptions);
});
```

---

## Backend API Endpoints (Future)

When backend FCM integration is ready, add these endpoints:

```
POST /api/v1/notifications/register
  Request: { fcm_token: string }
  Response: { success: bool }

POST /api/v1/notifications/unregister
  Request: { fcm_token: string }
  Response: { success: bool }

GET /api/v1/settings/notifications
  Response: { digest_enabled: bool, other_enabled: bool }

PUT /api/v1/settings/notifications
  Request: { digest_enabled: bool }
  Response: { success: bool }
```

---

## Migration from InMemory to Ktor

When backend is ready:

1. Create `KtorNotificationRepository.kt`:

```kotlin
class KtorNotificationRepository(private val client: HttpClient) : NotificationRepository {
    override suspend fun registerToken(token: String): Result<Unit> {
        return try {
            val response = client.post("/api/v1/notifications/register") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("fcm_token" to token))
            }
            if (response.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("Failed to register token"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // ... other methods
}
```

2. Update `EglooModule.kt`:

```kotlin
single<NotificationRepository> { KtorNotificationRepository(get()) }
```

---

## Testing Checklist

### Development (Android + Web)

- [ ] Firebase project created and configured
- [ ] Google Services JSON downloaded (Android)
- [ ] FCM token obtained on app launch
- [ ] Token persisted locally
- [ ] Token can be retrieved

### Integration

- [ ] Register token with backend on app start
- [ ] Toggle digest notifications in Settings
- [ ] Digest generation includes FCM token
- [ ] Backend receives and stores token

### FCM Notifications (Manual Testing)

- [ ] Send test notification via Firebase Console
- [ ] Notification appears on device/web
- [ ] Notification contains digest summary
- [ ] Tapping notification opens app to digest

---

## Environment Variables

For production deployments, add to `.env`:

```env
FIREBASE_API_KEY=...
FIREBASE_PROJECT_ID=...
FIREBASE_MESSAGING_SENDER_ID=...
FIREBASE_APP_ID=...
FCM_SERVER_KEY=...  # For backend
FCM_VAPID_KEY=...   # For web
```

---

## Security Considerations

1. **Token Rotation**: Firebase automatically rotates tokens (listen for `onNewToken`)
2. **Token Privacy**: Never expose tokens in logs or error messages
3. **Backend Validation**: Backend should validate tokens before storing
4. **HTTPS Only**: Firebase requires HTTPS (web)
5. **Service Worker**: Requires HTTPS + valid certificate (web)

---

## Monitoring & Troubleshooting

### Common Issues

| Issue | Cause | Fix |
|-------|-------|-----|
| Token is null | Firebase SDK not initialized | Check Firebase config + init order |
| Notifications not received | Token not registered | Ensure `registerToken()` was called |
| Old tokens still active | Token rotation not handled | Re-register on `onNewToken()` |
| Message received but no UI | Service worker not active | Check SW registration (web) |

### Logs to Check

```
// Android Logcat
adb logcat | grep FirebaseMsgSvc

// Web Console
console.log("Firebase token:", token)
```

---

## Next Steps

1. **Immediate**: Core code is ready (platform abstraction, ViewModel, integration)
2. **Android**: Add Firebase SDK + service to handle incoming notifications
3. **Backend**: Implement `/api/v1/notifications/*` endpoints
4. **iOS/Web**: Implement FCM token collection (stubs ready)
5. **Production**: Enable FCM in Firebase Console + configure web push

---

**Status**: Ready for Firebase SDK integration and backend endpoint implementation! 🚀

