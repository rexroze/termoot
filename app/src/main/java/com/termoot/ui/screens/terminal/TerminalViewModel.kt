package com.termoot.ui.screens.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.termoot.data.repository.StubWorkspaceRepository
import com.termoot.data.repository.WorkspaceRepository
import com.termoot.domain.model.Session
import com.termoot.domain.model.Workspace
import com.termoot.session.StubSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the terminal session screen.
 *
 * Loads the target [Workspace] from the repository and manages
 * open terminal sessions via [sessionManager].
 */
class TerminalViewModel(
    private val workspaceId: String,
    private val repository: WorkspaceRepository = StubWorkspaceRepository(),
    private val sessionManager: StubSessionManager = StubSessionManager()
) : ViewModel() {

    /** The workspace we're connecting to. */
    private val _workspace = MutableStateFlow<Workspace?>(null)
    val workspace: StateFlow<Workspace?> = _workspace.asStateFlow()

    /** All open sessions in this terminal. */
    val sessions: StateFlow<List<Session>> = sessionManager.sessions

    /** The currently active (focused) session id. */
    val activeSessionId: StateFlow<String?> = sessionManager.activeSessionId

    init {
        loadWorkspace()
    }

    private fun loadWorkspace() {
        viewModelScope.launch {
            val ws = repository.getWorkspaceById(workspaceId)
            _workspace.value = ws
            if (ws != null && sessions.value.isEmpty()) {
                sessionManager.openSession(ws)
            }
        }
    }

    /** Open a new terminal tab for the given workspace. */
    fun openSession(workspace: Workspace) {
        sessionManager.openSession(workspace)
    }

    /** Close the session with [sessionId]. */
    fun closeSession(sessionId: String) {
        sessionManager.closeSession(sessionId)
    }

    /** Switch the active tab to [sessionId]. */
    fun setActiveSession(sessionId: String) {
        sessionManager.setActiveSession(sessionId)
    }

    companion object {
        fun Factory(workspaceId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TerminalViewModel(workspaceId = workspaceId) as T
                }
            }
    }
}
