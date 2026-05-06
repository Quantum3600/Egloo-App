package com.trishit.egloo.platform

import kotlinx.browser.window

actual fun platformOpenUrl(url: String) {
    window.open(url, "_blank")
}