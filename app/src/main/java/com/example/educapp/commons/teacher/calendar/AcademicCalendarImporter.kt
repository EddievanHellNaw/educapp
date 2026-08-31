package com.example.educapp.commons.teacher.calendar

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Stage 1 representation of a document selected by the teacher.
 *
 * No OCR or calendar interpretation happens yet. This gives Stage 2 a clean,
 * validated input object instead of making the UI deal directly with files.
 */
data class CalendarImportDraft(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val documentType: CalendarDocumentType,
    val pageCount: Int
)

enum class CalendarDocumentType {
    IMAGE,
    PDF
}

class AcademicCalendarImporter {

    suspend fun prepare(
        context: Context,
        uri: Uri
    ): CalendarImportDraft = withContext(Dispatchers.IO) {

        // Keep read access when the provider allows persistable permissions.
        // Some providers do not support this, so failure is intentionally safe.
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Immediate preview/import still works with the temporary grant.
        }

        val resolver = context.contentResolver
        val displayName = readDisplayName(context, uri)
        val mimeType = resolver.getType(uri).orEmpty()

        val type = when {
            mimeType.equals("application/pdf", ignoreCase = true) ||
                displayName.endsWith(".pdf", ignoreCase = true) -> {
                CalendarDocumentType.PDF
            }

            mimeType.startsWith("image/", ignoreCase = true) -> {
                CalendarDocumentType.IMAGE
            }

            else -> {
                // Some document providers return no MIME type. Try decoding it
                // as an image before rejecting it.
                if (canDecodeImage(context, uri)) {
                    CalendarDocumentType.IMAGE
                } else {
                    throw IllegalArgumentException(
                        "Please choose an image or PDF academic calendar."
                    )
                }
            }
        }

        val pageCount = when (type) {
            CalendarDocumentType.IMAGE -> 1
            CalendarDocumentType.PDF -> readPdfPageCount(context, uri)
        }

        if (pageCount <= 0) {
            throw IllegalArgumentException("The selected calendar has no readable pages.")
        }

        CalendarImportDraft(
            uri = uri,
            displayName = displayName,
            mimeType = mimeType,
            documentType = type,
            pageCount = pageCount
        )
    }

    private fun readDisplayName(
        context: Context,
        uri: Uri
    ): String {
        var name: String? = null

        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    name = cursor.getString(index)
                }
            }
        }

        return name ?: uri.lastPathSegment ?: "Academic calendar"
    }

    private fun canDecodeImage(
        context: Context,
        uri: Uri
    ): Boolean {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        return options.outWidth > 0 && options.outHeight > 0
    }

    private fun readPdfPageCount(
        context: Context,
        uri: Uri
    ): Int {
        return context.contentResolver
            .openFileDescriptor(uri, "r")
            ?.use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    renderer.pageCount
                }
            }
            ?: 0
    }
}

/**
 * Loads only a display-sized bitmap. Stage 2 can add a separate full-resolution
 * loader for OCR/OpenCV without changing the UI contract.
 */
object CalendarImportPreviewLoader {

    suspend fun loadPreview(
        context: Context,
        draft: CalendarImportDraft,
        maxDimension: Int = 1600
    ): Bitmap = withContext(Dispatchers.IO) {
        when (draft.documentType) {
            CalendarDocumentType.IMAGE -> {
                loadImagePreview(context, draft.uri, maxDimension)
            }

            CalendarDocumentType.PDF -> {
                loadPdfPreview(context, draft.uri, maxDimension)
            }
        }
    }

    private fun loadImagePreview(
        context: Context,
        uri: Uri,
        maxDimension: Int
    ): Bitmap {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalArgumentException("Unable to decode the selected image.")
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(
                bounds.outWidth,
                bounds.outHeight,
                maxDimension
            )
        }

        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw IllegalArgumentException("Unable to open the selected image.")
    }

    private fun loadPdfPreview(
        context: Context,
        uri: Uri,
        maxDimension: Int
    ): Bitmap {
        return context.contentResolver
            .openFileDescriptor(uri, "r")
            ?.use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    if (renderer.pageCount == 0) {
                        throw IllegalArgumentException("The PDF has no pages.")
                    }

                    renderer.openPage(0).use { page ->
                        val largestSide = max(page.width, page.height).toFloat()
                        val scale = (maxDimension / largestSide)
                            .coerceAtMost(2f)
                            .coerceAtLeast(0.25f)

                        val width = (page.width * scale).toInt().coerceAtLeast(1)
                        val height = (page.height * scale).toInt().coerceAtLeast(1)

                        val bitmap = Bitmap.createBitmap(
                            width,
                            height,
                            Bitmap.Config.ARGB_8888
                        )

                        Canvas(bitmap).drawColor(Color.WHITE)

                        val matrix = Matrix().apply {
                            setScale(scale, scale)
                        }

                        page.render(
                            bitmap,
                            null,
                            matrix,
                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                        )

                        bitmap
                    }
                }
            }
            ?: throw IllegalArgumentException("Unable to open the selected PDF.")
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        maxDimension: Int
    ): Int {
        var sampleSize = 1

        while (
            width / sampleSize > maxDimension * 2 ||
            height / sampleSize > maxDimension * 2
        ) {
            sampleSize *= 2
        }

        return sampleSize
    }
}
