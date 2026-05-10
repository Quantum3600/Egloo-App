# Implementation Plan - OAuth Flow, Auth Interceptor & Job Polling

This plan prioritizes a real OAuth flow implementation, followed by secure token storage, automatic token rotation (Auth Interceptor), and improved ingestion job polling.

## User Review Required

- **Deep Link Schemes**: For mobile (Android/iOS), I'll use `egloo://auth` as the redirect URI. Ensure the backend is configured to redirect to this scheme.
- **Web Redirect**: For the Web target, the redirect will likely be back to the app's URL with query parameters.
- **Job Polling Interval**: Implemented as 2 seconds for a responsive feel.

## Proposed Changes

### [Component: Data Layer - Sources (HIGH PRIORITY)]

#### [KtorSourcesRepository.kt](file:///D:/projects/Egloo/Egloo-Kmp/shared/src/commonMain/kotlin/com/trishit/egloo/data/repositories/KtorSourcesRepository.kt)

- **Robust URL Extraction**: Refine `connectSource` to handle all possible backend responses:
    - 200 OK with `{"oauthUrl": "..."}` JSON body.
    - 3xx Redirects (302, 303, 307, 308) where the URL is in the `Location` header.
    - Log errors explicitly if no URL is found.
- **Source Type Mapping**: Ensure `SourceType` to backend string mapping is accurate (e.g., `google_drive` vs `gmail`).

#### [DeepLinkHandler.kt](file:///D:/projects/Egloo/Egloo-Kmp/shared/src/commonMain/kotlin/com/trishit/egloo/platform/DeepLinkHandler.kt)

- Ensure this common component correctly parses the `status` and `source` from the redirect URL and notifies the UI.

---

### [Component: Data Layer - Auth]

#### [AuthRepository.kt](file:///D:/projects/Egloo/Egloo-Kmp/shared/src/commonMain/kotlin/com/trishit/egloo/data/repositories/AuthRepository.kt)

- Update `AuthRepository` interface to include `refreshToken(): Result<TokenResponse>`.
- Update `KtorAuthRepository` to:
    - Store and retrieve `refresh_token`.
    - Implement `refreshToken()` using a separate `HttpClient` (to avoid circular dependency/interceptor loops).
    - Clear both tokens on `logout()`.

#### [HttpClientFactory.kt](file:///D:/projects/Egloo/Egloo-Kmp/shared/src/commonMain/kotlin/com/trishit/egloo/data/api/HttpClientFactory.kt)

- Update `createHttpClient` to use the `Auth` plugin's `refreshTokens` feature.
- Use `AuthRepository.refreshToken()` to get new tokens when a 401 is encountered.

---

### [Component: Data Layer - Ingestion]

#### [IngestViewModel.kt](file:///D:/projects/Egloo/Egloo-Kmp/shared/src/commonMain/kotlin/com/trishit/egloo/domain/viewmodels/ViewModels.kt)

- Update `pollJobsUntilComplete` to poll individual job statuses using `ingestRepo.getJobStatus(jobId)`.
- Change polling interval to 2 seconds.

---

## Verification Plan

### Automated Tests
- **SourcesRepositoryTest**: Mock `/sources/connect/{type}` with both JSON and Redirect responses, verifying `platformOpenUrl` is called with the expected URL.
- **AuthRepositoryTest**: Mock the `/auth/refresh` endpoint and verify that `refreshToken()` updates the stored tokens.

### Manual Verification
- **OAuth Flow (MAIN PRIORITY)**: Click "Connect" for Gmail/Slack. Verify:
    1. System browser opens the correct provider login page.
    2. After login, redirecting back to the app (`egloo://auth?status=success&source=gmail`) triggers the success message in the app.
- **Token Rotation**: Manually expire the access token (or simulate it) and verify the app continues to work without logging out.
- **Job Polling**: Trigger a sync and verify the progress updates in the UI every 2 seconds until completion.
