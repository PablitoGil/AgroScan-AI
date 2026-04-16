package com.example.agroscanai

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AgroScanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            Firebase.firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                        .build()
                )
                .build()
        } catch (_: Exception) {
        }
    }
}
