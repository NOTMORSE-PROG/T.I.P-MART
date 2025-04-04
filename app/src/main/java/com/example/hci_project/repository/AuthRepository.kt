package com.example.hci_project.repository

import android.util.Log
import com.example.hci_project.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor() {
    private val firebaseAuth: FirebaseAuth
    private val firestore: FirebaseFirestore
    private val usersCollection: com.google.firebase.firestore.CollectionReference

    init {
        try {
            // Initialize Firebase components
            firebaseAuth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            usersCollection = firestore.collection("users")
            Log.d("AuthRepository", "Repository initialized successfully")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error initializing Firebase: ${e.message}", e)
            throw e
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

    suspend fun signUp(email: String, password: String, fullname: String, studentId: String): Result<FirebaseUser> {
        return try {
            Log.d("AuthRepository", "Attempting to sign up user: $email with fullname: $fullname, studentId: $studentId")

            // Check if student ID already exists
            if (checkStudentIdExists(studentId)) {
                return Result.failure(Exception("Student ID already exists. Please use a different ID."))
            }

            // Check if email already exists in Firestore (as a document ID)
            if (checkEmailExists(email)) {
                return Result.failure(Exception("Email already exists in our database."))
            }

            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                Log.d("AuthRepository", "User created successfully: ${firebaseUser.uid}")
                // Create user document in Firestore with required fields
                val user = hashMapOf(
                    "email" to email,
                    "fullname" to fullname,
                    "studentId" to studentId,
                    "userId" to firebaseUser.uid,
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "lastLogin" to com.google.firebase.Timestamp.now()
                )

                // Store in users collection with email as document ID
                try {
                    Log.d("AuthRepository", "Storing user data in Firestore with email as document ID: $email")
                    usersCollection.document(email).set(user).await()
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
            Log.d("AuthRepository", "Attempting to sign in user: $email")
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                Log.d("AuthRepository", "User signed in successfully: ${firebaseUser.uid}")

                // Update last login timestamp
                try {
                    usersCollection.document(email).update(
                        "lastLogin", com.google.firebase.Timestamp.now()
                    ).await()
                    Log.d("AuthRepository", "Updated last login timestamp")
                } catch (e: Exception) {
                    Log.e("AuthRepository", "Error updating last login: ${e.message}", e)
                    // Continue even if update fails
                }

                Result.success(firebaseUser)
            } else {
                Log.e("AuthRepository", "Sign in returned null user")
                Result.failure(Exception("Authentication failed"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error in signIn: ${e.message}", e)
            // Handle Firebase specific error messages
            val errorMessage = when {
                e.message?.contains("no user record") == true ->
                    "No account found with this email"
                e.message?.contains("password is invalid") == true ->
                    "Incorrect password"
                else -> e.message ?: "Sign in failed"
            }
            Result.failure(Exception(errorMessage))
        }
    }

    suspend fun getUserData(userId: String): Result<User> {
        return try {
            Log.d("AuthRepository", "Fetching user data for userId: $userId")

            // First, get the user's email from Firebase Auth
            val firebaseUser = firebaseAuth.currentUser
            val email = firebaseUser?.email

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
                        userId = userId,
                        documentId = email  // Store the email as document ID
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
                            userId = userId,
                            documentId = doc.id
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

