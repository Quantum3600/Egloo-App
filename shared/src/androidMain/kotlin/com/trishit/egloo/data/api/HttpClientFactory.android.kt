package com.trishit.egloo.data.api

import io.ktor.client.engine.*
import io.ktor.client.engine.android.*

actual fun platformEngine(): HttpClientEngineFactory<*> = Android
