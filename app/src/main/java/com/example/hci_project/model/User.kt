package com.example.hci_project.model

// Firestore requires a no-arg constructor and proper field annotations
data class User(
    val email: String = "",
    val fullname: String = "",
    val studentId: String = "",
    val campus: String = "",
    val hashedPassword: String = "",  // Added hashed password field
    val userId: String = "",  // This is the Firebase Auth UID
    val documentId: String = "",  // This is the Firestore document ID
    val profilePictureUrl: String = ""  // Added profile picture URL field
) {

    override fun toString(): String {
        return "User(email='$email', fullname='$fullname', studentId='$studentId', campus='$campus', userId='$userId', documentId='$documentId', profilePictureUrl='$profilePictureUrl')"
    }
}

