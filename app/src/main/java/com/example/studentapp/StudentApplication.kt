package com.example.studentapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.studentapp.data.auth.AuthManager

class StudentApplication : Application() {
    
    companion object {
        const val CHANNEL_ID = "student_app_channel"
        private var instance: StudentApplication? = null
        
        fun getInstance(): StudentApplication {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
        
        lateinit var authManager: AuthManager
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize AuthManager
        authManager = AuthManager(this)
        
        // Create notification channel for Android O and above
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            val descriptionText = "Notifications for reminders and updates"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        instance = null
    }
}
