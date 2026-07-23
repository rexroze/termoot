package com.termoot.ui.screens.workspaces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.termoot.data.repository.StubWorkspaceRepository
import com.termoot.data.repository.WorkspaceRepository
import com.termoot.domain.model.Workspace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkspaceListViewModel(
    private val repository: WorkspaceRepository = StubWorkspaceRepository()
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** All workspaces from the repository (unfiltered). */
    private val allWorkspaces: StateFlow<List<Workspace>> =
        repository.getAllWorkspaces()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Workspaces filtered by the current search query. */
    val filteredWorkspaces: StateFlow<List<Workspace>> = combine(
        allWorkspaces, _searchQuery
    ) { workspaces, query ->
        if (query.isBlank()) workspaces
        else workspaces.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        loadWorkspaces()
    }

    fun loadWorkspaces() {
        // The Flow from the repository is already being collected via stateIn above.
        // This method exists as a hook for explicit pull-to-refresh later.
    }

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun deleteWorkspace(workspace: Workspace) {
        viewModelScope.launch {
            repository.deleteWorkspace(workspace)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WorkspaceListViewModel(StubWorkspaceRepository()) as T
            }
        }
    }
}
