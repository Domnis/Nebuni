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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.domnis.nebuni.AppState
import com.domnis.nebuni.data.ObservationPlace
import com.domnis.nebuni.data.ScienceMission
import com.domnis.nebuni.ui.missions.EmptyMissionPage
import com.domnis.nebuni.ui.missions.MissionPage
import com.domnis.nebuni.ui.theme.fontStyle_header
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nebuni.composeapp.generated.resources.Res
import nebuni.composeapp.generated.resources.e911_emergency
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class,
    ExperimentalSharedTransitionApi::class, ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
@Preview
fun MainPage(mainViewModel: MainViewModel = koinViewModel(), appState: AppState = koinInject()) {
    val scope = rememberCoroutineScope()
    val navigator = rememberListDetailPaneScaffoldNavigator<Nothing>()

    var isLoadingMissions by mainViewModel.isLoadingMissions
    var isListAndDetailVisible by remember { mutableStateOf(false) }

    var scienceMissionList by mainViewModel.scienceMissionList
    var selectedMission by mainViewModel.selectedMission

    var startDateTime by mainViewModel.startTime
    var endDateTime by mainViewModel.endTime

    val onBack = {
        scope.launch {
            navigator.navigateBack()

            launch(Dispatchers.Default) {
                delay(100) // add delay to avoid visual glitch

                mainViewModel.unselectMission()
            }
        }
    }

    BackHandler(enabled = navigator.canNavigateBack()) {
        onBack()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (!isListAndDetailVisible && selectedMission != null)
                            selectedMission!!.target_name
                        else "Nebuni",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    if (!isListAndDetailVisible && selectedMission != null) {
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
                    if (isListAndDetailVisible || selectedMission == null) {
                        FilledIconButton(
                            onClick = {
                                mainViewModel.refreshScienceMissions()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "A refresh icon"
                            )
                        }
                    }
                }
            )
        },
    ) { paddingValues ->
        val currentObservationPlaceConfigurationState by mainViewModel.currentObservationPlaceConfigurationState
        val currentObservationPlace by appState.currentObservationPlace

        ListDetailPaneScaffold(
            modifier = Modifier.padding(
                start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                top = paddingValues.calculateTopPadding(),
                end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
            )
                .padding(horizontal = 16.dp),
            directive = navigator.scaffoldDirective,
            value = navigator.scaffoldValue,
            listPane = {
                isListAndDetailVisible =
                    navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded
                            && navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded

                AnimatedPane {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Column {
                            if (
                                currentObservationPlaceConfigurationState
                                    != MainViewModel.ObservationPlaceConfigurationState.invalid
                                ) {
                                //header card
                                ListPaneHeaderCard(
                                    currentObservationPlace = currentObservationPlace,
                                    startDateTime = startDateTime,
                                    endDateTime = endDateTime,
                                    currentObservationPlaceConfigurationState = currentObservationPlaceConfigurationState
                                )
                            }

                            AnimatedContent(
                                targetState = currentObservationPlaceConfigurationState,
                                modifier = Modifier.fillMaxSize()
                            ) { targetState ->
                                when (targetState) {
                                    MainViewModel.ObservationPlaceConfigurationState.invalid ->
                                        ListPaneInvalidPage(
                                            isListAndDetailVisible = isListAndDetailVisible
                                        )

                                    MainViewModel.ObservationPlaceConfigurationState.valid -> {
                                        if (isLoadingMissions) {
                                            ListPaneLoadingPage()
                                        } else {
                                            ListPaneValidPage(
                                                scienceMissionList,
                                                selectedMission
                                            ) { mission ->
                                                mainViewModel.selectMission(mission)

                                                scope.launch {
                                                    navigator.navigateTo(
                                                        ListDetailPaneScaffoldRole.Detail
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    MainViewModel.ObservationPlaceConfigurationState.loading ->
                                        ListPaneLoadingPage()
                                }
                            }
                        }
                    }
                }
            },
            detailPane = {
                AnimatedPane {
                    Surface(
                        modifier = Modifier.padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        if (currentObservationPlaceConfigurationState
                            == MainViewModel.ObservationPlaceConfigurationState.invalid
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(16.dp)//.statusBarsPadding()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Top
                                ) {
                                    ListPaneInvalidFormPage()
                                }
                            }
                        } else if (selectedMission == null) {
                            EmptyMissionPage()
                        } else {
                            selectedMission?.let { mission ->
                                MissionPage(
                                    mission,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .padding(top = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun ListPaneHeaderCard(
    currentObservationPlace: ObservationPlace,
    startDateTime: String,
    endDateTime: String,
    modifier: Modifier = Modifier,
    currentObservationPlaceConfigurationState: MainViewModel.ObservationPlaceConfigurationState = MainViewModel.ObservationPlaceConfigurationState.loading
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
//                                    .clickable {
//                                        // show list of saved ObservationPlace or option to add one
//                                    }
//                                    .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = "A location icon"
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                if (currentObservationPlaceConfigurationState == MainViewModel.ObservationPlaceConfigurationState.valid)
                    currentObservationPlace.name
                else "???"
            )
        }

//        Row(
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Icon(
//                imageVector = Icons.Filled.DateRange,
//                contentDescription = "A date range icon"
//            )
//
//            Spacer(modifier = Modifier.width(8.dp))
//
//            Text(
//                convertCurrentDateAndTimeToLocalTimeZone(startDateTime) +
//                        " -> " +
//                        convertCurrentDateAndTimeToLocalTimeZone(
//                            endDateTime
//                        )
//            )
//        }

        HorizontalDivider()
    }
}

@Composable
fun ScienceMissionListItem(
    mission: ScienceMission,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = if (isSelected)
            CardDefaults.cardColors()
                .copy(containerColor = LocalTextSelectionColors.current.backgroundColor)
        else CardDefaults.cardColors(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (mission.priority) {
                    Icon(
                        painter = painterResource(Res.drawable.e911_emergency),
                        contentDescription = "A priority icon"
                    )
                }

                Text(
                    mission.target_name,
                    maxLines = 1,
                    style = fontStyle_header
                )
            }

            Text(
                "Mission type: ${mission.getMissionType().displayName}",
                maxLines = 1
            )

            Text(
                "Start at: ${mission.getMissionStartDate()}",
                maxLines = 1
            )
        }
    }
}