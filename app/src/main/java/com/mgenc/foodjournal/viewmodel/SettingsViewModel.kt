package com.mgenc.foodjournal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mgenc.foodjournal.FoodJournalApp
import com.mgenc.foodjournal.data.ErrorResponse
import com.mgenc.foodjournal.data.RebuildResponse
import com.mgenc.foodjournal.data.SyncRequest
import com.mgenc.foodjournal.data.SyncResponse
import com.mgenc.foodjournal.data.toEntity
import com.mgenc.foodjournal.data.toSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val foodApp = app as FoodJournalApp
    private val foodEntryDao = foodApp.database.foodEntryDao()
    private val recipeDao = foodApp.database.recipeDao()
    private val cookingPlanDao = foodApp.database.cookingPlanDao()

    private val _serverUrl = MutableStateFlow(foodApp.serverUrl)
    val serverUrl: StateFlow<String> = _serverUrl

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _lastSyncTime = MutableStateFlow<String?>(null)
    val lastSyncTime: StateFlow<String?> = _lastSyncTime

    private val _syncResult = MutableStateFlow<SyncResult?>(null)
    val syncResult: StateFlow<SyncResult?> = _syncResult

    private val json = Json { ignoreUnknownKeys = true }

    init {
        val lastSyncAt = foodApp.lastSyncAt
        if (lastSyncAt > 0L) {
            _lastSyncTime.value = formatEpochMs(lastSyncAt)
        }
    }

    fun setServerUrl(url: String) {
        _serverUrl.value = url
        foodApp.serverUrl = url
    }

    fun syncDatabase() {
        if (_isSyncing.value) return
        val url = _serverUrl.value.trim()
        if (url.isBlank()) {
            _syncResult.value = SyncResult.Error("Server URL is required")
            return
        }
        _isSyncing.value = true
        _syncResult.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                doSync(url)
            } catch (e: Exception) {
                _syncResult.value = SyncResult.Error(e.message ?: "Sync failed")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private suspend fun doSync(baseUrl: String) {
        val lastSyncAt = foodApp.lastSyncAt

        val changedEntries = foodEntryDao.getChangedSince(lastSyncAt).map { it.toSync() }
        val changedRecipes = recipeDao.getChangedSince(lastSyncAt).map { it.toSync() }
        val changedPlans = cookingPlanDao.getChangedSince(lastSyncAt).map { it.toSync() }

        val request = SyncRequest(
            lastSyncAt = lastSyncAt,
            entries = changedEntries,
            recipes = changedRecipes,
            cookingPlans = changedPlans,
        )

        val syncUrl = buildUrl(baseUrl, "/sync")
        val (status, body) = httpPostJson(syncUrl, json.encodeToString(SyncRequest.serializer(), request))

        when {
            status == 303 -> {
                val errorResp = json.decodeFromString(ErrorResponse.serializer(), body)
                val rebuildUrl = buildUrl(baseUrl, errorResp.rebuildUrl ?: "/rebuild")
                performRebuild(rebuildUrl, baseUrl)
            }
            status == 200 -> {
                val response = json.decodeFromString(SyncResponse.serializer(), body)
                applyServerChanges(response)
                foodApp.lastSyncAt = response.syncedAt
                _lastSyncTime.value = formatEpochMs(response.syncedAt)
                _syncResult.value = SyncResult.Success
            }
            else -> {
                val msg = try {
                    json.decodeFromString(ErrorResponse.serializer(), body).message
                } catch (_: Exception) {
                    "Server error: $status"
                }
                _syncResult.value = SyncResult.Error(msg)
            }
        }
    }

    private suspend fun performRebuild(rebuildUrl: String, baseUrl: String) {
        val dbFile = foodApp.getDatabasePath("food_journal")

        val cursor = foodApp.database.openHelper.readableDatabase.query("PRAGMA wal_checkpoint(FULL)")
        cursor.close()

        val tmpCopy = File.createTempFile("sync_export", ".db", foodApp.cacheDir)
        dbFile.copyTo(tmpCopy, overwrite = true)

        try {
            val (status, body) = httpPostBinary(rebuildUrl, tmpCopy)
            if (status != 200) {
                val msg = try {
                    json.decodeFromString(ErrorResponse.serializer(), body).message
                } catch (_: Exception) {
                    "Rebuild failed: $status"
                }
                _syncResult.value = SyncResult.Error(msg)
                return
            }

            val rebuildResp = json.decodeFromString(RebuildResponse.serializer(), body)
            foodApp.lastSyncAt = rebuildResp.syncedAt

            val syncUrl = buildUrl(baseUrl, "/sync")
            val (syncStatus, syncBody) = httpPostJson(
                syncUrl,
                json.encodeToString(SyncRequest.serializer(), SyncRequest(lastSyncAt = 0L, entries = emptyList(), recipes = emptyList(), cookingPlans = emptyList())),
            )
            if (syncStatus == 200) {
                val syncResp = json.decodeFromString(SyncResponse.serializer(), syncBody)
                applyServerChanges(syncResp)
                foodApp.lastSyncAt = syncResp.syncedAt
                _lastSyncTime.value = formatEpochMs(syncResp.syncedAt)
            } else {
                _lastSyncTime.value = formatEpochMs(rebuildResp.syncedAt)
            }
            _syncResult.value = SyncResult.Success
        } finally {
            tmpCopy.delete()
        }
    }

    private suspend fun applyServerChanges(response: SyncResponse) {
        if (response.entries.isNotEmpty()) {
            foodEntryDao.upsertAll(response.entries.map { it.toEntity() })
        }
        if (response.recipes.isNotEmpty()) {
            recipeDao.upsertAll(response.recipes.map { it.toEntity() })
        }
        if (response.cookingPlans.isNotEmpty()) {
            cookingPlanDao.upsertAll(response.cookingPlans.map { it.toEntity() })
        }
    }

    fun clearSyncResult() {
        _syncResult.value = null
    }

    private fun buildUrl(base: String, path: String): String {
        val b = base.trimEnd('/')
        return "$b$path"
    }

    private fun httpPostJson(url: String, jsonBody: String): Pair<Int, String> {
        val conn = URI(url).toURL().openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        conn.instanceFollowRedirects = false
        conn.outputStream.bufferedWriter().use { it.write(jsonBody) }
        val status = conn.responseCode
        val body = if (status in 200..299 || status == 303) {
            conn.inputStream.bufferedReader().readText()
        } else {
            conn.errorStream?.bufferedReader()?.readText() ?: ""
        }
        conn.disconnect()
        return status to body
    }

    private fun httpPostBinary(url: String, file: File): Pair<Int, String> {
        val conn = URI(url).toURL().openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/octet-stream")
        conn.setRequestProperty("Accept", "application/json")
        file.inputStream().use { input -> conn.outputStream.use { output -> input.copyTo(output) } }
        val status = conn.responseCode
        val body = if (status in 200..299) {
            conn.inputStream.bufferedReader().readText()
        } else {
            conn.errorStream?.bufferedReader()?.readText() ?: ""
        }
        conn.disconnect()
        return status to body
    }

    private fun formatEpochMs(ms: Long): String {
        return LocalDateTime.ofEpochSecond(ms / 1000, ((ms % 1000) * 1_000_000).toInt(), java.time.ZoneOffset.systemDefault().rules.getOffset(java.time.Instant.ofEpochMilli(ms)))
            .format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"))
    }

    sealed class SyncResult {
        data object Success : SyncResult()
        data class Error(val message: String) : SyncResult()
    }
}
