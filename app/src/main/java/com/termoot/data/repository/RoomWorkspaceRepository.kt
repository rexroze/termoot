package com.termoot.data.repository

import com.termoot.data.local.dao.WorkspaceDao
import com.termoot.data.local.entity.WorkspaceEntity
import com.termoot.domain.model.Workspace
import com.termoot.domain.model.WorkspaceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed implementation of [WorkspaceRepository].
 *
 * Maps between domain [Workspace] objects and [WorkspaceEntity] database rows,
 * and delegates all persistence operations to [WorkspaceDao].
 */
class RoomWorkspaceRepository(
    private val dao: WorkspaceDao
) : WorkspaceRepository {

    override fun getAllWorkspaces(): Flow<List<Workspace>> {
        return dao.getAll().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getWorkspaceById(id: String): Workspace? {
        return dao.getById(id)?.toDomainModel()
    }

    override suspend fun saveWorkspace(workspace: Workspace) {
        dao.insert(workspace.toEntity())
    }

    override suspend fun deleteWorkspace(workspace: Workspace) {
        dao.delete(workspace.toEntity())
    }

    override suspend fun updateLastAccessed(id: String, time: Long) {
        dao.updateLastAccessed(id, time)
    }

    private fun WorkspaceEntity.toDomainModel(): Workspace {
        return Workspace(
            id = id,
            name = name,
            type = try {
                WorkspaceType.valueOf(type)
            } catch (e: IllegalArgumentException) {
                WorkspaceType.LOCAL_SHELL
            },
            distroName = distroName,
            sshHost = sshHost,
            sshPort = sshPort,
            sshUser = sshUser,
            sshPassword = sshPassword,
            sshKeyPath = sshKeyPath,
            colorIndex = colorIndex,
            sortOrder = sortOrder,
            createdAt = createdAt,
            lastAccessed = lastAccessed
        )
    }

    private fun Workspace.toEntity(): WorkspaceEntity {
        return WorkspaceEntity(
            id = id,
            name = name,
            type = type.name,
            distroName = distroName,
            sshHost = sshHost,
            sshPort = sshPort,
            sshUser = sshUser,
            sshPassword = sshPassword,
            sshKeyPath = sshKeyPath,
            colorIndex = colorIndex,
            sortOrder = sortOrder,
            createdAt = createdAt,
            lastAccessed = lastAccessed
        )
    }
}
