package com.example.domain.repository

import kotlinx.coroutines.flow.Flow

data class UserPreferences(
    val currencySymbol: String = "₹",
    val showDecimals: Boolean = false,
    val darkTheme: Boolean = true,
    val haptics: Boolean = true,
    val dailyReminder: Boolean = false,
    val budgetAlerts: Boolean = false,
    val defaultPayment: String = "Cash"
)

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setCurrencySymbol(symbol: String)
    suspend fun setShowDecimals(enabled: Boolean)
    suspend fun setDarkTheme(enabled: Boolean)
    suspend fun setHaptics(enabled: Boolean)
    suspend fun setDailyReminder(enabled: Boolean)
    suspend fun setBudgetAlerts(enabled: Boolean)
    suspend fun setDefaultPayment(mode: String)
}
