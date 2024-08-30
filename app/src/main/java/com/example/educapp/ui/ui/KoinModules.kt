package com.example.educapp.ui.ui

import com.example.educapp.ui.RegistrationViewModel
import com.example.educapp.ui.teacher.attendance.AttendanceViewModel
import com.example.educapp.ui.teacher.planner.PlannerViewModel
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // ViewModels
    viewModel { RegistrationViewModel(androidContext()) }
    viewModel { AttendanceViewModel(get()) }
    viewModel { PlannerViewModel() }

    // Firebase instance
    single { FirebaseFirestore.getInstance() }
}