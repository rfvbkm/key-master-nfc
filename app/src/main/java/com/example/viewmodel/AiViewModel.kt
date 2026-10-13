package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiResult
import com.example.ai.AiSettingsManager
import com.example.ai.GeminiApiService
import com.example.model.CardDump
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AiUiState {
    object Idle : AiUiState()
    data class Thinking(val query: String) : AiUiState()
    data class Success(val markdown: String) : AiUiState()
    data class Error(val message: String) : AiUiState()
}

class AiViewModel(application: Application) : AndroidViewModel(application) {

    val settingsManager = AiSettingsManager(application)
    private val apiService = GeminiApiService(application)

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private val _currentModelName = MutableStateFlow(settingsManager.selectedModel)
    val currentModelName: StateFlow<String> = _currentModelName.asStateFlow()

    fun updateApiKey(newKey: String) {
        settingsManager.customApiKey = newKey
    }

    fun updateSelectedModel(model: String) {
        settingsManager.selectedModel = model
        _currentModelName.value = model
    }

    fun updateAutoFallback(enabled: Boolean) {
        settingsManager.autoFallbackOn429 = enabled
    }

    fun analyzeCard(dump: CardDump, customQuery: String? = null) {
        viewModelScope.launch {
            _uiState.value = AiUiState.Thinking(customQuery ?: "Глубокий анализ протоколов и безопасности...")
            when (val result = apiService.analyzeCardDump(dump, customQuery)) {
                is AiResult.Success -> {
                    _uiState.value = AiUiState.Success(result.analysisMarkdown)
                }
                is AiResult.Error -> {
                    _uiState.value = AiUiState.Error(result.message)
                }
            }
        }
    }

    fun reset() {
        _uiState.value = AiUiState.Idle
    }
}
