package com.termoot.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "distro_name")
    val distroName: String? = null,

    @ColumnInfo(name = "ssh_host")
    val sshHost: String? = null,

    @ColumnInfo(name = "ssh_port")
    val sshPort: Int = 22,

    @ColumnInfo(name = "ssh_user")
    val sshUser: String? = null,

    @ColumnInfo(name = "ssh_password")
    val sshPassword: String? = null,

    @ColumnInfo(name = "ssh_key_path")
    val sshKeyPath: String? = null,

    @ColumnInfo(name = "color_index")
    val colorIndex: Int = 0,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_accessed")
    val lastAccessed: Long = System.currentTimeMillis()
)
