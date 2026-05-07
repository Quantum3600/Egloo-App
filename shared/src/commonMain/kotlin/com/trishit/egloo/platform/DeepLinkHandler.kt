package com.trishit.egloo.platform

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Handles deep links from OAuth redirects.
 * Provides a shared flow that ViewModels can collect to handle auth results.
 */
object DeepLinkHandler {
    private val _authResultFlow = MutableSharedFlow<AuthDeepLinkResult>()
    val authResultFlow: SharedFlow<AuthDeepLinkResult> = _authResultFlow.asSharedFlow()

    suspend fun emitAuthResult(status: String?, source: String?) {
        if (status != null && source != null) {
            _authResultFlow.emit(AuthDeepLinkResult(status = status, source = source))
        }
    }

    data class AuthDeepLinkResult(
        val status: String, // "success" or "error"
        val source: String  // "gmail", "slack", "google_drive"
    )
}

