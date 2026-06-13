package com.example.core

data class ChatMessage(
    val sender: String,
    val role: String,
    val text: String,
    val timestamp: String,
    val isMe: Boolean = false,
    val avatarColor: Long,
    val id: String = java.util.UUID.randomUUID().toString()
)
