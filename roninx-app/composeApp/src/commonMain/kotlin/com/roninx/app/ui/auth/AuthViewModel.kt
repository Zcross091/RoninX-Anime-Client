package com.roninx.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roninx.shared.network.Supabase
import io.github.jan_tennert.supabase.gotrue.auth
import io.github.jan_tennert.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val _user = MutableStateFlow<String?>(null)
    val user: StateFlow<String?> = _user

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        checkSession()
    }

    private fun checkSession() {
        _user.value = Supabase.client.auth.currentSessionOrNull()?.user?.email
    }

    fun signIn(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                Supabase.client.auth.signInWith(Email) {
                    this.email = email
                    password = pass
                }
                checkSession()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signUp(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                Supabase.client.auth.signUpWith(Email) {
                    this.email = email
                    password = pass
                }
                _error.value = "Check your email for confirmation"
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            Supabase.client.auth.signOut()
            _user.value = null
        }
    }
}
