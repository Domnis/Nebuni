package com.domnis.nebuni

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.domnis.nebuni.ui.main.MainPage
import com.domnis.nebuni.ui.theme.NebuniTheme
import com.domnis.nebuni.ui.welcome.WelcomePage
import org.jetbrains.compose.ui.tooling.preview.Preview

import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
    KoinApplication(application = {
        modules(appModule)
    }) {
        NebuniTheme {
            val appState: AppState = koinInject()
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = appState.currentRootScreen.value.toString()
            ) {
                composable(Screen.Welcome.toString()) {
                    WelcomePage()
                }
                composable(Screen.Main.toString()) {
                    MainPage()
                }
            }
        }
    }
}