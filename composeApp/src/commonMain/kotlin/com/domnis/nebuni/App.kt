/*
 * Nebuni
 * Copyright (c) 2025 Sylvain.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.domnis.nebuni

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.domnis.nebuni.ui.main.MainPage
import com.domnis.nebuni.ui.splash.SplashView
import com.domnis.nebuni.ui.theme.NebuniTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinMultiplatformApplication
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinConfiguration

@OptIn(KoinExperimentalAPI::class)
@Composable
@Preview
fun App() {
    KoinMultiplatformApplication(config = koinConfiguration {
        modules(
            platformModule(),
            databaseModule,
            appModule,
        )
    }) {
        NebuniTheme {
            val appState: AppState = koinInject()
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = appState.currentRootScreen.value.toString()
            ) {
                composable(Screen.Splash.toString()) {
                    SplashView()
                }
                composable(Screen.Main.toString()) {
                    MainPage()
                }
            }
        }
    }
}