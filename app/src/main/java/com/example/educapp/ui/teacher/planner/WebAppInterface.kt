package com.example.educapp.ui.teacher.planner

import android.webkit.JavascriptInterface
import android.content.Context

class WebAppInterface(private val context: Context, private val onContentChange: (String) -> Unit) {
    @JavascriptInterface
    fun onEditorChange(newContent: String) {
        onContentChange(newContent)
    }

}