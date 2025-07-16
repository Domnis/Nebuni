package com.domnis.nebuni

import androidx.compose.runtime.mutableStateOf
import com.domnis.nebuni.data.ObservationPlace

enum class Screen { Welcome, Main }

class AppState {
    val currentRootScreen = mutableStateOf(Screen.Welcome)

    val currentObservationPlace = mutableStateOf(ObservationPlace())

    fun navigateTo(newRootScreen: Screen) {
        currentRootScreen.value = newRootScreen
    }

    fun updateObservationPlace(newObservationPlace: ObservationPlace) {
        currentObservationPlace.value = newObservationPlace
    }
}