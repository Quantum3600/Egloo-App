package com.trishit.egloo

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform