package com.termoot.ui.screens.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.termoot.data.repository.StubWorkspaceRepository
import com.termoot.data.repository.WorkspaceRepository
import com.termoot.domain.model.Workspace
import com.termoot.domain.model.WorkspaceType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Holds the full form state for creating / editing a workspace.
 *
 * Exposes [saved] as a hot SharedFlow so the screen can observe save
 * completion and navigate back.
 */
class WorkspaceEditorViewModel(
    private val workspaceId: String?,
    private val repository: WorkspaceRepository = StubWorkspaceRepository()
) : ViewModel() {

    /* ── Form fields ── */
    var name by mutableStateOf("")
    var type by mutableStateOf(WorkspaceType.LOCAL_SHELL)
    var distroName by mutableStateOf("")
    var sshHost by mutableStateOf("")
    var sshPort by mutableStateOf("22")
    var sshUser by mutableStateOf("")
    var sshPassword by mutableStateOf("")
    var sshKeyPath by mutableStateOf("")
    var colorIndex by mutableIntStateOf(0)

    /* ── Derived state ── */
    var isEditing by mutableStateOf(false)
        private set

    var nameError by mutableStateOf(false)
        private set

    var sshAuthMethod by mutableStateOf(SshAuthMethod.PASSWORD)

    enum class SshAuthMethod { PASSWORD, KEY }

    private val _saved = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val saved: SharedFlow<Boolean> = _saved.asSharedFlow()

    init {
        if (workspaceId != null) {
            loadWorkspace(workspaceId)
        }
    }

    /* ── Field updaters ── */
    fun updateName(value: String) {
        name = value
        if (value.isNotBlank()) nameError = false
    }

    fun updateType(value: WorkspaceType) { type = value }

    fun updateDistro(value: String) { distroName = value }

    fun updateSshHost(value: String) { sshHost = value }

    fun updateSshPort(value: String) {
        // Only allow digits, max 5 chars
        if (value.all { it.isDigit() } && value.length <= 5) {
            sshPort = value
        }
    }

    fun updateSshUser(value: String) { sshUser = value }

    fun updateSshPassword(value: String) { sshPassword = value }

    fun updateSshKeyPath(value: String) { sshKeyPath = value }

    fun updateSshAuthMethod(value: SshAuthMethod) { sshAuthMethod = value }

    fun updateColorIndex(index: Int) { colorIndex = index }

    /* ── Actions ── */

    /**
     * Validates the form and persists the workspace. Emits on [saved] when done.
     */
    fun saveWorkspace() {
        if (name.isBlank()) {
            nameError = true
            return
        }

        viewModelScope.launch {
            val workspace = Workspace(
                id = workspaceId ?: UUID.randomUUID().toString(),
                name = name.trim(),
                type = type,
                distroName = if (type == WorkspaceType.PROOT_DISTRO) distroName.trim().ifBlank { null } else null,
                sshHost = if (type == WorkspaceType.SSH) sshHost.trim().ifBlank { null } else null,
                sshPort = sshPort.toIntOrNull() ?: 22,
                sshUser = if (type == WorkspaceType.SSH) sshUser.trim().ifBlank { null } else null,
                sshPassword = if (type == WorkspaceType.SSH && sshAuthMethod == SshAuthMethod.PASSWORD)
                    sshPassword.trim().ifBlank { null } else null,
                sshKeyPath = if (type == WorkspaceType.SSH && sshAuthMethod == SshAuthMethod.KEY)
                    sshKeyPath.trim().ifBlank { null } else null,
                colorIndex = colorIndex,
                sortOrder = 0,
                createdAt = System.currentTimeMillis(),
                lastAccessed = System.currentTimeMillis()
            )
            repository.saveWorkspace(workspace)
            _saved.tryEmit(true)
        }
    }

    /**
     * Deletes the workspace (only valid in edit mode). Emits on [saved] when done.
     */
    fun deleteWorkspace() {
        val id = workspaceId ?: return
        viewModelScope.launch {
            repository.getWorkspaceById(id)?.let { ws ->
                repository.deleteWorkspace(ws)
                _saved.tryEmit(true)
            }
        }
    }

    /* ── Internals ── */

    private fun loadWorkspace(id: String) {
        viewModelScope.launch {
            val ws = repository.getWorkspaceById(id) ?: return@launch
            name = ws.name
            type = ws.type
            distroName = ws.distroName ?: ""
            sshHost = ws.sshHost ?: ""
            sshPort = ws.sshPort.toString()
            sshUser = ws.sshUser ?: ""
            sshPassword = ws.sshPassword ?: ""
            sshKeyPath = ws.sshKeyPath ?: ""
            colorIndex = ws.colorIndex
            isEditing = true

            // Guess auth method from saved data
            sshAuthMethod = if (!ws.sshKeyPath.isNullOrBlank()) SshAuthMethod.KEY
                           else SshAuthMethod.PASSWORD
        }
    }

    companion object {
        fun Factory(workspaceId: String? = null): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WorkspaceEditorViewModel(workspaceId) as T
                }
            }
    }
}
