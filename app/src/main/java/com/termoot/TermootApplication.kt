package com.termoot

import android.app.Application
import com.termoot.data.local.AppDatabase
import com.termoot.data.repository.RoomWorkspaceRepository

class TermootApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    /** Room database singleton */
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    /** Room-backed workspace repository singleton */
    val repository: RoomWorkspaceRepository by lazy {
        RoomWorkspaceRepository(database.workspaceDao())
    }

    companion object {
        lateinit var instance: TermootApplication
            private set
    }
}
