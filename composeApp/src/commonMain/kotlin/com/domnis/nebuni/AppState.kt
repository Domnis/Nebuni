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

import androidx.compose.runtime.mutableStateOf
import com.domnis.nebuni.data.ObservationPlace

enum class Screen { Splash, Welcome, Main }

class AppState {
//    val currentWindowSizeClass = mutableStateOf(WindowSizeClass.compute(300f, 1200f))
    val currentRootScreen = mutableStateOf(Screen.Splash)

    val currentObservationPlace = mutableStateOf(ObservationPlace())

//    fun setWindowSizeClass(sizeClass: WindowSizeClass) {
//        currentWindowSizeClass.value = sizeClass
//    }

    fun navigateTo(newRootScreen: Screen) {
        if (newRootScreen == currentRootScreen.value) return

        currentRootScreen.value = newRootScreen
    }

    fun updateObservationPlace(newObservationPlace: ObservationPlace) {
        currentObservationPlace.value = newObservationPlace
    }
}