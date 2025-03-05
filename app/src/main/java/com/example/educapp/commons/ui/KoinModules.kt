package com.example.educapp.commons.ui

import com.example.educapp.commons.RegistrationViewModel
import com.example.educapp.commons.assistant.network.AssistantRepository
import com.example.educapp.commons.teacher.attendance.AttendanceViewModel
import com.example.educapp.commons.teacher.grading.GradesViewModel
import com.example.educapp.commons.teacher.settings.SettingsRepository
import com.example.educapp.commons.teacher.settings.SettingsViewModel
import com.example.myapp.teacher.assistant.AssistantViewModel
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // ViewModels
    viewModel { RegistrationViewModel(androidContext()) }

    viewModel { AttendanceViewModel(get()) }

    single<AssistantRepository> { AssistantRepository() }
    factory { (userRole: String) ->
        AssistantViewModel(
            userRole = userRole,
            repository = get()  // or whatever your repository definition is
        )
    }

    viewModel { GradesViewModel(get())}

    single<SettingsRepository> { SettingsRepository(androidContext()) }
    viewModel { SettingsViewModel(get()) }

    // Firebase instance
    single { FirebaseFirestore.getInstance() }
}