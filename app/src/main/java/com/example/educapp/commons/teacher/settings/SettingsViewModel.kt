package com.example.educapp.commons.teacher.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.educapp.commons.ui.AppTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {


    // Block until the datastore emits its first value so you have a proper default
    private val defaultTheme: AppTheme = runBlocking {
        repository.themeFlow.first()    // This returns the saved theme or AppTheme.Ghost if none is saved.
    }

    val theme = repository.themeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = defaultTheme
        )

    val timerDuration = repository.timerDurationFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 60
        )

    fun updateTheme(newTheme: AppTheme) {
        viewModelScope.launch {
            repository.saveTheme(newTheme)
        }
    }

    fun updateTimerDuration(seconds: Int) {
        viewModelScope.launch {
            repository.saveTimerDuration(seconds)
        }
    }
}
