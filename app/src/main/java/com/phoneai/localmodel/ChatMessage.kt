package com.phoneai.localmodel

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    var text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    var isGenerating: Boolean = false,
    var statsInfo: String? = null
)
