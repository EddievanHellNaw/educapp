package com.example.educapp.commons.teacher.grading

import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition

/**
 * Custom PDFTextStripper that records the average Y-coordinate of text segments
 * that look like a student name. In our example, we assume a student name is a string
 * of at least 5 uppercase letters (and spaces).
 */
class StudentNamePositionExtractor : PDFTextStripper() {

    val nameYPositions = mutableMapOf<String, Float>()

    override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
        val cleaned = text.trim()
        val numericMarkers = setOf("30", "50", "20")

        // 1) Detect uppercase names (existing logic)
        if (cleaned.matches(Regex("^[A-ZÁÉÍÓÚÑ ]{5,}\$"))) {
            val avgY = textPositions.map { it.y }.average().toFloat()
            nameYPositions[cleaned] = avgY
        }
        // 2) Detect "30", "50", or "20" exactly
        else if (cleaned in numericMarkers) {
            val avgY = textPositions.map { it.y }.average().toFloat()
            nameYPositions[cleaned] = avgY
        }

        // Continue with default PDFTextStripper processing
        super.writeString(text, textPositions)
    }
}
