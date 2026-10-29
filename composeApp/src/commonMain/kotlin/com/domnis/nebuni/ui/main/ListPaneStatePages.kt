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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import com.domnis.nebuni.data.ScienceMission
import com.domnis.nebuni.ui.theme.fontStyle_header
import nebuni.composeapp.generated.resources.Res
import nebuni.composeapp.generated.resources.cd_info
import nebuni.composeapp.generated.resources.filter_clear
import nebuni.composeapp.generated.resources.filter_no_matching_mission
import nebuni.composeapp.generated.resources.onboarding_intro
import nebuni.composeapp.generated.resources.onboarding_observation_place_info
import nebuni.composeapp.generated.resources.onboarding_privacy
import nebuni.composeapp.generated.resources.onboarding_welcome
import nebuni.composeapp.generated.resources.place_form_altitude_max
import nebuni.composeapp.generated.resources.place_form_altitude_max_hint
import nebuni.composeapp.generated.resources.place_form_altitude_min
import nebuni.composeapp.generated.resources.place_form_altitude_min_hint
import nebuni.composeapp.generated.resources.place_form_azimuth_max
import nebuni.composeapp.generated.resources.place_form_azimuth_max_hint
import nebuni.composeapp.generated.resources.place_form_azimuth_min
import nebuni.composeapp.generated.resources.place_form_azimuth_min_hint
import nebuni.composeapp.generated.resources.place_form_gps_coordinates
import nebuni.composeapp.generated.resources.place_form_latitude
import nebuni.composeapp.generated.resources.place_form_latitude_hint
import nebuni.composeapp.generated.resources.place_form_longitude
import nebuni.composeapp.generated.resources.place_form_longitude_hint
import nebuni.composeapp.generated.resources.place_form_name
import nebuni.composeapp.generated.resources.place_form_name_hint
import nebuni.composeapp.generated.resources.place_form_submit
import nebuni.composeapp.generated.resources.place_form_title
import nebuni.composeapp.generated.resources.place_form_visible_sky_area
import nebuni.composeapp.generated.resources.section_today
import org.jetbrains.compose.resources.stringResource
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ListPaneValidPage(
    scienceMissionList: List<MissionSection>,
    isLoadingMissions: Boolean,
    selectedMission: ScienceMission? = null,
    isFilterResultEmpty: Boolean = false,
    onClearFilters: () -> Unit = {},
    onRefresh: () -> Unit,
    onMissionSelected: (ScienceMission) -> Unit
) {
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isLoadingMissions,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullToRefreshState,
                isRefreshing = isLoadingMissions,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (isFilterResultEmpty) {
                // Kept inside the list so pull-to-refresh still works
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(stringResource(Res.string.filter_no_matching_mission))

                        Button(onClick = onClearFilters) {
                            Text(stringResource(Res.string.filter_clear))
                        }
                    }
                }
            }

            scienceMissionList.forEach { section ->
                stickyHeader {
                    Text(
                        when {
                            section.type != null -> stringResource(section.type.labelResource())
                            section.isToday -> stringResource(Res.string.section_today)
                            else -> section.date
                        },
                        modifier = Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(top = 20.dp, bottom = 8.dp),
                        maxLines = 1,
                        style = fontStyle_header
                    )
                }

                items(section.missions) { mission ->
                    ScienceMissionListItem(
                        mission,
                        modifier = Modifier.padding(bottom = 8.dp),
                        isSelected = selectedMission?.missionKey == mission.missionKey
                    ) {
                        onMissionSelected(mission)
                    }
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
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
                Text(stringResource(Res.string.onboarding_welcome), style = fontStyle_header)
            }

            Text(stringResource(Res.string.onboarding_intro))

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
                        contentDescription = stringResource(Res.string.cd_info)
                    )

                    Text(stringResource(Res.string.onboarding_observation_place_info))
                }
            }

            Text(stringResource(Res.string.onboarding_privacy))

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

    var inputError by remember { mutableStateOf<ObservationPlaceInputError?>(null) }

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
            Text(stringResource(Res.string.place_form_title), style = fontStyle_header)

            OutlinedTextField(
                state = nameState,
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(stringResource(Res.string.place_form_name))
                },
                supportingText = {
                    Text(stringResource(Res.string.place_form_name_hint))
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
                Text(stringResource(Res.string.place_form_gps_coordinates), style = fontStyle_header)

                OutlinedTextField(
                    state = latState,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(stringResource(Res.string.place_form_latitude))
                    },
                    supportingText = {
                        Text(stringResource(Res.string.place_form_latitude_hint))
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
                        Text(stringResource(Res.string.place_form_longitude))
                    },
                    supportingText = {
                        Text(stringResource(Res.string.place_form_longitude_hint))
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
                Text(stringResource(Res.string.place_form_visible_sky_area), style = fontStyle_header)

                OutlinedTextField(
                    state = altMinState,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(stringResource(Res.string.place_form_altitude_min))
                    },
                    supportingText = {
                        Text(stringResource(Res.string.place_form_altitude_min_hint))
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
                        Text(stringResource(Res.string.place_form_altitude_max))
                    },
                    supportingText = {
                        Text(stringResource(Res.string.place_form_altitude_max_hint))
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
                        Text(stringResource(Res.string.place_form_azimuth_min))
                    },
                    supportingText = {
                        Text(stringResource(Res.string.place_form_azimuth_min_hint))
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
                        Text(stringResource(Res.string.place_form_azimuth_max))
                    },
                    supportingText = {
                        Text(stringResource(Res.string.place_form_azimuth_max_hint))
                    },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )
            }

            inputError?.let { error ->
                Text(stringResource(error.message), color = MaterialTheme.colorScheme.error)
            }

            val size = ButtonDefaults.MediumContainerHeight
            Button(
                onClick = {
                    val result = validateObservationPlaceInput(
                        name = nameState.text.toString(),
                        latitude = latState.text.toString(),
                        longitude = lonState.text.toString(),
                        altMin = altMinState.text.toString(),
                        altMax = altMaxState.text.toString(),
                        azMin = azMinState.text.toString(),
                        azMax = azMaxState.text.toString()
                    )
                    when (result) {
                        is ObservationPlaceInputResult.Invalid -> inputError = result.error
                        is ObservationPlaceInputResult.Valid -> mainViewModel.addObservationPlace(result.place)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(size),
                contentPadding = ButtonDefaults.contentPaddingFor(size)
            ) {
                Text(stringResource(Res.string.place_form_submit), style = ButtonDefaults.textStyleFor(size))
            }
        }
    }
}