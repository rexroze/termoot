package com.termoot.domain.model

enum class SessionState {
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    ERROR
}

data class Session(
    val id: String,
    val workspaceId: String,
    val name: String,                      // Display name (tab title)
    val state: SessionState = SessionState.DISCONNECTED,
    val errorMessage: String? = null,
    val isActive: Boolean = false
)
