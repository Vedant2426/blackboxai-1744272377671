package com.example.studentapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.studentapp.data.auth.AuthManager
import com.example.studentapp.data.models.User
import com.example.studentapp.databinding.ActivityRegisterBinding
import com.example.studentapp.ui.MainActivity
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        authManager = AuthManager(this)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.registerButton.setOnClickListener {
            val name = binding.nameInput.text.toString()
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()
            val confirmPassword = binding.confirmPasswordInput.text.toString()

            if (validateInput(name, email, password, confirmPassword)) {
                performRegistration(name, email, password)
            }
        }

        binding.loginLink.setOnClickListener {
            finish() // Return to login activity
        }
    }

    private fun validateInput(name: String, email: String, password: String, confirmPassword: String): Boolean {
        // Reset errors
        binding.nameLayout.error = null
        binding.emailLayout.error = null
        binding.passwordLayout.error = null
        binding.confirmPasswordLayout.error = null

        if (name.isEmpty()) {
            binding.nameLayout.error = "Name is required"
            return false
        }
        
        if (email.isEmpty()) {
            binding.emailLayout.error = "Email is required"
            return false
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = "Invalid email format"
            return false
        }
        
        if (password.isEmpty()) {
            binding.passwordLayout.error = "Password is required"
            return false
        }
        
        if (password.length < 6) {
            binding.passwordLayout.error = "Password must be at least 6 characters"
            return false
        }
        
        if (password != confirmPassword) {
            binding.confirmPasswordLayout.error = "Passwords do not match"
            return false
        }

        return true
    }

    private fun performRegistration(name: String, email: String, password: String) {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val user = User(email, password, name)
                val result = authManager.register(user)
                
                result.fold(
                    onSuccess = {
                        // Auto login after successful registration
                        val loginResult = authManager.login(email, password)
                        loginResult.fold(
                            onSuccess = { isValid ->
                                if (isValid) {
                                    startMainActivity()
                                } else {
                                    showError("Registration successful but login failed")
                                }
                            },
                            onFailure = { exception ->
                                showError("Registration successful but login failed: ${exception.message}")
                            }
                        )
                    },
                    onFailure = { exception ->
                        showError(exception.message ?: "Registration failed")
                    }
                )
            } catch (e: Exception) {
                showError("An error occurred during registration")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.registerButton.isEnabled = !show
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
