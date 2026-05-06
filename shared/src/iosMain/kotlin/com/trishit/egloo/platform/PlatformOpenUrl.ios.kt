package com.trishit.egloo.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun platformOpenUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl)
}