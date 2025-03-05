package com.example.educapp.commons.teacher.attendance

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI

class OcrHelper(context: Context) {
    private val tessBaseAPI = TessBaseAPI().apply {
        init(context.getExternalFilesDir(null)?.absolutePath, "eng")
    }

    fun extractTextFromBitmap(bitmap: Bitmap): String {
        tessBaseAPI.setImage(bitmap)
        return tessBaseAPI.utF8Text
    }
}