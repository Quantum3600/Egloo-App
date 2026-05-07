package com.trishit.egloo.platform

import kotlin.js.Date

actual fun currentTimeMillis(): Long = Date.now().toLong()
