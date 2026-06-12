package com.example.ui.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.domain.repository.PreferencesRepository
import com.example.domain.repository.TransactionRepository
import com.example.domain.repository.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesRepository,
    private val txnRepo: TransactionRepository,
    private val backupManager: BackupManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val state: StateFlow<UserPreferences> =
        prefs.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    /** One-shot user feedback (export/import results) surfaced as toasts by the screen. */
    val messages: SharedFlow<String> = _messages

    fun setShowDecimals(v: Boolean) = viewModelScope.launch { prefs.setShowDecimals(v) }
    fun setDarkTheme(v: Boolean) = viewModelScope.launch { prefs.setDarkTheme(v) }
    fun setHaptics(v: Boolean) = viewModelScope.launch { prefs.setHaptics(v) }
    fun setDailyReminder(v: Boolean) = viewModelScope.launch { prefs.setDailyReminder(v) }
    fun setBudgetAlerts(v: Boolean) = viewModelScope.launch { prefs.setBudgetAlerts(v) }
    fun setCurrencySymbol(v: String) = viewModelScope.launch { prefs.setCurrencySymbol(v) }
    fun deleteAllData() = viewModelScope.launch { txnRepo.deleteAll() }

    fun exportTo(uri: Uri) = viewModelScope.launch {
        runCatching {
            val json = backupManager.export()
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    ?: error("Could not open file")
            }
        }.onSuccess { _messages.tryEmit("Backup exported") }
            .onFailure { _messages.tryEmit("Export failed: ${it.message}") }
    }

    fun importFrom(uri: Uri) = viewModelScope.launch {
        runCatching {
            val json = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Could not open file")
            }
            backupManager.import(json)
        }.onSuccess { _messages.tryEmit("Backup restored") }
            .onFailure { _messages.tryEmit("Import failed: ${it.message}") }
    }
}
