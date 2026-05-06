package com.trishit.egloo.platform

/**
 * Opens a URL in the platform's default browser.
 * Used by SourcesViewModel when the backend returns an OAuth URL.
 *
 * See data/api/ApiGuidelines.kt STEP 6 for the full OAuth flow.
 */
expect fun platformOpenUrl(url: String)
