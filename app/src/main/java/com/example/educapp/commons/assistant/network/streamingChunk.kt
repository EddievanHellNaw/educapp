package com.example.educapp.commons.assistant.network

data class StreamingChunk(
    val id: String? = null,
    val `object`: String? = null,
    val created: Long? = null,
    val model: String? = null,
    val choices: List<StreamChoice>? = null
)

data class StreamChoice(
    val delta: Delta? = null,
    val index: Int? = null,
    val finish_reason: String? = null
)

data class Delta(
    val role: String? = null,
    val content: String? = null,
    val reasoning_content: String? = null // New field for chain-of-thought tokens
)
