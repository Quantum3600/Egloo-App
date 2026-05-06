package com.trishit.egloo.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

lateinit var androidAppContext: Context
actual fun platformOpenUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    androidAppContext.startActivity(intent)
}