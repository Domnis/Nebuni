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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.domnis.nebuni.AppState
import com.domnis.nebuni.convertCurrentDateAndTimeToLocalTimeZone
import com.domnis.nebuni.ui.missions.EmptyMissionPage
import com.domnis.nebuni.ui.missions.MissionPage
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Serializable
object List

@Serializable
data class Detail(val key: String)

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class,
    ExperimentalSharedTransitionApi::class, ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
@Preview
fun MainPage(mainViewModel: MainViewModel = koinViewModel(), appState: AppState = koinInject()) {
    val scope = rememberCoroutineScope()
    val currentObservationPlace by appState.currentObservationPlace

    val navigator = rememberListDetailPaneScaffoldNavigator<Nothing>()

    var isLoadingMissions by mainViewModel.isLoadingMissions
    var isListAndDetailVisible by remember { mutableStateOf(false) }

    var scienceMissionMap by mainViewModel.scienceMissionMap
    var selectedMission by mainViewModel.selectedMission

    var startDateTime by mainViewModel.startTime
    var endDateTime by mainViewModel.endTime

    val onBack = {
        scope.launch {
            navigator.navigateBack()
            mainViewModel.unselectMission()
        }
    }

    BackHandler(enabled = navigator.canNavigateBack()) {
        onBack()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(if (!isListAndDetailVisible && !selectedMission.isNullOrEmpty()) selectedMission!! else "Nebuni")
                },
                navigationIcon = {
                    if (!isListAndDetailVisible && !selectedMission.isNullOrEmpty()) {
                        IconButton(
                            onClick = {
                                onBack()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "A back arrow icon"
                            )
                        }
                    }
                },
                actions = {
//                    FilledIconButton(
//                        onClick = {
//                            // navigate to setting
//                        },
//                    ) {
//                        Icon(
//                            imageVector = Icons.Filled.Settings,
//                            contentDescription = "A settings icon"
//                        )
//                    }
                }
            )
        },
    ) { paddingValues ->
        ListDetailPaneScaffold(
            modifier = Modifier.padding(paddingValues),
            directive = navigator.scaffoldDirective,
            value = navigator.scaffoldValue,
            listPane = {
                isListAndDetailVisible =
                    navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded
                            && navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded

                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = "A location icon"
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text("${appState.currentObservationPlace.value.latitude} / ${appState.currentObservationPlace.value.longitude}")
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DateRange,
                                        contentDescription = "A date range icon"
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        convertCurrentDateAndTimeToLocalTimeZone(startDateTime) +
                                            " -> " +
                                            convertCurrentDateAndTimeToLocalTimeZone(endDateTime)
                                    )
                                }

                                HorizontalDivider()
                            }
                        }

                        items(scienceMissionMap.keys.toList()) { key ->
                            Column(
                                Modifier.fillMaxWidth()
                                    .background(
                                        color = if (selectedMission == key)
                                            LocalTextSelectionColors.current.backgroundColor
                                        else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        1.dp,
                                        color = LocalContentColor.current,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(16.dp)
                                    .clickable(
                                        interactionSource = null,
                                        indication = null,
                                        onClickLabel = "Get information for science mission named: $key",
                                        onClick = {
                                            mainViewModel.selectMission(key)

                                            scope.launch {
                                                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                                            }
                                        }
                                    ),
                            ) {
                                Text(key, maxLines = 1)
                            }
                        }

                        item { Spacer(Modifier.height(72.dp)) }
                    }

                    AnimatedVisibility(
                        visible = isLoadingMissions,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(
                                modifier = Modifier.size(112.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            mainViewModel.refreshScienceMissions()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding(),
                        enabled = !isLoadingMissions
                    ) {
                        Text("Fetch science missions")
                    }
                }
            },
            detailPane = {
                if (selectedMission == null) {
                    EmptyMissionPage()
                } else {
                    selectedMission?.let { missionKey ->
                        scienceMissionMap[missionKey]?.let { mission ->
                            MissionPage(
                                missionKey,
                                mission
                            )
                        }
                    }
                }
            }
        )
    }
}