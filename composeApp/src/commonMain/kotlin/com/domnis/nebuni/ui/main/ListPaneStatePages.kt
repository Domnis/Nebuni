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

package com.domnis.nebuni.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.domnis.nebuni.AppState
import com.domnis.nebuni.data.ObservationPlace
import com.domnis.nebuni.data.ScienceMission
import com.domnis.nebuni.ui.theme.fontStyle_header
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ListPaneLoadingPage() {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(
            modifier = Modifier.size(112.dp)
        )
    }
}

@Composable
fun ListPaneValidPage(
    scienceMissionList: ArrayList<Pair<String, List<ScienceMission>>>,
    selectedMission: ScienceMission? = null,
    onMissionSelected: (ScienceMission) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(2.dp))
        }

        scienceMissionList.forEach { section ->
            item {
                Text(
                    section.first,
                    modifier = Modifier.padding(top = 12.dp),
                    maxLines = 1,
                    style = fontStyle_header
                )
            }

            items(section.second) { mission ->
                ScienceMissionListItem(
                    mission,
                    isSelected = selectedMission?.missionKey == mission.missionKey
                ) {
                    onMissionSelected(mission)
                }
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
fun ListPaneInvalidPage(
    appState: AppState = koinInject(),
    isListAndDetailVisible: Boolean = false
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.systemBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Welcome to Nebuni! \uD83D\uDC30", style = fontStyle_header)
            }

            Text("Let's start by creating your first Observation Place!")

            OutlinedCard(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "An info icon"
                    )

                    Text("Observation Place represents a geographic position " +
                            "associated with a \"Visible Sky Area\" (VSA).\n" +
                            "VSA is defined by a range of azimuth and altitude. " +
                            "(See Unistellar app for more details)"
                    )
                }
            }

            Text("No worry, " +
                    "GPS coordinate of Observation Place will only be used when " +
                    "you'll ask a refresh of the mission's list. " +
                    "Observation Place are stored locally on your device.")

            if (!isListAndDetailVisible) {
                HorizontalDivider()

                ListPaneInvalidFormPage()
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ListPaneInvalidFormPage(
    appState: AppState = koinInject(),
    mainViewModel: MainViewModel = koinViewModel(),
) {
    val nameState = rememberTextFieldState(initialText = appState.currentObservationPlace.value.name)

    val latState = rememberTextFieldState(initialText = appState.currentObservationPlace.value.latitude.toString())
    val lonState = rememberTextFieldState(initialText = appState.currentObservationPlace.value.longitude.toString())

    val altMinState = rememberTextFieldState(initialText = appState.currentObservationPlace.value.altMin.toString())
    val altMaxState = rememberTextFieldState(initialText = appState.currentObservationPlace.value.altMax.toString())

    val azMinState = rememberTextFieldState(initialText = appState.currentObservationPlace.value.azMin.toString())
    val azMaxState = rememberTextFieldState(initialText = appState.currentObservationPlace.value.azMax.toString())

    var errorText by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Create a new Observation Place", style = fontStyle_header)

            OutlinedTextField(
                state = nameState,
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Name")
                },
                supportingText = {
                    Text("Home, Home - Balcony, etc...")
                },
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next
                )
            )

            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("GPS Coordinates", style = fontStyle_header)

                OutlinedTextField(
                    state = latState,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Latitude")
                    },
                    supportingText = {
                        Text("Should be between -90° and 90°")
                    },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    state = lonState,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Longitude")
                    },
                    supportingText = {
                        Text("Should be between -180° and 180°")
                    },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next
                    )
                )
            }

            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Visible Sky Area", style = fontStyle_header)

                OutlinedTextField(
                    state = altMinState,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Altitude Min")
                    },
                    supportingText = {
                        Text("Should be between 0° and 90°")
                    },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    state = altMaxState,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Altitude Max")
                    },
                    supportingText = {
                        Text("Should be between 0° and 90° and bigger than Altitude Min")
                    },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    state = azMinState,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Azimuth Min")
                    },
                    supportingText = {
                        Text("Should be between 0° and 360°")
                    },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    state = azMaxState,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Azimuth Max")
                    },
                    supportingText = {
                        Text("Should be between 0° and 360° and bigger than Azimuth Min")
                    },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )
            }

            if (errorText != null) {
                Text(errorText ?: "", color = MaterialTheme.colorScheme.error)
            }

            val size = ButtonDefaults.MediumContainerHeight
            Button(
                onClick = {
                    //TODO: Refactor how to check data and display error
                    val latText = latState.text.toString().trim().replace(",", ".")
                    val lonText = lonState.text.toString().trim().replace(",", ".")

                    if (latText.isEmpty() || latText.toDoubleOrNull() == null) {
                        //TODO: show error
                        errorText = "Latitude is in incorrect format"
                        return@Button
                    }

                    val lat = latText.toDouble()
                    if (lat < -90.0 || lat > 90.0) {
                        //TODO: show error
                        errorText = "Latitude should be between -90° and 90°"
                        return@Button
                    }

                    if (lonText.isEmpty() || lonText.toDoubleOrNull() == null) {
                        //TODO: show error
                        errorText = "Longitude is in incorrect format"
                        return@Button
                    }

                    val lon = lonText.toDouble()
                    if (lon < -180.0 || lon > 180.0) {
                        //TODO: show error
                        errorText = "Longitude should be between -180° and 180°"
                        return@Button
                    }

                    val altMinText = altMinState.text.toString().trim().replace(",", ".")
                    val altMaxText = altMaxState.text.toString().trim().replace(",", ".")
                    val azMinText = azMinState.text.toString().trim().replace(",", ".")
                    val azMaxText = azMaxState.text.toString().trim().replace(",", ".")

                    if (altMinText.isEmpty() || altMinText.toIntOrNull() == null) {
                        //TODO: show error
                        errorText = "Altitude Min is in incorrect format"
                        return@Button
                    }

                    if (altMaxText.isEmpty() || altMaxText.toIntOrNull() == null) {
                        //TODO: show error
                        errorText = "Altitude Max is in incorrect format"
                        return@Button
                    }

                    val altMin = altMinText.toInt()
                    if (altMin < 0 || altMin > 90) {
                        //TODO: show error
                        errorText = "Altitude Min should be between 0° (horizon) and 90° (zenith)"
                        return@Button
                    }

                    val altMax = altMaxText.toInt()
                    if (altMax < 0 || altMax > 90 || altMax < altMin) {
                        //TODO: show error
                        errorText =
                            "Altitude Max should be between 0° (horizon) and 90° (zenith) and bigger than Altitude Min"
                        return@Button
                    }

                    if (azMinText.isEmpty() || azMinText.toIntOrNull() == null) {
                        //TODO: show error
                        errorText = "Azimuth Min is in incorrect format"
                        return@Button
                    }

                    if (azMaxText.isEmpty() || azMaxText.toIntOrNull() == null) {
                        //TODO: show error
                        errorText = "Azimuth Max is in incorrect format"
                        return@Button
                    }

                    val azMin = azMinText.toInt()
                    if (azMin < 0 || azMin > 360) {
                        //TODO: show error
                        errorText = "Azimuth Min should be between 0° and 360°"
                        return@Button
                    }

                    val azMax = azMaxText.toInt()
                    if (azMax < 0 || azMax > 360 || azMax < azMin) {
                        //TODO: show error
                        errorText =
                            "Azimuth Max should be between 0° and 360° and bigger than Azimuth Min"
                        return@Button
                    }

                    val place = ObservationPlace(
                        name = nameState.text.toString().trim(),
                        latitude = lat,
                        longitude = lon,
                        altMin = altMin,
                        altMax = altMax,
                        azMin = azMin,
                        azMax = azMax
                    )

                    mainViewModel.addObservationPlace(place)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(size),
                contentPadding = ButtonDefaults.contentPaddingFor(size)
            ) {
                Text("Create Observation Place", style = ButtonDefaults.textStyleFor(size))
            }
        }
    }
}