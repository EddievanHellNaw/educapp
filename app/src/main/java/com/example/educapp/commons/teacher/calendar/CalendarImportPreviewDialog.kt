package com.example.educapp.commons.teacher.calendar

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.educapp.commons.ui.HapticButton

@Composable
fun CalendarImportPreviewDialog(
    draft: CalendarImportDraft,
    onUseCalendar: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var preview by remember(draft.uri) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }

    var errorMessage by remember(draft.uri) {
        mutableStateOf<String?>(null)
    }

    var isLoading by remember(draft.uri) {
        mutableStateOf(true)
    }

    LaunchedEffect(draft.uri) {
        isLoading = true
        errorMessage = null

        try {
            preview = CalendarImportPreviewLoader.loadPreview(
                context = context,
                draft = draft
            )
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Unable to preview this calendar."
        } finally {
            isLoading = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Academic Calendar Preview",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = draft.displayName,
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = when (draft.documentType) {
                        CalendarDocumentType.IMAGE -> "Image"
                        CalendarDocumentType.PDF -> "PDF · ${draft.pageCount} page(s)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    errorMessage != null -> {
                        Text(
                            text = errorMessage ?: "Preview error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    preview != null -> {
                        Image(
                            bitmap = preview!!.asImageBitmap(),
                            contentDescription = "Preview of ${draft.displayName}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 500.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HapticButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    HapticButton(
                        onClick = onUseCalendar,
                        enabled = preview != null && errorMessage == null && !isLoading
                    ) {
                        Text("Use This Calendar")
                    }
                }
            }
        }
    }
}
