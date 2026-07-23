package com.termoot.session

import com.termoot.domain.model.Session
import com.termoot.domain.model.SessionState
import com.termoot.domain.model.Workspace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * In-memory stub session manager for UI development.
 */
class StubSessionManager {

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    fun openSession(workspace: Workspace): Session {
        val session = Session(
            id = UUID.randomUUID().toString(),
            workspaceId = workspace.id,
            name = workspace.name,
            state = SessionState.CONNECTED,
            isActive = true
        )
        val updated = _sessions.value.map { it.copy(isActive = false) } + session
        _sessions.value = updated
        _activeSessionId.value = session.id
        return session
    }

    fun closeSession(sessionId: String) {
        val remaining = _sessions.value.filter { it.id != sessionId }
        _sessions.value = remaining
        if (_activeSessionId.value == sessionId) {
            val next = remaining.lastOrNull()
            _activeSessionId.value = next?.id
            if (next != null) {
                _sessions.value = remaining.map { it.copy(isActive = it.id == next.id) }
            }
        }
    }

    fun setActiveSession(sessionId: String) {
        _activeSessionId.value = sessionId
        _sessions.value = _sessions.value.map {
            it.copy(isActive = it.id == sessionId)
        }
    }
}
