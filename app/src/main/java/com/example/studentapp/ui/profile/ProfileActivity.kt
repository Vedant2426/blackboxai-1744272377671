package com.example.studentapp.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.studentapp.data.auth.AuthManager
import com.example.studentapp.data.models.User
import com.example.studentapp.databinding.ActivityProfileBinding
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var authManager: AuthManager
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = AuthManager(this)
        setupToolbar()
        loadUserProfile()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadUserProfile() {
        currentUser = authManager.getCurrentUser()
        currentUser?.let { user ->
            binding.nameInput.setText(user.name)
            binding.emailInput.setText(user.email)
        }
    }

    private fun setupClickListeners() {
        binding.updateProfileButton.setOnClickListener {
            val newName = binding.nameInput.text.toString().trim()
            if (validateInput(newName)) {
                updateProfile(newName)
            }
        }
    }

    private fun validateInput(name: String): Boolean {
        if (name.isEmpty()) {
            binding.nameLayout.error = "Name is required"
            return false
        }
        binding.nameLayout.error = null
        return true
    }

    private fun updateProfile(newName: String) {
        showLoading(true)
        
        // Since we're using SharedPreferences, we'll need to update the stored user data
        currentUser?.let { user ->
            val updatedUser = User(user.email, user.password, newName)
            
            lifecycleScope.launch {
                try {
                    val result = authManager.register(updatedUser) // Re-register to update the data
                    result.fold(
                        onSuccess = {
                            showSuccess("Profile updated successfully")
                        },
                        onFailure = { exception ->
                            showError(exception.message ?: "Failed to update profile")
                        }
                    )
                } catch (e: Exception) {
                    showError("An error occurred while updating profile")
                } finally {
                    showLoading(false)
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.updateProfileButton.isEnabled = !show
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
