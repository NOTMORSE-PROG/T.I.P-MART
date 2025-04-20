package com.example.hci_project.model

import com.google.firebase.Timestamp

data class Rating(
    val id: String = "",
    val productId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val userProfilePicture: String = "" // Added field for user profile picture
) {
    override fun toString(): String {
        return "Rating(id='$id', productId='$productId', userId='$userId', userName='$userName', rating=$rating, comment='$comment', createdAt=$createdAt, userProfilePicture='$userProfilePicture')"
    }
}
