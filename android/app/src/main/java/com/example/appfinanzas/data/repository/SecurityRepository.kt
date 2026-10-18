package com.example.appfinanzas.data.repository

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.appfinanzas.data.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val IS_SECURITY_ENABLED = booleanPreferencesKey("is_security_enabled")
        val SECURITY_PIN = stringPreferencesKey("security_pin")
        val IS_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")
    }

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    val pin: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.SECURITY_PIN] ?: ""
        }

    val isSecurityEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val enabled = preferences[PreferencesKeys.IS_SECURITY_ENABLED] ?: false
            val currentPin = preferences[PreferencesKeys.SECURITY_PIN] ?: ""
            enabled && currentPin.isNotBlank()
        }

    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.IS_BIOMETRIC_ENABLED] ?: false
        }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            combine(isSecurityEnabled, pin) { enabled, currentPin ->
                enabled && currentPin.isNotBlank()
            }.collect { canLock ->
                if (!canLock) {
                    _isAppLocked.value = false
                }
            }
        }
    }

    suspend fun setSecurityEnabled(enabled: Boolean) {
        val currentPin = pin.first()
        val canEnable = enabled && currentPin.isNotBlank()
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_SECURITY_ENABLED] = canEnable
        }
        if (!canEnable) {
            _isAppLocked.value = false
        }
    }

    suspend fun setPin(newPin: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SECURITY_PIN] = newPin
            if (newPin.isBlank()) {
                preferences[PreferencesKeys.IS_SECURITY_ENABLED] = false
            }
        }
        if (newPin.isBlank()) {
            _isAppLocked.value = false
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun validatePin(inputPin: String): Boolean {
        val currentPin = pin.first()
        if (currentPin.isBlank()) {
            _isAppLocked.value = false
            return true
        }
        return inputPin == currentPin
    }

    fun lockApp() {
        CoroutineScope(Dispatchers.IO).launch {
            val currentPin = pin.first()
            val enabled = isSecurityEnabled.first()
            _isAppLocked.value = enabled && currentPin.isNotBlank()
        }
    }

    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun isBiometricSupported(): Boolean {
        return try {
            val biometricManager = BiometricManager.from(context)
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
        } catch (_: Exception) {
            false
        }
    }
}
