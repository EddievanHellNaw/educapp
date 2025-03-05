package com.example.educapp.commons.teacher.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.educapp.commons.ui.AppTheme
import com.example.educapp.commons.ui.CinnamoSpiralColorScheme
import com.example.educapp.commons.ui.GhostColorScheme
import com.example.educapp.commons.ui.HapticButton
import com.example.educapp.commons.ui.ItalianHunterColorScheme
import com.example.educapp.commons.ui.ItalianPlumberColorScheme
import com.example.educapp.commons.ui.ValiantAutonomyColorScheme
import com.example.educapp.commons.ui.hapticClickable

@Composable
fun SettingsMainScreen(
    viewModel: SettingsViewModel
) {
    // Observe the current theme and timer from the ViewModel
    val currentTheme by viewModel.theme.collectAsState()
    val currentTimer by viewModel.timerDuration.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {


            // THEME PICKER
            // A list of the available themes (assuming you have an AppTheme enum)
            val allThemes = listOf(
                AppTheme.Ghost,
                AppTheme.CinnamoSpiral,
                AppTheme.ItalianPlumber,
                AppTheme.ItalianHunter,
                AppTheme.ValiantAutonomy
            )

            // For each theme, we show a Row with a RadioButton + label + sample color row
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp)
            ) {
                Text("Select Theme", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))

                allThemes.forEach { themeOption ->
                    ThemeRow(
                        theme = themeOption,
                        isSelected = (currentTheme == themeOption),
                        onSelect = { viewModel.updateTheme(themeOption) }
                    )
                }

            Spacer(modifier = Modifier.height(24.dp))

            // TIMER DURATION
            Text("Timer Settings", style = MaterialTheme.typography.headlineMedium)

            TimerDurationSetting(
                currentTimerMinutes = currentTimer,
                onTimerUpdate = { newMinutes ->
                    viewModel.updateTimerDuration(newMinutes) // store new value in DataStore/Flow
                }
            )
        }
    }
}

@Composable
fun ThemeRow(
    theme: AppTheme,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    // Replace this row with a GradientCard (or just Card) for the "boxed" look.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .hapticClickable { onSelect() },  // So tapping the whole row selects
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
            Text(
                text = theme.name,
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.weight(1f))

            // A small row of color circles showing the theme's palette
            SamplePaletteRow(theme)
        }
    }
}


/**
 * A composable that shows a small row of “sample squares” or circles
 * that represent some key colors from each theme. This is purely illustrative.
 */
@Composable
fun SamplePaletteRow(themeOption: AppTheme) {
    // For demonstration, we might show: primary, secondary, surface, background
    // You can define a function that returns the color scheme for each theme without changing the global theme
    val (primaryColor, secondaryColor, surfaceColor, backgroundColor) = getSampleColors(themeOption)

    Row(
        modifier = Modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(primaryColor, shape = CircleShape)
        )
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(secondaryColor, shape = CircleShape)
        )
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(surfaceColor, shape = CircleShape)
        )
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(backgroundColor, shape = CircleShape)
        )
    }
}

/**
 * For each theme, return a few representative colors. You can do something like:
 */
@Composable
fun getSampleColors(themeOption: AppTheme): List<Color> {
    return when (themeOption) {
        AppTheme.Ghost -> listOf(
            GhostColorScheme.primary,
            GhostColorScheme.secondary,
            GhostColorScheme.surface,
            GhostColorScheme.background
        )
        AppTheme.CinnamoSpiral -> listOf(
            CinnamoSpiralColorScheme.primary,
            CinnamoSpiralColorScheme.secondary,
            CinnamoSpiralColorScheme.surface,
            CinnamoSpiralColorScheme.background
        )
        AppTheme.ItalianPlumber -> listOf(
            ItalianPlumberColorScheme.primary,
            ItalianPlumberColorScheme.secondary,
            ItalianPlumberColorScheme.surface,
            ItalianPlumberColorScheme.background
        )
        AppTheme.ItalianHunter -> listOf(
            ItalianHunterColorScheme.primary,
            ItalianHunterColorScheme.secondary,
            ItalianHunterColorScheme.surface,
            ItalianHunterColorScheme.background
        )
        AppTheme.ValiantAutonomy -> listOf(
            ValiantAutonomyColorScheme.primary,
            ValiantAutonomyColorScheme.secondary,
            ValiantAutonomyColorScheme.surface,
            ValiantAutonomyColorScheme.background
        )
    }
}

@Composable
fun TimerDurationSetting(
    currentTimerMinutes: Int,            // e.g., 5 means 5 minutes
    onTimerUpdate: (Int) -> Unit         // Callback to save the chosen minutes
) {
    var showDialog by remember { mutableStateOf(false) }

    // Button that opens the dialog
    HapticButton(
        onClick = { showDialog = true }
    ) {
        Text("Set Timer Duration (Currently: $currentTimerMinutes minutes)")
    }

    // Dialog appears if showDialog = true
    if (showDialog) {
        var selectedMinutes by remember { mutableStateOf(currentTimerMinutes) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Select Timer Duration") },
            text = {
                // A scrollable list from 1..10 minutes
                val possibleMinutes = (1..10).toList()

                // You can use LazyColumn for scrolling. We'll highlight
                // whichever matches `selectedMinutes`.
                LazyColumn {
                    items(possibleMinutes) { minute ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .hapticClickable {
                                    selectedMinutes = minute
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedMinutes == minute),
                                onClick = { selectedMinutes = minute }
                            )
                            Text(
                                text = "$minute minutes",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                HapticButton(onClick = {
                    // When user confirms, call onTimerUpdate with the selected value
                    onTimerUpdate(selectedMinutes)
                    showDialog = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                HapticButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
