package com.example.educapp.commons.ui

import android.app.Application
import android.webkit.WebView
import com.example.educapp.commons.classwork.LocalDateSerializer
import com.google.firebase.FirebaseApp
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.compose.BuildConfig
import org.koin.core.context.startKoin
import org.koin.dsl.module
import timber.log.Timber
import java.time.LocalDate

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        WebView.setWebContentsDebuggingEnabled(true)
        Timber.d("Timber is initialized")
        val serializationModule = module {
            single<Json> {  // Explicitly specify the Json type
                Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                    isLenient = true
                    serializersModule = SerializersModule {
                        contextual(LocalDate::class, LocalDateSerializer)
                    }
                }
            }
        }
        startKoin {
            androidLogger()
            androidContext(this@MyApplication)
            modules(appModule,serializationModule) // Add your Koin modules here
        }
    }
}