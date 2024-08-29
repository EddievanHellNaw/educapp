package com.example.educapp.ui

import android.app.Application
import com.google.firebase.BuildConfig
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this) // Initialize Firebase
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree()) // Initialize Timber for logging in debug mode
        }
        // You can add analytics initialization here later if needed
    }
}