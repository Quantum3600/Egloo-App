package com.trishit.egloo.platform

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object DeepLinkHandler {
    private val _authResultFlow = MutableSharedFlow<AuthDeepLinkResult>()
    val authResultFlow = _authResultFlow.asSharedFlow()

    suspend fun emitAuthResult(status: String, source: String) {
        _authResultFlow.emit(AuthDeepLinkResult(status, source))
    }

    data class AuthDeepLinkResult(val status: String, val source: String)
}
