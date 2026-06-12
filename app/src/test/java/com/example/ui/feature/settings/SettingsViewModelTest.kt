package com.example.ui.feature.settings

import com.example.domain.FakeTransactionRepository
import com.example.domain.model.Transaction
import com.example.domain.model.TxnType
import com.example.domain.repository.PreferencesRepository
import com.example.domain.repository.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakePreferencesRepository : PreferencesRepository {
    private val flow = MutableStateFlow(UserPreferences())
    override val preferences: Flow<UserPreferences> = flow
    override suspend fun setCurrencySymbol(symbol: String) { flow.value = flow.value.copy(currencySymbol = symbol) }
    override suspend fun setShowDecimals(enabled: Boolean) { flow.value = flow.value.copy(showDecimals = enabled) }
    override suspend fun setDarkTheme(enabled: Boolean) { flow.value = flow.value.copy(darkTheme = enabled) }
    override suspend fun setHaptics(enabled: Boolean) { flow.value = flow.value.copy(haptics = enabled) }
    override suspend fun setDailyReminder(enabled: Boolean) { flow.value = flow.value.copy(dailyReminder = enabled) }
    override suspend fun setBudgetAlerts(enabled: Boolean) { flow.value = flow.value.copy(budgetAlerts = enabled) }
    override suspend fun setDefaultPayment(mode: String) { flow.value = flow.value.copy(defaultPayment = mode) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val prefs = FakePreferencesRepository()
    private val txnRepo = FakeTransactionRepository()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `toggles persist into preferences flow`() = runTest {
        val vm = SettingsViewModel(prefs, txnRepo)
        vm.setShowDecimals(true)
        vm.setDailyReminder(true)
        val s = vm.state.first { it.showDecimals }
        assertTrue(s.showDecimals)
        assertTrue(prefs.preferences.first().dailyReminder)
    }

    @Test fun `delete all data clears transactions`() = runTest {
        txnRepo.upsert(Transaction(0, TxnType.EXPENSE, 1.0, "x", 1, null, 0, ""))
        val vm = SettingsViewModel(prefs, txnRepo)
        vm.deleteAllData()
        assertEquals(0, txnRepo.all().first().size)
    }
}
