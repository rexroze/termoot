package com.termoot.domain.model

import java.util.UUID

data class Workspace(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: WorkspaceType,
    val distroName: String? = null,       // For PROOT_DISTRO: "ubuntu", "debian", etc.
    val sshHost: String? = null,           // For SSH
    val sshPort: Int = 22,
    val sshUser: String? = null,
    val sshPassword: String? = null,
    val sshKeyPath: String? = null,        // Path to private key
    val colorIndex: Int = 0,               // Index into accent color palette
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessed: Long = System.currentTimeMillis()
)
