package com.parvez.privacyisland

data class IslandEvent(
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val isSensitive: Boolean
)
