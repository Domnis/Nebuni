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

package com.domnis.nebuni.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domnis.nebuni.AppState
import com.domnis.nebuni.Screen
import com.domnis.nebuni.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashViewModel(val appState: AppState, val database: AppDatabase): ViewModel() {
    init {
        viewModelScope.launch(Dispatchers.Default) {
            delay(2000)

            database.getObservationPlaceDao().getAllAsFlow().collect { places ->
                if (places.isNotEmpty()) {
                    appState.updateObservationPlace(places.first())
                    appState.navigateTo(Screen.Main)
                } else {
                    appState.navigateTo(Screen.Welcome)
                }
            }
        }
    }
}