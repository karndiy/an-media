package com.anmedia.player

import android.app.Application
import com.anmedia.player.di.AppContainer

class AnMediaApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
