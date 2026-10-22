package com.example.finanzaspersonales.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode(val title: String) {
    DARK("Modo Oscuro"),
    LIGHT("Modo Claro"),
    SYSTEM("Sistema")
}

object ThemeManager {
    private const val PREFS_NAME = "pardofinanzas_theme_prefs"
    private const val KEY_THEME_MODE = "key_theme_mode"

    var currentTheme by mutableStateOf(ThemeMode.DARK)
        private set

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedName = prefs?.getString(KEY_THEME_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
            currentTheme = try {
                ThemeMode.valueOf(savedName)
            } catch (_: Exception) {
                ThemeMode.DARK
            }
        }
    }

    fun setTheme(mode: ThemeMode) {
        currentTheme = mode
        prefs?.edit()?.putString(KEY_THEME_MODE, mode.name)?.apply()
    }

    fun toggleDarkLight() {
        val next = if (currentTheme == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
        setTheme(next)
    }
}
