package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.repository.PreferencesRepository
import com.example.domain.repository.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class DataStorePreferencesRepository(private val context: Context) : PreferencesRepository {

    private object Keys {
        val CURRENCY = stringPreferencesKey("currency_symbol")
        val DECIMALS = booleanPreferencesKey("show_decimals")
        val DARK = booleanPreferencesKey("dark_theme")
        val HAPTICS = booleanPreferencesKey("haptics")
        val DAILY = booleanPreferencesKey("daily_reminder")
        val ALERTS = booleanPreferencesKey("budget_alerts")
        val PAYMENT = stringPreferencesKey("default_payment")
    }

    override val preferences: Flow<UserPreferences> = context.dataStore.data.map { p ->
        UserPreferences(
            currencySymbol = p[Keys.CURRENCY] ?: "₹",
            showDecimals = p[Keys.DECIMALS] ?: false,
            darkTheme = p[Keys.DARK] ?: true,
            haptics = p[Keys.HAPTICS] ?: true,
            dailyReminder = p[Keys.DAILY] ?: false,
            budgetAlerts = p[Keys.ALERTS] ?: false,
            defaultPayment = p[Keys.PAYMENT] ?: "Cash"
        )
    }

    override suspend fun setCurrencySymbol(symbol: String) { context.dataStore.edit { it[Keys.CURRENCY] = symbol } }
    override suspend fun setShowDecimals(enabled: Boolean) { context.dataStore.edit { it[Keys.DECIMALS] = enabled } }
    override suspend fun setDarkTheme(enabled: Boolean) { context.dataStore.edit { it[Keys.DARK] = enabled } }
    override suspend fun setHaptics(enabled: Boolean) { context.dataStore.edit { it[Keys.HAPTICS] = enabled } }
    override suspend fun setDailyReminder(enabled: Boolean) { context.dataStore.edit { it[Keys.DAILY] = enabled } }
    override suspend fun setBudgetAlerts(enabled: Boolean) { context.dataStore.edit { it[Keys.ALERTS] = enabled } }
    override suspend fun setDefaultPayment(mode: String) { context.dataStore.edit { it[Keys.PAYMENT] = mode } }
}
