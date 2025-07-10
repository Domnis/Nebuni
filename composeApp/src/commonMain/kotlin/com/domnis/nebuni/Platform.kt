package com.domnis.nebuni

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform