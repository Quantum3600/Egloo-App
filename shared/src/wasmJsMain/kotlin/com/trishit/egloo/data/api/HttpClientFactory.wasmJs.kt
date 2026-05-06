package com.trishit.egloo.data.api

import io.ktor.client.engine.*
import io.ktor.client.engine.js.*

actual fun platformEngine(): HttpClientEngineFactory<*> = Js
