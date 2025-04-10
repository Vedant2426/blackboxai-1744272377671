package com.example.studentapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studentapp.R
import com.example.studentapp.data.auth.AuthManager
import com.example.studentapp.databinding.ActivityMainBinding
import com.example.studentapp.ui.auth.LoginActivity
import com.example.studentapp.ui.files.FileStorageActivity
import com.example.studentapp.ui.profile.ProfileActivity
import com.example.studentapp.ui.qrscan.QrScanActivity

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = AuthManager(this)
        
        setupToolbar()
        setupClickListeners()
        updateUserInfo()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.action_logout -> {
                    performLogout()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupClickListeners() {
        binding.openFilesButton.setOnClickListener {
            startActivity(Intent(this, FileStorageActivity::class.java))
        }

        binding.openScannerButton.setOnClickListener {
            startActivity(Intent(this, QrScanActivity::class.java))
        }

        binding.openRemindersButton.setOnClickListener {
            // TODO: Implement Reminders Activity
            showFeatureNotImplemented()
        }
    }

    private fun updateUserInfo() {
        val currentUser = authManager.getCurrentUser()
        currentUser?.let { user ->
            supportActionBar?.subtitle = "Welcome, ${user.name}"
        }
    }

    private fun performLogout() {
        authManager.logout()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun showFeatureNotImplemented() {
        Toast.makeText(this, "This feature will be available soon!", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        // Check if user is still logged in
        if (!authManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }
}
