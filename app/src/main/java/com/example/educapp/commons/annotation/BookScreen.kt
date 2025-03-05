package com.example.educapp.commons.annotation

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun BookAnnotationScreen(navController: NavHostController, englishOption: String) {
    // Map the chosen option to an asset file name.
    val assetFileName = when (englishOption) {
        "English 1" -> "english1.pdf"
        "English 2" -> "english2.pdf"
        "English 3" -> "english3.pdf"
        "English 4" -> "english4.pdf"
        "English 5" -> "english5.pdf"
        else -> "english1.pdf"
    }

    val context = LocalContext.current

    // Asynchronously load the PDF from assets using PDFBox
    val pdfBitmap by produceState<Bitmap?>(initialValue = null, key1 = assetFileName) {
        try {
            // Load the PDF and render it
            val bitmap = context.assets.open(assetFileName).use { inputStream ->
                val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(inputStream)
                val renderer = com.tom_roush.pdfbox.rendering.PDFRenderer(document)
                val renderedBitmap = renderer.renderImage(0)
                document.close()
                renderedBitmap // Return bitmap from `use` block
            }
            value = bitmap // Assign OUTSIDE the nested lambda
        } catch (e: Exception) {
            e.printStackTrace()
            value = null // Assign in case of error
        }
    }


    if (pdfBitmap != null) {
        // Display the PDF along with the scribble (drawing) overlay.
        PdfAnnotationScreen(pdfBitmap = pdfBitmap!!)
    } else {
        // Display a loading indicator while the PDF loads.
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
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