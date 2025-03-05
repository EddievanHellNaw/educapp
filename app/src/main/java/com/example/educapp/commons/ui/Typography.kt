package com.example.educapp.commons.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.unit.sp
import com.example.educapp.R
import androidx.compose.material3.Typography



// Define the provider
val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Define the font family
val nunitoFamily = FontFamily(
    Font(
        googleFont = GoogleFont("Nunito"),
        fontProvider = fontProvider, // Provide the fontProvider here
        weight = FontWeight.Normal
    ),
    Font(
        googleFont = GoogleFont("Nunito"),
        fontProvider = fontProvider,
        weight = FontWeight.Bold
    )
)

val CustomTypography = Typography(
    // Headlines (large titles)
    headlineLarge = TextStyle(
        fontFamily = nunitoFamily,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    ),
    headlineMedium = TextStyle(
        fontFamily = nunitoFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headlineSmall = TextStyle(
        fontFamily = nunitoFamily,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium
    ),

    // Body text
    bodyLarge = TextStyle(
        fontFamily = nunitoFamily,
        fontSize = 18.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontFamily = nunitoFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    ),
    bodySmall = TextStyle(
        fontFamily = nunitoFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal
    ),

    // Labels
    labelLarge = TextStyle(
        fontFamily = nunitoFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    ),
    labelMedium = TextStyle(
        fontFamily = nunitoFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
    ),
    labelSmall = TextStyle(
        fontFamily = nunitoFamily,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
    )
)


