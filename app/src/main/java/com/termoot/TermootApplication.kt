package com.termoot

import android.app.Application

class TermootApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: TermootApplication
            private set
    }
}
