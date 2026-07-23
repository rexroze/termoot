package com.termoot.data.repository

import com.termoot.domain.model.Workspace
import kotlinx.coroutines.flow.Flow

interface WorkspaceRepository {
    fun getAllWorkspaces(): Flow<List<Workspace>>
    suspend fun getWorkspaceById(id: String): Workspace?
    suspend fun saveWorkspace(workspace: Workspace)
    suspend fun deleteWorkspace(workspace: Workspace)
    suspend fun updateLastAccessed(id: String, time: Long)
}
