package com.example.hci_project

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TipMartApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        try {
            if (FirebaseApp.getApps(this).isNotEmpty()) {
                Log.d("TipMartApplication", "Firebase already initialized")
            } else {
                val result = FirebaseApp.initializeApp(this)
                if (result != null) {
                    Log.d("TipMartApplication", "Firebase initialized successfully")

                    // Test Firebase services
                    try {
                        FirebaseAuth.getInstance()
                        Log.d("TipMartApplication", "Firebase Auth initialized")

                        FirebaseFirestore.getInstance()
                        Log.d("TipMartApplication", "Firestore initialized")
                    } catch (e: Exception) {
                        Log.e("TipMartApplication", "Error testing Firebase services: ${e.message}")
                    }
                } else {
                    Log.e("TipMartApplication", "Firebase initialization returned null")
                }
            }
        } catch (e: Exception) {
            Log.e("TipMartApplication", "Error initializing Firebase: ${e.message}", e)
        }
    }
}

