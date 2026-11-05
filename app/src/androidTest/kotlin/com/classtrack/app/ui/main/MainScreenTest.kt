package com.classtrack.app.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.viewmodel.compose.viewModel
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun headerTitle_isDisplayed() {
        composeTestRule.setContent {
            val vm: MainScreenViewModel = viewModel()
            MainScreen(viewModel = vm)
        }
        composeTestRule.onNodeWithText("课轨").assertExists()
    }
}
