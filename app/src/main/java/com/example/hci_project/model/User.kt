package com.example.hci_project.model

// Firestore requires a no-arg constructor and proper field annotations
data class User(
    val email: String = "",
    val fullname: String = "",
    val studentId: String = "",
    val userId: String = "",  // This is the Firebase Auth UID
    val documentId: String = ""  // This is the Firestore document ID
) {

    override fun toString(): String {
        return "User(email='$email', fullname='$fullname', studentId='$studentId', userId='$userId', documentId='$documentId')"
    }
}

