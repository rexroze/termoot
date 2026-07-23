package com.termoot.data.repository

import com.termoot.domain.model.Workspace
import com.termoot.domain.model.WorkspaceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * In-memory stub repository so the UI layer can be developed independently
 * of the real Room-backed implementation. Swap [StubWorkspaceRepository]
 * for the real [WorkspaceRepository] when the data layer is ready.
 */
class StubWorkspaceRepository : WorkspaceRepository {

    private val _workspaces = MutableStateFlow(
        listOf(
            Workspace(
                name = "Local Shell",
                type = WorkspaceType.LOCAL_SHELL,
                colorIndex = 0,
                sortOrder = 0,
                lastAccessed = System.currentTimeMillis() - 1_800_000           // 30 min ago
            ),
            Workspace(
                name = "Ubuntu 24.04",
                type = WorkspaceType.PROOT_DISTRO,
                distroName = "ubuntu",
                colorIndex = 1,
                sortOrder = 1,
                lastAccessed = System.currentTimeMillis() - 7_200_000           // 2 h ago
            ),
            Workspace(
                name = "Debian Bookworm",
                type = WorkspaceType.PROOT_DISTRO,
                distroName = "debian",
                colorIndex = 4,
                sortOrder = 2,
                lastAccessed = System.currentTimeMillis() - 86_400_000          // 1 d ago
            ),
            Workspace(
                name = "My Server",
                type = WorkspaceType.SSH,
                sshHost = "vps.example.com",
                sshPort = 22,
                sshUser = "root",
                sshPassword = null,
                sshKeyPath = "/data/data/com.termoot/files/ssh/id_ed25519",
                colorIndex = 2,
                sortOrder = 3,
                lastAccessed = System.currentTimeMillis() - 360_000             // 6 min ago
            ),
            Workspace(
                name = "Arch Linux",
                type = WorkspaceType.PROOT_DISTRO,
                distroName = "archlinux",
                colorIndex = 3,
                sortOrder = 4,
                lastAccessed = System.currentTimeMillis() - 14_400_000          // 4 h ago
            ),
            Workspace(
                name = "Raspberry Pi",
                type = WorkspaceType.SSH,
                sshHost = "192.168.1.100",
                sshPort = 22,
                sshUser = "pi",
                sshPassword = "raspberry",
                colorIndex = 5,
                sortOrder = 5,
                lastAccessed = System.currentTimeMillis() - 604_800_000         // 7 d ago
            )
        )
    )

    override fun getAllWorkspaces(): Flow<List<Workspace>> =
        _workspaces.asStateFlow()

    override suspend fun getWorkspaceById(id: String): Workspace? =
        _workspaces.value.find { it.id == id }

    override suspend fun saveWorkspace(workspace: Workspace) {
        val current = _workspaces.value.toMutableList()
        val idx = current.indexOfFirst { it.id == workspace.id }
        if (idx >= 0) {
            current[idx] = workspace
        } else {
            current.add(workspace)
        }
        _workspaces.value = current
    }

    override suspend fun deleteWorkspace(workspace: Workspace) {
        _workspaces.value = _workspaces.value.filter { it.id != workspace.id }
    }

    override suspend fun updateLastAccessed(id: String, time: Long) {
        val current = _workspaces.value.toMutableList()
        val idx = current.indexOfFirst { it.id == id }
        if (idx >= 0) {
            current[idx] = current[idx].copy(lastAccessed = time)
            _workspaces.value = current
        }
    }
}
