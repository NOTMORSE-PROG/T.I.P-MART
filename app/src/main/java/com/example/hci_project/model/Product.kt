package com.example.hci_project.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Product(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val category: String = "",
    val campus: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val sellerEmail: String = "",
    val imageUrls: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val status: String = "active" // active, sold, reserved
)
