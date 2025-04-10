package com.example.studentapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.studentapp.data.auth.AuthManager
import com.example.studentapp.databinding.ActivityLoginBinding
import com.example.studentapp.ui.MainActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        authManager = AuthManager(this)
        
        // Check if user is already logged in
        if (authManager.isLoggedIn()) {
            startMainActivity()
            return
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()

            if (validateInput(email, password)) {
                performLogin(email, password)
            }
        }

        binding.registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        binding.emailLayout.error = null
        binding.passwordLayout.error = null

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
        return true
    }

    private fun performLogin(email: String, password: String) {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val result = authManager.login(email, password)
                result.fold(
                    onSuccess = { isValid ->
                        if (isValid) {
                            startMainActivity()
                        } else {
                            showError("Invalid email or password")
                        }
                    },
                    onFailure = { exception ->
                        showError(exception.message ?: "Login failed")
                    }
                )
            } catch (e: Exception) {
                showError("An error occurred")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !show
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
