package com.domnis.nebuni

import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.Serializable

enum class Screen { Welcome, Main }

class AppState {
    val currentRootScreen = mutableStateOf(Screen.Welcome)

    fun navigateTo(newRootScreen: Screen) {
        currentRootScreen.value = newRootScreen
    }
}