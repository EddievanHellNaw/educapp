package com.example.educapp.commons.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun CircularButton(
    image: ImageBitmap? = null,
    imageResId: Int? = null,
    isLarge: Boolean = false,
    onClick: () -> Unit) {
    val size = if (isLarge) 64.dp else 48.dp

    HapticButton (
        onClick = onClick,
        modifier = Modifier
            .size(size)
            .background(Color.LightGray, shape = CircleShape),
        contentPadding = PaddingValues(0.dp)
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else if (imageResId != null) {
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
