package com.example.educapp.commons.assistant.network

import kotlinx.serialization.Serializable


@Serializable
data class StreamingChunk(
    val id: String? = null,
    val `object`: String? = null,
    val created: Long? = null,
    val model: String? = null,
    val choices: List<StreamChoice>? = null
)

@Serializable
data class StreamChoice(
    val delta: Delta? = null,
    val index: Int? = null,
    val finish_reason: String? = null
)

@Serializable
data class Delta(
    val role: String? = null,
    val content: String? = null,
    val reasoning_content: String? = null // New field for chain-of-thought tokens
)
