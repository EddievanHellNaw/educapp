package com.example.educapp.commons.ui

import com.example.educapp.commons.RegistrationViewModel
import com.example.educapp.commons.UserRole
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
import com.example.educapp.commons.assistant.AssistantViewModel
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // ViewModels
    viewModel { RegistrationViewModel(androidContext()) }
    viewModel { AttendanceViewModel(get()) }
    viewModel { GradesViewModel(get())}
    viewModel { SettingsViewModel(get()) }
    viewModel {
        ClassworkViewModel(
            repository = get(),
            savedStateHandle = get() // Get SavedStateHandle from Koin
        )
    }
    viewModel { (userRole: UserRole, groupId: String) ->
        AssistantViewModel(
            userRole = userRole,
            groupId = groupId,
            repository = get(),
            classworkViewModel = get(),
            authRepository = get()
        )
    }
    // Repositories
    single<ClassworkRepository> { FirebaseClassworkRepository(get()) } // New Repository
    single<SettingsRepository> { SettingsRepository(androidContext()) }
    single<AssistantRepository> { AssistantRepositoryImpl(get()) }
    single<AuthRepository> { AuthRepository() }


    // Firebase
    single { FirebaseFirestore.getInstance() }
}