package com.example.educapp.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.educapp.R

@Composable
fun EduLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.thinking_cap),
        contentDescription = "Educapp Logo",
        modifier = modifier
    )
}