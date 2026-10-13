package com.example.ai

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

class AiSettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ai_settings_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"
        const val KEY_SELECTED_MODEL = "selected_gemini_model"
        const val KEY_AUTO_FALLBACK = "auto_fallback_on_429"

        const val MODEL_PRO_THINKING = "gemini-3.1-pro-preview"
        const val MODEL_FLASH = "gemini-3.5-flash"
    }

    var customApiKey: String
        get() = prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_API_KEY, value.trim()).apply()

    var selectedModel: String
        get() = prefs.getString(KEY_SELECTED_MODEL, MODEL_PRO_THINKING) ?: MODEL_PRO_THINKING
        set(value) = prefs.edit().putString(KEY_SELECTED_MODEL, value).apply()

    var autoFallbackOn429: Boolean
        get() = prefs.getBoolean(KEY_AUTO_FALLBACK, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_FALLBACK, value).apply()

    fun getEffectiveApiKey(): String {
        val custom = customApiKey
        if (custom.isNotBlank()) return custom
        val buildKey = BuildConfig.GEMINI_API_KEY
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") return buildKey
        return ""
    }

    fun isUsingCustomKey(): Boolean {
        return customApiKey.isNotBlank()
    }
}
