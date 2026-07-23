package com.termoot.session

import android.util.Log
import com.termoot.domain.model.SessionState
import com.termoot.domain.model.Workspace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton that manages all active [TerminalSession] instances.
 *
 * Provides thread-safe session lifecycle operations (open, close, activate)
 * and exposes an observable [StateFlow] of the current session map.
 *
 * Sessions remain registered in the manager until explicitly closed via
 * [closeSession] or [closeAllSessions]. The UI layer should observe
 * [sessionsFlow] and [sessionStateChanges] to react to state transitions.
 */
object SessionManager {

    private val sessions = ConcurrentHashMap<String, TerminalSession>()
    private val _sessionsFlow = MutableStateFlow<Map<String, TerminalSession>>(emptyMap())

    /** Observable state flow of all active sessions, keyed by session ID. */
    val sessionsFlow: StateFlow<Map<String, TerminalSession>> = _sessionsFlow.asStateFlow()

    /** The currently active (focused) session ID, or null if no session is active. */
    @Volatile
    var activeSessionId: String? = null
        private set

    /**
     * Creates a new [TerminalSession] for the given [workspace], connects it,
     * and registers it in the active session map.
     *
     * @param workspace The workspace to open a session for.
     * @param name Optional display name for the session tab. Defaults to the workspace name.
     * @param onStateChange Optional callback invoked on every state transition for this session.
     * @return The newly created [TerminalSession] (already in CONNECTING/CONNECTED state).
     */
    fun openSession(
        workspace: Workspace,
        name: String? = null,
        onStateChange: ((sessionId: String, newState: SessionState) -> Unit)? = null
    ): TerminalSession {
        val sessionId = UUID.randomUUID().toString()
        val displayName = name ?: workspace.name

        val session = TerminalSession(
            id = sessionId,
            workspace = workspace,
            name = displayName
        )

        sessions[sessionId] = session
        publishSessions()
        setActiveSession(sessionId)

        // Wire up state change forwarding
        session.onStateChanged = { newState ->
            onStateChange?.invoke(sessionId, newState)
        }

        session.connect()

        Log.i(TAG, "Opened session [$sessionId] for workspace [${workspace.id}]")
        return session
    }

    /**
     * Closes and removes a session by its ID. Disconnects the underlying
     * terminal and releases all resources.
     *
     * If the session is the currently active one, [activeSessionId] is reassigned
     * to the next available session, or cleared if no sessions remain.
     */
    fun closeSession(sessionId: String) {
        val session = sessions.remove(sessionId) ?: return

        // Detach state callback to prevent reentrancy during disconnect
        session.onStateChanged = null
        session.disconnect()

        // Reassign active session if needed
        if (activeSessionId == sessionId) {
            activeSessionId = sessions.keys.firstOrNull()
        }

        publishSessions()
        Log.i(TAG, "Closed session [$sessionId]")
    }

    /**
     * Disconnects and removes all active sessions.
     */
    fun closeAllSessions() {
        val ids = sessions.keys.toList()
        ids.forEach { id ->
            val session = sessions.remove(id)
            session?.onStateChanged = null
            session?.disconnect()
        }
        activeSessionId = null
        publishSessions()
        Log.i(TAG, "All sessions closed")
    }

    /**
     * Returns the [TerminalSession] with the given ID, or null if not found.
     */
    fun getSession(sessionId: String): TerminalSession? {
        return sessions[sessionId]
    }

    /**
     * Sets the active (focused) session to the given session ID.
     * If the ID is null, the active session is cleared.
     */
    fun setActiveSession(sessionId: String?) {
        activeSessionId = sessionId
        publishSessions()
    }

    /**
     * Returns the number of currently active sessions.
     */
    fun activeSessionCount(): Int = sessions.size

    /**
     * Returns whether there are any active sessions.
     */
    fun hasActiveSessions(): Boolean = sessions.isNotEmpty()

    /**
     * Publishes a snapshot of the current session map to the StateFlow.
     */
    private fun publishSessions() {
        _sessionsFlow.value = sessions.toMap()
    }

    companion object {
        private const val TAG = "SessionManager"
    }
}
