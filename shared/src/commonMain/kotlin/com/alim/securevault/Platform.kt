package com.alim.securevault

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform