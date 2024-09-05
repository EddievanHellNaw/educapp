package com.example.educapp.ui.ui

import android.app.Application
import android.webkit.WebView
import com.google.firebase.FirebaseApp
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.compose.BuildConfig
import org.koin.core.context.startKoin
import timber.log.Timber

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        WebView.setWebContentsDebuggingEnabled(true)
        Timber.d("Timber is initialized")
        startKoin {
            androidLogger()
            androidContext(this@MyApplication)
            modules(appModule) // Add your Koin modules here
        }
    }
}