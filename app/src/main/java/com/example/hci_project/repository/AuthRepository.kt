package com.example.hci_project.repository

import android.net.Uri
import android.util.Log
import com.example.hci_project.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor() {
    private val firebaseAuth: FirebaseAuth
    private val firestore: FirebaseFirestore
    private val storage: FirebaseStorage
    private val usersCollection: com.google.firebase.firestore.CollectionReference

    init {
        try {
            // Initialize Firebase components
            firebaseAuth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            storage = FirebaseStorage.getInstance()
            usersCollection = firestore.collection("users")
            Log.d("AuthRepository", "Repository initialized successfully")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error initializing Firebase: ${e.message}", e)
            throw e
        }
    }

    // Function to hash passwords
    private fun hashPassword(password: String): String {
        try {
            val bytes = password.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            return digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error hashing password: ${e.message}", e)
            // Return a placeholder if hashing fails
            return "HASH_ERROR"
        }
    }

    private suspend fun checkStudentIdExists(studentId: String): Boolean {
        return try {
            Log.d("AuthRepository", "Checking if student ID exists: $studentId")
            val query = usersCollection.whereEqualTo("studentId", studentId).get().await()
            val exists = !query.isEmpty
            Log.d("AuthRepository", "Student ID exists: $exists")
            exists
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error checking student ID: ${e.message}", e)
            false // Assume it doesn't exist if there's an error
        }
    }

    private suspend fun checkEmailExists(email: String): Boolean {
        return try {
            Log.d("AuthRepository", "Checking if email exists in Firestore: $email")
            val documentRef = usersCollection.document(email).get().await()
            val exists = documentRef.exists()
            Log.d("AuthRepository", "Email exists in Firestore: $exists")
            exists
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error checking email in Firestore: ${e.message}", e)
            false // Assume it doesn't exist if there's an error
        }
    }

    suspend fun signUp(email: String, password: String, fullname: String, studentId: String, campus: String): Result<FirebaseUser> {
        return try {
            // Trim inputs to remove any accidental whitespace
            val trimmedEmail = email.trim()
            val trimmedPassword = password.trim()

            Log.d("AuthRepository", "Attempting to sign up user: $trimmedEmail with fullname: $fullname, studentId: $studentId, campus: $campus")

            // Check if student ID already exists
            if (checkStudentIdExists(studentId)) {
                return Result.failure(Exception("Student ID already exists. Please use a different ID."))
            }

            // Check if email already exists in Firestore (as a document ID)
            if (checkEmailExists(trimmedEmail)) {
                return Result.failure(Exception("Email already exists in our database."))
            }

            val authResult = firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, trimmedPassword).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                Log.d("AuthRepository", "User created successfully: ${firebaseUser.uid}")

                // Hash the password before storing
                val hashedPassword = hashPassword(trimmedPassword)

                // Create user document in Firestore with required fields
                val user = hashMapOf(
                    "email" to trimmedEmail,
                    "fullname" to fullname,
                    "studentId" to studentId,
                    "campus" to campus,
                    "hashedPassword" to hashedPassword,  // Store hashed password
                    "userId" to firebaseUser.uid,
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "lastLogin" to com.google.firebase.Timestamp.now(),
                    "profilePictureUrl" to ""  // Initialize with empty profile picture URL
                )

                // Store in users collection with email as document ID
                try {
                    Log.d("AuthRepository", "Storing user data in Firestore with email as document ID: $trimmedEmail")
                    usersCollection.document(trimmedEmail).set(user).await()
                    Log.d("AuthRepository", "User data stored in Firestore successfully with email as document ID")

                    // Update the user's display name in Firebase Auth
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(fullname)
                        .build()
                    firebaseUser.updateProfile(profileUpdates).await()

                } catch (e: Exception) {
                    Log.e("AuthRepository", "Error storing user data in Firestore: ${e.message}", e)
                    // Continue even if Firestore fails - the user is still authenticated
                }

                Result.success(firebaseUser)
            } else {
                Log.e("AuthRepository", "User creation returned null user")
                Result.failure(Exception("User creation failed"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error in signUp: ${e.message}", e)
            // Handle Firebase specific error messages
            val errorMessage = when {
                e.message?.contains("email address is already in use") == true ->
                    "Email address is already in use"
                e.message?.contains("password is invalid") == true ->
                    "Password is invalid"
                else -> e.message ?: "Sign up failed"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            // Trim inputs to remove any accidental whitespace
            val trimmedEmail = email.trim()
            val trimmedPassword = password.trim()

            Log.d("AuthRepository", "Attempting to sign in user: $trimmedEmail")

            // Debug log to help diagnose issues
            Log.d("AuthRepository", "Password length: ${trimmedPassword.length}, contains spaces: ${trimmedPassword.contains(" ")}")

            try {
                // Try to authenticate with Firebase Auth
                val authResult = firebaseAuth.signInWithEmailAndPassword(trimmedEmail, trimmedPassword).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null) {
                    Log.d("AuthRepository", "User signed in successfully: ${firebaseUser.uid}")

                    // Check if user exists in Firestore
                    val userExists = checkEmailExists(trimmedEmail)
                    if (!userExists) {
                        Log.w("AuthRepository", "User authenticated but not found in Firestore. Creating minimal record.")
                        // You could create a minimal user record here if needed
                    }

                    // Update last login timestamp if the user exists in Firestore
                    try {
                        if (userExists) {
                            usersCollection.document(trimmedEmail).update(
                                "lastLogin", com.google.firebase.Timestamp.now()
                            ).await()
                            Log.d("AuthRepository", "Updated last login timestamp")
                        }
                    } catch (e: Exception) {
                        Log.e("AuthRepository", "Error updating last login: ${e.message}", e)
                        // Continue even if update fails
                    }

                    Result.success(firebaseUser)
                } else {
                    Log.e("AuthRepository", "Sign in returned null user")
                    Result.failure(Exception("Authentication failed"))
                }
            } catch (e: FirebaseAuthInvalidUserException) {
                // This exception is thrown when the user doesn't exist
                Log.e("AuthRepository", "User not found: ${e.message}")
                Result.failure(Exception("User not found"))
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                // This exception is thrown when the password is incorrect
                Log.e("AuthRepository", "Invalid credentials: ${e.message}")
                Result.failure(Exception("Incorrect password"))
            } catch (e: Exception) {
                // Handle other exceptions
                Log.e("AuthRepository", "Error during authentication: ${e.message}", e)

                // More detailed error handling
                val errorMessage = when {
                    e.message?.contains("no user record") == true ||
                            e.message?.contains("user may have been deleted") == true ->
                        "User not found"
                    e.message?.contains("password is invalid") == true ||
                            e.message?.contains("credential") == true ||
                            e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true ->
                        "Incorrect password"
                    else -> "Sign in failed: ${e.message}"
                }

                Log.e("AuthRepository", "Returning error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            // This catch block handles any exceptions outside the inner try-catch
            Log.e("AuthRepository", "Unexpected error in signIn: ${e.message}", e)
            Result.failure(Exception("Sign in failed: ${e.message}"))
        }
    }

    suspend fun getUserData(userId: String): Result<User> {
        return try {
            Log.d("AuthRepository", "Fetching user data for userId: $userId")

            // First, get the user's email from Firebase Auth
            val firebaseUser = firebaseAuth.currentUser
            val email = firebaseUser?.email?.trim()

            if (email != null) {
                // Try to get the document using the email as document ID
                val userDoc = usersCollection.document(email).get().await()

                if (userDoc.exists()) {
                    val userData = userDoc.data
                    Log.d("AuthRepository", "Raw user data from Firestore: $userData")

                    // Manually create the User object to avoid deserialization issues
                    val user = User(
                        email = userData?.get("email") as? String ?: "",
                        fullname = userData?.get("fullname") as? String ?: "",
                        studentId = userData?.get("studentId") as? String ?: "",
                        campus = userData?.get("campus") as? String ?: "",
                        hashedPassword = userData?.get("hashedPassword") as? String ?: "",
                        userId = userId,
                        documentId = email,  // Store the email as document ID
                        profilePictureUrl = userData?.get("profilePictureUrl") as? String ?: ""
                    )

                    Log.d("AuthRepository", "User data retrieved successfully: $user")
                    Result.success(user)
                } else {
                    Log.e("AuthRepository", "User document does not exist in Firestore for email: $email")

                    // Fallback: Query by userId field
                    val querySnapshot = usersCollection.whereEqualTo("userId", userId).get().await()

                    if (!querySnapshot.isEmpty) {
                        val doc = querySnapshot.documents[0]
                        val userData = doc.data

                        val user = User(
                            email = userData?.get("email") as? String ?: "",
                            fullname = userData?.get("fullname") as? String ?: "",
                            studentId = userData?.get("studentId") as? String ?: "",
                            campus = userData?.get("campus") as? String ?: "",
                            hashedPassword = userData?.get("hashedPassword") as? String ?: "",
                            userId = userId,
                            documentId = doc.id,
                            profilePictureUrl = userData?.get("profilePictureUrl") as? String ?: ""
                        )

                        Log.d("AuthRepository", "User data retrieved by userId query: $user")
                        Result.success(user)
                    } else {
                        // Create a minimal user with just the ID and email
                        val minimalUser = User(
                            userId = userId,
                            email = email,
                            documentId = email
                        )
                        Result.success(minimalUser)
                    }
                }
            } else {
                Log.e("AuthRepository", "No email found for current user")
                // Create a minimal user with just the ID
                val minimalUser = User(
                    userId = userId,
                    email = ""
                )
                Result.success(minimalUser)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error in getUserData: ${e.message}", e)
            // Create a minimal user with just the ID
            val minimalUser = User(
                userId = userId,
                email = firebaseAuth.currentUser?.email ?: ""
            )
            Result.success(minimalUser)
        }
    }

    // Updated function to upload profile picture with deletion of previous picture
    suspend fun uploadProfilePicture(imageUri: Uri, email: String): Result<String> {
        return try {
            val trimmedEmail = email.trim()
            Log.d("AuthRepository", "Uploading profile picture for user: $trimmedEmail")

            if (trimmedEmail.isEmpty()) {
                return Result.failure(Exception("Email is required to upload profile picture"))
            }

            // First, check if the user already has a profile picture
            val userDoc = usersCollection.document(trimmedEmail).get().await()
            val currentProfilePicUrl = userDoc.getString("profilePictureUrl") ?: ""

            // If there's an existing profile picture, delete it first
            if (currentProfilePicUrl.isNotEmpty()) {
                try {
                    // Extract the path from the URL to delete the file
                    val storageRef = storage.getReferenceFromUrl(currentProfilePicUrl)
                    storageRef.delete().await()
                    Log.d("AuthRepository", "Previous profile picture deleted: $currentProfilePicUrl")
                } catch (e: Exception) {
                    // If deletion fails, log the error but continue with the upload
                    Log.e("AuthRepository", "Error deleting previous profile picture: ${e.message}", e)
                }
            }

            // Create a reference to the user's profile picture in Firebase Storage
            // Using a consistent filename pattern to make it easier to manage
            val storageRef = storage.reference
                .child("profile_pictures")
                .child(trimmedEmail)
                .child("profile.jpg")

            // Upload the file and wait for completion
            storageRef.putFile(imageUri).await()

            // Get the download URL
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Log.d("AuthRepository", "Profile picture uploaded successfully. URL: $downloadUrl")

            // Update the user's profile picture URL in Firestore
            usersCollection.document(trimmedEmail).update("profilePictureUrl", downloadUrl).await()
            Log.d("AuthRepository", "Profile picture URL updated in Firestore")

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error uploading profile picture: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                val email = currentUser.email?.trim()
                val userId = currentUser.uid

                Log.d("AuthRepository", "Attempting to delete account for user: $email, $userId")

                // First, ensure we delete all user documents from Firestore
                var firestoreDeleteSuccess = false

                // Try to delete by email first (our primary document ID)
                if (email != null) {
                    try {
                        usersCollection.document(email).delete().await()
                        Log.d("AuthRepository", "User document deleted from Firestore by email")
                        firestoreDeleteSuccess = true
                    } catch (e: Exception) {
                        Log.e("AuthRepository", "Error deleting user document by email: ${e.message}", e)
                        // We'll try the userId method next
                    }
                }

                // If email deletion failed or email was null, try finding by userId
                if (!firestoreDeleteSuccess) {
                    try {
                        val querySnapshot = usersCollection.whereEqualTo("userId", userId).get().await()
                        if (!querySnapshot.isEmpty) {
                            for (doc in querySnapshot.documents) {
                                doc.reference.delete().await()
                                Log.d("AuthRepository", "User document deleted from Firestore by userId: ${doc.id}")
                            }
                            firestoreDeleteSuccess = true
                        }
                    } catch (e: Exception) {
                        Log.e("AuthRepository", "Error finding/deleting user document by userId: ${e.message}", e)
                    }
                }

                // If we still haven't found the document, try one last query with all user fields
                if (!firestoreDeleteSuccess) {
                    try {
                        // Try to find any documents that might be related to this user
                        val queries = listOf(
                            usersCollection.whereEqualTo("email", email ?: "").get(),
                            usersCollection.whereEqualTo("userId", userId).get()
                        )

                        queries.forEach { queryTask ->
                            val querySnapshot = queryTask.await()
                            if (!querySnapshot.isEmpty) {
                                for (doc in querySnapshot.documents) {
                                    doc.reference.delete().await()
                                    Log.d("AuthRepository", "User document deleted from Firestore by additional query: ${doc.id}")
                                }
                                firestoreDeleteSuccess = true
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("AuthRepository", "Error in additional document deletion queries: ${e.message}", e)
                    }
                }

                if (!firestoreDeleteSuccess) {
                    Log.w("AuthRepository", "Could not find and delete user documents in Firestore")
                }

                // Delete profile pictures from Storage if they exist
                if (email != null) {
                    try {
                        // Delete the entire profile_pictures folder for this user
                        val storageRef = storage.reference.child("profile_pictures").child(email)

                        // First delete all files in the folder
                        val listResult = storageRef.listAll().await()

                        // Delete all items (files) in the folder
                        listResult.items.forEach { item ->
                            item.delete().await()
                            Log.d("AuthRepository", "Deleted profile picture: ${item.path}")
                        }

                        // Delete all prefixes (subfolders) recursively
                        listResult.prefixes.forEach { prefix ->
                            // For each prefix, list and delete its contents
                            val subListResult = prefix.listAll().await()
                            subListResult.items.forEach { item ->
                                item.delete().await()
                                Log.d("AuthRepository", "Deleted nested file: ${item.path}")
                            }
                        }

                        // The folder itself is automatically removed when empty
                        Log.d("AuthRepository", "Successfully cleaned up user storage data")
                    } catch (e: Exception) {
                        Log.e("AuthRepository", "Error deleting profile pictures: ${e.message}", e)
                        // Continue even if deletion fails
                    }
                }

                // Finally delete the Firebase Auth account
                currentUser.delete().await()
                Log.d("AuthRepository", "User account deleted from Firebase Auth")

                Result.success(Unit)
            } else {
                Log.e("AuthRepository", "No current user to delete")
                Result.failure(Exception("No user is currently signed in"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error in deleteAccount: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            Log.d("AuthRepository", "Signing out user")
            firebaseAuth.signOut()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error in signOut: ${e.message}", e)
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return try {
            val user = firebaseAuth.currentUser
            if (user != null) {
                Log.d("AuthRepository", "Current user: ${user.uid}")
            } else {
                Log.d("AuthRepository", "No current user")
            }
            user
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error in getCurrentUser: ${e.message}", e)
            null
        }
    }
}