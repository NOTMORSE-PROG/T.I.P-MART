package com.example.hci_project.model

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val read: Boolean = false,
    val senderProfilePicture: String = ""
)

data class Conversation(
    val id: String = "",
    val participants: List<String> = listOf(), // List of user IDs
    val participantNames: Map<String, String> = mapOf(), // Map of user IDs to names
    val participantProfiles: Map<String, String> = mapOf(), // Map of user IDs to profile pictures
    val lastMessage: String = "",
    val lastMessageTimestamp: Timestamp = Timestamp.now(),
    val lastMessageSenderId: String = "",
    val unreadCount: Map<String, Int> = mapOf(), // Map of user IDs to unread counts
    val productId: String = "", // The product this conversation is about
    val productTitle: String = "", // Title of the product
    val productImage: String = "" // First image of the product
)
