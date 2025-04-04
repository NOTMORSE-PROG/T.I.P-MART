package com.example.hci_project.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hci_project.model.User
import com.example.hci_project.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    init {
        Log.d("AuthViewModel", "Initializing AuthViewModel")
        // Check if user is already logged in
        try {
            val firebaseUser = authRepository.getCurrentUser()
            if (firebaseUser != null) {
                Log.d("AuthViewModel", "User already logged in: ${firebaseUser.uid}")
                _authState.value = AuthState.LoggedIn
                fetchUserData(firebaseUser.uid)
            } else {
                Log.d("AuthViewModel", "No user logged in")
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error checking current user: ${e.message}", e)
        }
    }

    fun signUp(email: String, password: String, fullname: String, studentId: String) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Attempting to sign up: $email with fullname: $fullname, studentId: $studentId")
            _authState.value = AuthState.Loading

            try {
                val result = authRepository.signUp(email, password, fullname, studentId)

                result.fold(
                    onSuccess = {
                        Log.d("AuthViewModel", "Sign up successful: ${it.uid}")

                        // Create a user object directly to ensure we have the data
                        _currentUser.value = User(
                            email = email,
                            fullname = fullname,
                            studentId = studentId,
                            userId = it.uid
                        )

                        _authState.value = AuthState.LoggedIn
                    },
                    onFailure = {
                        Log.e("AuthViewModel", "Sign up failed: ${it.message}")
                        _authState.value = AuthState.Error(it.message ?: "Sign up failed")
                    }
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Exception during sign up: ${e.message}", e)
                _authState.value = AuthState.Error(e.message ?: "Sign up failed")
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Attempting to sign in: $email")
            _authState.value = AuthState.Loading

            try {
                val result = authRepository.signIn(email, password)

                result.fold(
                    onSuccess = {
                        Log.d("AuthViewModel", "Sign in successful: ${it.uid}")
                        _authState.value = AuthState.LoggedIn
                        fetchUserData(it.uid)
                    },
                    onFailure = {
                        Log.e("AuthViewModel", "Sign in failed: ${it.message}")
                        _authState.value = AuthState.Error(it.message ?: "Sign in failed")
                    }
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Exception during sign in: ${e.message}", e)
                _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            }
        }
    }

    private fun fetchUserData(userId: String) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Fetching user data for: $userId")
            try {
                val result = authRepository.getUserData(userId)

                result.fold(
                    onSuccess = { user ->
                        Log.d("AuthViewModel", "User data fetched successfully: $user")
                        _currentUser.value = user
                    },
                    onFailure = {
                        Log.e("AuthViewModel", "Failed to fetch user data: ${it.message}")
                        // Create a minimal user with just the ID if Firestore data isn't found
                        _currentUser.value = User(
                            userId = userId,
                            email = authRepository.getCurrentUser()?.email ?: ""
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Exception fetching user data: ${e.message}", e)
                // Create a minimal user with just the ID if there's an error
                _currentUser.value = User(
                    userId = userId,
                    email = authRepository.getCurrentUser()?.email ?: ""
                )
            }
        }
    }

    fun signOut() {
        Log.d("AuthViewModel", "Signing out")
        authRepository.signOut()
        _authState.value = AuthState.Idle
        _currentUser.value = null
    }

    fun resetAuthState() {
        Log.d("AuthViewModel", "Resetting auth state")
        _authState.value = AuthState.Idle
    }

    sealed class AuthState {
        data object Idle : AuthState()
        data object Loading : AuthState()
        data object LoggedIn : AuthState()
        data class Error(val message: String) : AuthState()
    }
}

