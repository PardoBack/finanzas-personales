package com.example.appfinanzas.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

open class UserPreferencesRepository(private val context: Context?) {

    constructor() : this(null)

    private object PreferencesKeys {
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_CURRENCY = stringPreferencesKey("user_currency")
        val AVERAGE_SALARY = doublePreferencesKey("average_salary")
        val GOOGLE_WEB_APP_URL = stringPreferencesKey("google_web_app_url")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val DISMISSED_NOTIFICATIONS = stringSetPreferencesKey("dismissed_notifications")
    }

    open val isFirstLaunch: Flow<Boolean> get() = context?.dataStore?.data
        ?.catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        ?.map { preferences ->
            preferences[PreferencesKeys.IS_FIRST_LAUNCH] ?: true
        } ?: flowOf(false)

    open suspend fun setFirstLaunchCompleted() {
        context?.dataStore?.edit { preferences ->
            preferences[PreferencesKeys.IS_FIRST_LAUNCH] = false
        }
    }

    open val userName: Flow<String> get() = context?.dataStore?.data
        ?.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        ?.map { preferences -> preferences[PreferencesKeys.USER_NAME] ?: "Marlon" }
        ?: flowOf("Marlon")

    open suspend fun setUserName(name: String) {
        context?.dataStore?.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name.trim()
        }
    }

    open val userCurrency: Flow<String> get() = context?.dataStore?.data
        ?.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        ?.map { preferences -> preferences[PreferencesKeys.USER_CURRENCY] ?: "COP" }
        ?: flowOf("COP")

    open suspend fun setUserCurrency(currency: String) {
        context?.dataStore?.edit { preferences ->
            preferences[PreferencesKeys.USER_CURRENCY] = currency.trim()
        }
    }

    open val averageSalary: Flow<Double> get() = context?.dataStore?.data
        ?.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        ?.map { preferences -> preferences[PreferencesKeys.AVERAGE_SALARY] ?: 2500000.0 }
        ?: flowOf(2500000.0)

    open suspend fun setAverageSalary(salary: Double) {
        context?.dataStore?.edit { preferences ->
            preferences[PreferencesKeys.AVERAGE_SALARY] = salary
        }
    }

    open val googleWebAppUrl: Flow<String> get() = context?.dataStore?.data
        ?.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        ?.map { preferences -> preferences[PreferencesKeys.GOOGLE_WEB_APP_URL] ?: "https://script.google.com/macros/s/AKfycbxfGcLK2UtQUpLYTOmD6o7PO-cCD_MmUnGul0F7MlfLbPebKYNy8HLV8XM3amI8a_BQ/exec" }
        ?: flowOf("https://script.google.com/macros/s/AKfycbxfGcLK2UtQUpLYTOmD6o7PO-cCD_MmUnGul0F7MlfLbPebKYNy8HLV8XM3amI8a_BQ/exec")

    open suspend fun setGoogleWebAppUrl(url: String) {
        context?.dataStore?.edit { preferences ->
            preferences[PreferencesKeys.GOOGLE_WEB_APP_URL] = url.trim()
        }
    }

    open val geminiApiKey: Flow<String> get() = context?.dataStore?.data
        ?.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        ?.map { preferences -> preferences[PreferencesKeys.GEMINI_API_KEY] ?: "" }
        ?: flowOf("https://script.google.com/macros/s/AKfycbxfGcLK2UtQUpLYTOmD6o7PO-cCD_MmUnGul0F7MlfLbPebKYNy8HLV8XM3amI8a_BQ/exec")

    open suspend fun setGeminiApiKey(apiKey: String) {
        context?.dataStore?.edit { preferences ->
            preferences[PreferencesKeys.GEMINI_API_KEY] = apiKey.trim()
        }
    }

    open val dismissedNotifications: Flow<Set<String>> get() = context?.dataStore?.data
        ?.catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        ?.map { preferences -> preferences[PreferencesKeys.DISMISSED_NOTIFICATIONS] ?: emptySet() }
        ?: flowOf(emptySet())

    open suspend fun dismissNotification(notificationId: String) {
        context?.dataStore?.edit { preferences ->
            val current = preferences[PreferencesKeys.DISMISSED_NOTIFICATIONS] ?: emptySet()
            preferences[PreferencesKeys.DISMISSED_NOTIFICATIONS] = current + notificationId
        }
    }

    open suspend fun clearAllDismissedNotifications() {
        context?.dataStore?.edit { preferences ->
            preferences[PreferencesKeys.DISMISSED_NOTIFICATIONS] = emptySet()
        }
    }

    open val budgetLimits: Flow<Map<String, Double>> get() = context?.dataStore?.data
        ?.catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        ?.map { preferences ->
            val defaultLimits = mapOf(
                "Alimentación" to 0.0,
                "Transporte" to 0.0,
                "Servicios" to 0.0,
                "Ocio" to 0.0,
                "Hogar" to 0.0,
                "Salud" to 0.0
            )

            defaultLimits.mapValues { (category, defaultVal) ->
                val key = doublePreferencesKey("budget_limit_$category")
                preferences[key] ?: defaultVal
            }
        } ?: flowOf(emptyMap())

    open suspend fun setBudgetLimit(category: String, limit: Double) {
        context?.dataStore?.edit { preferences ->
            val key = doublePreferencesKey("budget_limit_$category")
            preferences[key] = limit
        }
    }
}
