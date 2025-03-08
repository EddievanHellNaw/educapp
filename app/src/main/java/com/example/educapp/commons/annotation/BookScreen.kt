package com.example.educapp.commons.annotation

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.educapp.commons.ui.HapticButton

@Composable
fun BookAnnotationScreen(navController: NavHostController, englishOption: String) {
    val assetFileName = when (englishOption) {
        "English 1" -> "books/english1.pdf"
        "English 2" -> "books/english2.pdf"
        "English 3" -> "books/english3.pdf"
        "English 4" -> "books/english4.pdf"
        "English 5" -> "books/english5.pdf"
        else -> "english1.pdf"
    }

    val viewModel: PdfViewModel = viewModel(key = "pdf-$englishOption")
    val context = LocalContext.current
    val pdfState by viewModel.pdfState
    val loadingProgress by viewModel.loadingProgress

    LaunchedEffect(englishOption) {
        viewModel.loadPdf(context, assetFileName)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = pdfState) {
            is PdfViewModel.PdfState.Loading -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading PDF: ${loadingProgress}%")
                }
            }

            is PdfViewModel.PdfState.Success -> {
                PdfAnnotationScreen(pdfBitmap = state.bitmap)
            }

            is PdfViewModel.PdfState.Error -> {
                Text(
                    text = "Error: ${state.message}",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Red
                )
            }

            is PdfViewModel.PdfState.Timeout -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Timeout loading PDF", color = Color.Red)
                    HapticButton (onClick = { viewModel.loadPdf(context, assetFileName) }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}


@Composable
fun PdfAnnotationScreen(pdfBitmap: Bitmap) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf(listOf<Offset>()) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = pdfBitmap.asImageBitmap(),
            contentDescription = "PDF Page",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // CORRECTED: Use foundation.Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> currentStroke = listOf(offset) },
                        onDrag = { change, _ -> currentStroke = currentStroke + change.position },
                        onDragEnd = {
                            strokes.add(currentStroke)
                            currentStroke = emptyList()
                        }
                    )
                }
        ) {
            // Draw strokes (same as before)
            for (stroke in strokes) {
                if (stroke.size > 1) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(stroke.first().x, stroke.first().y)
                        stroke.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        path = path,
                        color = Color.Black,
                        style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
            if (currentStroke.size > 1) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(currentStroke.first().x, currentStroke.first().y)
                    currentStroke.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = path,
                    color = Color.Black,
                    style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}