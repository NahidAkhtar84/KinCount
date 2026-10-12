package com.nahid.kincount

import android.app.Application
import android.content.Context

class KinCountApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/**
 * Manual DI root — no Hilt by design (PLAN.md §3).
 * Room, repositories, the sync engine, and the GLM client register here as M1+ land.
 */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
}

val Context.appContainer: AppContainer
    get() = (applicationContext as KinCountApp).container
