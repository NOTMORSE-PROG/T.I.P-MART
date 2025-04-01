package com.example.hci_project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.example.hci_project.ui.theme.HCI_PROJECYTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HCI_PROJECYTheme {
                // Use states to control which screen to show
                val isLoggedIn = remember { mutableStateOf(false) }
                val showSignUp = remember { mutableStateOf(false) }

                when {
                    isLoggedIn.value -> {
                        // Show the dashboard when logged in
                        DashboardScreen(
                            onLogout = { isLoggedIn.value = false }
                        )
                    }
                    showSignUp.value -> {
                        // Show the sign up screen
                        SignUpScreen(
                            onSignUpSuccess = {
                                isLoggedIn.value = true
                                showSignUp.value = false
                            },
                            onBackToLogin = { showSignUp.value = false }
                        )
                    }
                    else -> {
                        // Show the login screen by default
                        LoginScreen(
                            onLoginSuccess = {
                                isLoggedIn.value = true
                            },
                            onSignUpClick = { showSignUp.value = true }
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
            onLogout = {}
        )
    }
}

