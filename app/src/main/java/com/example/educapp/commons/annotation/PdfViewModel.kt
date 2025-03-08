package com.example.educapp.commons.annotation

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.ImageType
import com.tom_roush.pdfbox.rendering.PDFRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException

class PdfViewModel : ViewModel() {
    sealed class PdfState {
        object Loading : PdfState()
        data class Success(val bitmap: Bitmap) : PdfState()
        data class Error(val message: String) : PdfState()
        object Timeout : PdfState()
    }

    private val _pdfState = mutableStateOf<PdfState>(PdfState.Loading)
    val pdfState: State<PdfState> = _pdfState

    // Progress tracking
    private val _loadingProgress = mutableStateOf(0)
    val loadingProgress: State<Int> = _loadingProgress

    private val LOAD_TIMEOUT_MS = 10_000L

    fun loadPdf(context: Context, assetFileName: String) {
        _pdfState.value = PdfState.Loading
        _loadingProgress.value = 0

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = withTimeoutOrNull(LOAD_TIMEOUT_MS) {
                    context.assets.open(assetFileName).use { inputStream ->
                        // 1) File opened
                        _loadingProgress.value = 25

                        PDDocument.load(inputStream).use { document ->
                            // 2) Document loaded
                            _loadingProgress.value = 50

                            val renderer = PDFRenderer(document).apply {
                                // Toggle if needed
                                setSubsamplingAllowed(false)
                            }

                            // 3) Renderer ready
                            _loadingProgress.value = 75

                            // Render at 150 DPI in RGB color space
                            val pageBitmap = renderer.renderImageWithDPI(
                                0,            // page index
                                300f,         // DPI
                                ImageType.ARGB // Force RGB color
                            )

                            // Optionally convert to ARGB_8888 if you prefer
                            val finalBitmap = pageBitmap.copy(Bitmap.Config.ARGB_8888, true)

                            // 4) Final progress update
                            _loadingProgress.value = 100

                            finalBitmap
                        }
                    }
                }

                when (result) {
                    null -> {
                        // Timeout occurred
                        withContext(Dispatchers.Main) {
                            _pdfState.value = PdfState.Timeout
                        }
                    }
                    else -> {
                        // Success
                        withContext(Dispatchers.Main) {
                            _pdfState.value = PdfState.Success(result)
                        }
                    }
                }
            } catch (e: Throwable) {
                // Handle errors (including cancellation)
                if (e is CancellationException) {
                    // Coroutine was cancelled (e.g., timeout)
                    withContext(Dispatchers.Main) {
                        _pdfState.value = PdfState.Timeout
                    }
                } else {
                    // Other errors
                    withContext(Dispatchers.Main) {
                        _pdfState.value = PdfState.Error(e.message ?: "Unknown error")
                    }
                }
            }
        }
    }
}
