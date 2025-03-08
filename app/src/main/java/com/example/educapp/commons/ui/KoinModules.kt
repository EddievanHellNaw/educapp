package com.example.educapp.commons.ui

import com.example.educapp.commons.RegistrationViewModel
import com.example.educapp.commons.assistant.network.AssistantRepository
import com.example.educapp.commons.assistant.network.AssistantRepositoryImpl
import com.example.educapp.commons.assistant.network.AuthRepository
import com.example.educapp.commons.classwork.ClassworkRepository
import com.example.educapp.commons.classwork.ClassworkViewModel
import com.example.educapp.commons.classwork.FirebaseClassworkRepository
import com.example.educapp.commons.teacher.attendance.AttendanceViewModel
import com.example.educapp.commons.teacher.grading.GradesViewModel
import com.example.educapp.commons.teacher.settings.SettingsRepository
import com.example.educapp.commons.teacher.settings.SettingsViewModel
import com.example.myapp.teacher.assistant.AssistantViewModel
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val appModule = module {
    // ViewModels
    viewModel { RegistrationViewModel(androidContext()) }
    viewModel { AttendanceViewModel(get()) }
    viewModel { GradesViewModel(get())}
    viewModel { SettingsViewModel(get()) }
    viewModel { (groupId: String) ->
        ClassworkViewModel(
            repository = get(),
            groupId = groupId
        )
    } // New ViewModel
    viewModel { params ->
        AssistantViewModel(
            userRole = params.get(),
            groupId = params.get(),
            repository = get(),
            classworkViewModel = get { parametersOf(params.get<String>()) }, // Pass groupId
            authRepository = get()
        )
    }
    // Repositories
    single<ClassworkRepository> {
        FirebaseClassworkRepository(firestore = get())
    } // New Repository
    single<SettingsRepository> { SettingsRepository(androidContext()) }
    single<AssistantRepository> { AssistantRepositoryImpl(get()) }
    single<AuthRepository> { AuthRepository() }


    // Firebase
    single { FirebaseFirestore.getInstance() }
}