package com.example.educapp.commons.annotation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.educapp.commons.ui.GradientCard
import java.net.URLEncoder

@Composable
fun AnnotationMainScreen(navController: NavHostController) {
    // List of options
    val englishOptions = listOf("English 1", "English 2", "English 3", "English 4", "English 5")

    val gradientColors = listOf(
        Color(0xFFFD0331), // English 1
        Color(0xFF0045F5), // English 2
        Color(0xFF008000), // English 3
        Color(0xFFF57C00), // English 4
        Color(0xFF5E006E)  // English 5
    )

    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Select a PDF to annotate",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            // Use LazyColumn if you expect many items; here five items are fine in a Column.
            englishOptions.forEachIndexed { index, option ->
                GradientCard(
                    onClick = {
                        navController.navigate("annotation/${URLEncoder.encode(option, "UTF-8")}")
                    },
                    gradientBrush = Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            gradientColors[index]
                        )
                    ),
                    content = {
                        // Use your existing styling or customizations for the card's inner content.
                        Text(
                            text = option,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(16.dp).fillMaxWidth()
                        )
                    }
                )

                Spacer(modifier = Modifier.padding(8.dp))
            }
        }
    }
}