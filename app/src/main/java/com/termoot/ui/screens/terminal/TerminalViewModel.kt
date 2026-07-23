package com.termoot.ui.screens.terminal

import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.termoot.TermootApplication
import com.termoot.data.repository.WorkspaceRepository
import com.termoot.domain.model.Session
import com.termoot.domain.model.Workspace
import com.termoot.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the terminal session screen.
 *
 * Loads the target [Workspace] from the repository and manages
 * open terminal sessions via [SessionManager].
 */
class TerminalViewModel(
    private val workspaceId: String,
    private val repository: WorkspaceRepository = TermootApplication.instance.repository
) : ViewModel() {

    /** The workspace we are connecting to. */
    private val _workspace = MutableStateFlow<Workspace?>(null)
    val workspace: StateFlow<Workspace?> = _workspace.asStateFlow()

    /** All open sessions in this terminal, derived from [SessionManager]. */
    val sessions: StateFlow<List<Session>> = SessionManager.domainSessions

    /** The currently active (focused) session id. */
    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    /** The active [com.termoot.session.TerminalSession], or null. */
    val activeSession: com.termoot.session.TerminalSession?
        get() = _activeSessionId.value?.let { SessionManager.getSession(it) }

    init {
        loadWorkspace()
    }

    private fun loadWorkspace() {
        viewModelScope.launch {
            val ws = repository.getWorkspaceById(workspaceId)
            _workspace.value = ws
            if (ws != null && sessions.value.isEmpty()) {
                openSession(ws)
            }
        }
    }

    /** Open a new terminal tab for the given workspace. */
    fun openSession(workspace: Workspace) {
        val ts = SessionManager.openSession(workspace)
        _activeSessionId.value = ts.id
    }

    /** Close the session with [sessionId]. */
    fun closeSession(sessionId: String) {
        SessionManager.closeSession(sessionId)
        if (_activeSessionId.value == sessionId) {
            _activeSessionId.value = SessionManager.activeSessionId
        }
    }

    /** Switch the active tab to [sessionId]. */
    fun setActiveSession(sessionId: String) {
        SessionManager.setActiveSession(sessionId)
        _activeSessionId.value = sessionId
    }

    // ── Terminal interaction ────────────────────────────────────

    /** Send a special key / control sequence to the active session. */
    fun sendKey(key: String) {
        val session = activeSession ?: return
        val bytes = when (key) {
            "Ctrl" -> byteArrayOf(0x00)
            "Alt" -> byteArrayOf(0x1B)       // ESC prefix
            "Meta" -> byteArrayOf(0x1B)       // ESC prefix
            "Shift" -> byteArrayOf()
            "Tab" -> byteArrayOf(0x09)
            "Esc" -> byteArrayOf(0x1B)
            "Up" -> "\u001B[A".toByteArray()
            "Down" -> "\u001B[B".toByteArray()
            "Left" -> "\u001B[D".toByteArray()
            "Right" -> "\u001B[C".toByteArray()
            "PgUp" -> "\u001B[5~".toByteArray()
            "PgDn" -> "\u001B[6~".toByteArray()
            "Home" -> "\u001B[H".toByteArray()
            "End" -> "\u001B[F".toByteArray()
            "Ins" -> "\u001B[2~".toByteArray()
            "Del" -> "\u001B[3~".toByteArray()
            "F1" -> "\u001BOP".toByteArray()
            "F2" -> "\u001BOQ".toByteArray()
            "F3" -> "\u001BOR".toByteArray()
            "F4" -> "\u001BOS".toByteArray()
            "F5" -> "\u001B[15~".toByteArray()
            "F6" -> "\u001B[17~".toByteArray()
            "F7" -> "\u001B[18~".toByteArray()
            "F8" -> "\u001B[19~".toByteArray()
            "F9" -> "\u001B[20~".toByteArray()
            "F10" -> "\u001B[21~".toByteArray()
            "F11" -> "\u001B[23~".toByteArray()
            "F12" -> "\u001B[24~".toByteArray()
            "Enter" -> byteArrayOf(0x0D)
            "/" -> "/".toByteArray()
            "|" -> "|".toByteArray()
            "-" -> "-".toByteArray()
            else -> byteArrayOf()
        }
        if (bytes.isNotEmpty()) {
            session.writeBytes(bytes)
        }
    }

    /** Paste clipboard content into the active terminal session. */
    fun pasteText() {
        val session = activeSession ?: return
        val context = TermootApplication.instance
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val text = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: return
        session.write(text)
    }

    /** Disconnect (close) the session identified by [sessionId]. */
    fun disconnectSession(sessionId: String) {
        SessionManager.closeSession(sessionId)
        if (_activeSessionId.value == sessionId) {
            _activeSessionId.value = SessionManager.activeSessionId
        }
    }

    /** Reconnect: open a fresh session for the current workspace. */
    fun reconnectSession() {
        _workspace.value?.let { ws ->
            openSession(ws)
        }
    }

    companion object {
        fun Factory(workspaceId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TerminalViewModel(workspaceId) as T
                }
            }
    }
}
