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

package com.domnis.nebuni.ui.welcome

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.domnis.nebuni.AppState
import com.domnis.nebuni.Screen
import com.domnis.nebuni.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
@Preview
fun WelcomePage(appState: AppState = koinInject(), database: AppDatabase = koinInject()) {
    val coroutineScope = rememberCoroutineScope()

    val currentObservationPlace by appState.currentObservationPlace

    val latState = rememberTextFieldState(initialText = currentObservationPlace.latitude.toString())
    val lonState = rememberTextFieldState(initialText = currentObservationPlace.longitude.toString())

    Scaffold {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().scrollable(
                    rememberScrollState(0),
                    orientation = Orientation.Vertical
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {

                Spacer(modifier = Modifier.height(64.dp))

                Column(
                    modifier = Modifier.systemBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Welcome to Nebuni!")
                    Text("Let's start with a first configuration!")
                    Text("We need to get your GPS coordinate to configure the app. No worry, those information will only be used when you'll ask a refresh of the data.")
                }

                Spacer(modifier = Modifier.heightIn(min = 64.dp, max = 256.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    OutlinedTextField(
                        state = latState,
                        label = {
                            Text("Latitude")
                        },
                        lineLimits = TextFieldLineLimits.SingleLine,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        )
                    )

                    OutlinedTextField(
                        state = lonState,
                        label = {
                            Text("Longitude")
                        },
                        lineLimits = TextFieldLineLimits.SingleLine,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        )
                    )
                }
            }

            Button(
                onClick = {
                    val latText = latState.text.toString().trim().replace(",", ".")
                    val lonText = lonState.text.toString().trim().replace(",", ".")

                    if (latText.isEmpty() || latText.toDoubleOrNull() == null) {
                        //TODO: show error
                        return@Button
                    }

                    if (lonText.isEmpty() || lonText.toDoubleOrNull() == null) {
                        //TODO: show error
                        return@Button
                    }

                    val place = currentObservationPlace.copy(
                        latitude = latText.toDouble(),
                        longitude = lonText.toDouble()
                    )

                    coroutineScope.launch(Dispatchers.Default) {
                        database.getDao().insert(place)
                    }

                    appState.updateObservationPlace(place)

                    appState.navigateTo(Screen.Main)
                },
                modifier = Modifier.align(Alignment.BottomCenter).systemBarsPadding().height(64.dp)
            ) {
                Text("Validate")
            }
        }
    }
}