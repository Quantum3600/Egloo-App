package com.trishit.egloo.platform

import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => Date.now()")
external fun jsDateNow(): Double

actual fun currentTimeMillis(): Long = jsDateNow().toLong()
