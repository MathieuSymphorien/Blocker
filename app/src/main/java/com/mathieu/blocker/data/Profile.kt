package com.mathieu.blocker.data

data class Profile(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val blockedApps: Set<String> = emptySet(),
    val timerSeconds: Int = 10,
    val challengeEnabled: Boolean = false,
    val challengeLength: Int = 12
)
