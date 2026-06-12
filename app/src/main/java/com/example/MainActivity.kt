package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.example.ui.FinanceViewModel
import com.example.ui.navigation.XpenseNavHost
import com.example.ui.theme.MyApplicationTheme

@dagger.hilt.android.AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        MainAppContent()
      }
    }
  }
}

@Composable
fun MainAppContent() {
  // Triggers the single idempotent category/account seed on first composition.
  androidx.hilt.navigation.compose.hiltViewModel<com.example.ui.AppBootstrapViewModel>()
  val viewModel: FinanceViewModel = androidx.hilt.navigation.compose.hiltViewModel()

  // Surface swallowed save/validation errors as a transient snackbar.
  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(Unit) {
    viewModel.errors.collect { snackbarHostState.showSnackbar(it) }
  }

  XpenseNavHost(snackbarHostState = snackbarHostState)
}
