package com.termoot.data.local.converter

import androidx.room.TypeConverter
import com.termoot.domain.model.WorkspaceType

class Converters {

    @TypeConverter
    fun fromWorkspaceType(value: WorkspaceType): String {
        return value.name
    }

    @TypeConverter
    fun toWorkspaceType(value: String): WorkspaceType {
        return try {
            WorkspaceType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            WorkspaceType.LOCAL_SHELL
        }
    }
}
