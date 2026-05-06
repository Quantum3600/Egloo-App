package com.trishit.egloo.data.api

import io.ktor.client.engine.*
import io.ktor.client.engine.darwin.*

actual fun platformEngine(): HttpClientEngineFactory<*> = Darwin
