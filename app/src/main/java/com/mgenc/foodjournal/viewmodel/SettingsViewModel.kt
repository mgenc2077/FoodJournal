package com.mgenc.foodjournal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _lastSyncTime = MutableStateFlow<String?>(null)
    val lastSyncTime: StateFlow<String?> = _lastSyncTime

    private val _syncResult = MutableStateFlow<SyncResult?>(null)
    val syncResult: StateFlow<SyncResult?> = _syncResult

    fun setServerUrl(url: String) {
        _serverUrl.value = url
    }

    fun syncDatabase() {
        if (_isSyncing.value) return
        _isSyncing.value = true
        _syncResult.value = null
        viewModelScope.launch {
            delay(500)
            val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"))
            _lastSyncTime.value = now
            _syncResult.value = SyncResult.Success
            _isSyncing.value = false
        }
    }

    fun clearSyncResult() {
        _syncResult.value = null
    }

    sealed class SyncResult {
        data object Success : SyncResult()
        data class Error(val message: String) : SyncResult()
    }
}
