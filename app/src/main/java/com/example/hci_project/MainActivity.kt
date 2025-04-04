package com.example.hci_project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hci_project.ui.theme.HCI_PROJECYTheme
import com.example.hci_project.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HCI_PROJECYTheme {
                val viewModel: AuthViewModel = hiltViewModel()
                val authState by viewModel.authState.collectAsState()

                // Use states to control which screen to show
                val showSignUp = remember { mutableStateOf(false) }

                when {
                    authState is AuthViewModel.AuthState.LoggedIn -> {
                        // Show the dashboard when logged in
                        DashboardScreen(
                            onLogout = {
                                viewModel.signOut()
                            },
                            viewModel = viewModel
                        )
                    }
                    showSignUp.value -> {
                        // Show the sign up screen
                        SignUpScreen(
                            onBackToLogin = {
                                showSignUp.value = false
                                viewModel.resetAuthState()
                            }
                        )
                    }
                    else -> {
                        // Show the login screen by default
                        LoginScreen(
                            onLoginSuccess = {
                                // The viewModel will handle the state change
                            },
                            onSignUpClick = {
                                showSignUp.value = true
                                viewModel.resetAuthState()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    HCI_PROJECYTheme {
        DashboardScreen(
            onLogout = {},
            viewModel = hiltViewModel()
        )
    }
}

