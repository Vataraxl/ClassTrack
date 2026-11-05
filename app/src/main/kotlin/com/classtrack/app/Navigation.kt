package com.classtrack.app

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.classtrack.app.ui.main.MainScreen
import com.classtrack.app.ui.main.MainScreenViewModel

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Main)
    val mainViewModel: MainScreenViewModel = viewModel()

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Main> {
                MainScreen(viewModel = mainViewModel)
            }
        }
    )
}
