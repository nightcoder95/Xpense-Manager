package com.example.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.designsystem.BottomNavBar
import com.example.core.designsystem.XColors
import com.example.ui.FinanceViewModel
import com.example.ui.feature.accounts.AccountsScreen
import com.example.ui.feature.analysis.AnalysisScreen
import com.example.ui.feature.home.HomeScreen
import com.example.ui.feature.more.MoreScreen
import com.example.ui.screens.AddTransactionScreen

/**
 * Single navigation graph. The bottom bar drives the four tab destinations; the center
 * FAB opens Add as a real full-screen destination so it gets its own back stack and insets
 * (fixes A1#17 — no more hand-rolled overlay).
 */
@Composable
fun XpenseNavHost(
    snackbarHostState: SnackbarHostState,
    navController: NavHostController = rememberNavController()
) {
    val viewModel: FinanceViewModel = hiltViewModel()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val isAddSheetOpen by viewModel.isAddSheetOpen.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Surface ViewModel errors as snackbars (replaces swallowed exceptions — Part E).
    LaunchedEffect(Unit) {
        viewModel.errors.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    // Bridge legacy openAddTransaction()/closeAddTransaction() calls (used by list-row edit)
    // into real navigation so existing screens drive the Add destination unchanged.
    LaunchedEffect(isAddSheetOpen) {
        if (isAddSheetOpen && currentRoute != Routes.ADD) {
            navController.navigate(Routes.ADD)
        } else if (!isAddSheetOpen && currentRoute == Routes.ADD) {
            navController.popBackStack()
        }
    }

    val showBottomBar = currentRoute in Routes.bottomTabs

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = XColors.Background,
        contentWindowInsets = WindowInsets.systemBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    current = currentRoute ?: Routes.HOME,
                    onSelect = { route ->
                        navController.navigate(route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onAdd = { viewModel.openAddTransaction(null) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onSeeAllRecent = { navController.navigate(Routes.DAY) },
                    onSetBudget = { navController.navigate(Routes.BUDGETS) },
                    onCustomize = { navController.navigate(Routes.MORE) },
                    onTransactionClick = { id ->
                        val txn = transactions.firstOrNull { it.id == id }
                        viewModel.openAddTransaction(txn)
                    }
                )
            }
            composable(Routes.ANALYSIS) { AnalysisScreen(onSetBudget = { navController.navigate(Routes.BUDGETS) }) }
            composable(Routes.ACCOUNTS) {
                AccountsScreen(
                    onAddAccount = { navController.navigate(Routes.EDIT_ACCOUNT) },
                    onAccountClick = { id -> navController.navigate("${Routes.EDIT_ACCOUNT}?id=$id") }
                )
            }
            composable(Routes.MORE) {
                val scope = rememberCoroutineScope()
                MoreScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onPlaceholder = { label ->
                        scope.launch { snackbarHostState.showSnackbar("$label coming soon") }
                    }
                )
            }
            composable(Routes.ADD) {
                AddTransactionScreen(
                    viewModel = viewModel,
                    categories = categories,
                    onClose = { viewModel.closeAddTransaction() },
                    onEditCategories = { navController.navigate(Routes.CATEGORIES) }
                )
            }
            composable(Routes.CATEGORIES) {
                com.example.ui.feature.categories.CategoriesScreen(
                    onBack = { navController.popBackStack() },
                    onAddCategory = { navController.navigate(Routes.EDIT_CATEGORY) },
                    onEditCategory = { name -> navController.navigate("${Routes.EDIT_CATEGORY}?name=$name") }
                )
            }
            composable(
                route = "${Routes.EDIT_CATEGORY}?name={name}",
                arguments = listOf(navArgument("name") { type = NavType.StringType; defaultValue = "" })
            ) {
                com.example.ui.feature.editcategory.EditCategoryScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = "${Routes.EDIT_ACCOUNT}?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType; defaultValue = "" })
            ) {
                com.example.ui.feature.editaccount.EditAccountScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.DAY) {
                com.example.ui.feature.views.DayViewScreen(onTransactionClick = { id ->
                    transactions.firstOrNull { it.id == id }?.let { viewModel.openAddTransaction(it) }
                })
            }
            composable(Routes.CALENDAR) {
                com.example.ui.feature.views.CalendarViewScreen(onDayClick = { navController.navigate(Routes.DAY) })
            }
            composable(Routes.CUSTOM) {
                com.example.ui.feature.views.CustomViewScreen(onTransactionClick = { id ->
                    transactions.firstOrNull { it.id == id }?.let { viewModel.openAddTransaction(it) }
                })
            }
            composable(Routes.BUDGETS) {
                com.example.ui.feature.budgets.BudgetsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                com.example.ui.feature.settings.SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.TAGS) {
                com.example.ui.feature.tags.TagsScreen(
                    onBack = { navController.popBackStack() },
                    onTagClick = { navController.navigate(Routes.CUSTOM) }
                )
            }
        }
    }
}
