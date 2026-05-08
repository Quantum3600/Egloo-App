package com.trishit.egloo.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DeepLinkHandler {
    private val _authResultFlow = MutableStateFlow<AuthDeepLinkResult?>(null)
    val authResultFlow: StateFlow<AuthDeepLinkResult?> = _authResultFlow.asStateFlow()

    fun emitAuthResult(status: String?, source: String?) {
        if (status != null && source != null) {
            _authResultFlow.value = AuthDeepLinkResult(status, source)
        }
    }

    fun clearAuthResult() {
        _authResultFlow.value = null
    }

    data class AuthDeepLinkResult(val status: String, val source: String)
}
