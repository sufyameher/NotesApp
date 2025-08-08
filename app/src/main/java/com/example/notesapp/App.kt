 package com.example.notesapp

import android.app.Application
import android.content.Context
import androidx.databinding.library.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber


 @HiltAndroidApp
 class App : Application() {

    override fun onCreate() {
        super.onCreate()
        context = applicationContext

        if (isDebug()) Timber.plant(Timber.DebugTree())
        Timber.d("App context initialized")
    }

    companion object {
        lateinit var context: Context
            private set
    }

     inline fun isDebug() = BuildConfig.DEBUG
 }
