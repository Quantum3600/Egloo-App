package com.trishit.egloo.data.api

import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*

actual fun platformEngine(): HttpClientEngineFactory<*> = OkHttp
