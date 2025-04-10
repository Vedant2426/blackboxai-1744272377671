package com.example.studentapp.data.auth

import android.content.Context
import com.example.studentapp.data.models.User
import com.example.studentapp.utils.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthManager(private val context: Context) {
    private val prefs = SecurityUtils.getEncryptedSharedPreferences(context)
    
    companion object {
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PASSWORD = "user_password"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    suspend fun register(user: User): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val hashedPassword = SecurityUtils.hashPassword(user.password)
            prefs.edit()
                .putString(KEY_USER_EMAIL, user.email)
                .putString(KEY_USER_PASSWORD, hashedPassword)
                .putString(KEY_USER_NAME, user.name)
                .apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val storedEmail = prefs.getString(KEY_USER_EMAIL, null)
            val storedPassword = prefs.getString(KEY_USER_PASSWORD, null)
            
            if (storedEmail == null || storedPassword == null) {
                return@withContext Result.failure(Exception("User not found"))
            }

            val hashedPassword = SecurityUtils.hashPassword(password)
            val isValid = storedEmail == email && storedPassword == hashedPassword
            
            if (isValid) {
                prefs.edit().putBoolean(KEY_IS_LOGGED_IN, true).apply()
            }
            
            Result.success(isValid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, false).apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getCurrentUser(): User? {
        val email = prefs.getString(KEY_USER_EMAIL, null)
        val name = prefs.getString(KEY_USER_NAME, "")
        return if (email != null) User(email, "", name ?: "") else null
    }
}
