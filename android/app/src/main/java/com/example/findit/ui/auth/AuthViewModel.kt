package com.example.findit.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.findit.api.RetrofitClient
import com.example.findit.model.LoginRequest
import com.example.findit.model.RegisterRequest
import com.example.findit.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitClient.api.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    response.body()?.let {
                        tokenManager.saveToken(it.token)
                        _authState.value = AuthState.Success
                    }
                } else {
                    _authState.value = AuthState.Error("Invalid email or password")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Cannot connect to server: ${e.message}")
            }
        }
    }

    fun register(name: String, email: String, password: String, collegeId: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitClient.api.register(
                    RegisterRequest(name, email, password, collegeId)
                )
                if (response.isSuccessful) {
                    // Auto login after register
                    login(email, password)
                } else {
                    val errorBody = response.errorBody()?.string()
                    _authState.value = AuthState.Error("Registration failed: $errorBody")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Cannot connect to server: ${e.message}")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}