package com.termoot.domain.model

enum class WorkspaceType {
    LOCAL_SHELL,   // Plain Termux bash shell
    PROOT_DISTRO,  // Proot-distro login (e.g., ubuntu, debian, arch)
    SSH            // Remote SSH connection
}
