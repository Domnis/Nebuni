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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.domnis.nebuni.AppState
import com.domnis.nebuni.data.ScienceMission
import com.domnis.nebuni.network.ScienceAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Serializable
object List

@Serializable
data class Detail(val key: String)

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
@Preview
fun MainPage(appState: AppState = koinInject()) {
    val scope = rememberCoroutineScope()
    val currentObservationPlace by appState.currentObservationPlace

    var isLoadingMissions by remember { mutableStateOf(false) }

    var scienceMissionMap by remember { mutableStateOf(emptyMap<String, ScienceMission>()) }

    val navController = rememberNavController()
    val bottomSheetState = rememberModalBottomSheetState()
    val sheetScaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

    fun refreshScienceMissions() {
        isLoadingMissions = true
        scope.launch(Dispatchers.Default) {
            val apiResult = ScienceAPI().listScienceMissions(currentObservationPlace)

            launch(Dispatchers.Main) {
                isLoadingMissions = false
                scienceMissionMap = apiResult
            }
        }
    }

    LaunchedEffect(null) {
        refreshScienceMissions()
    }

    BottomSheetScaffold(
        sheetContent = {
            // implements sheets as needed
        },
        scaffoldState = sheetScaffoldState,
        sheetPeekHeight = 0.dp,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Nebuni")
                },
                navigationIcon = {
                    val currentRoute =
                        navController.currentBackStackEntryAsState().value?.destination?.route

                    if (currentRoute?.contains("detail", ignoreCase = true) == true) {
                        IconButton(
                            onClick = {
                                navController.navigateUp()
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
                    FilledIconButton(
                        onClick = {
                            // navigate to setting
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "A settings icon"
                        )
                    }
                }
            )
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = List,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable<List> {
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
                            Column {
                                Text("Position used:", maxLines = 1)
                                Text("${appState.currentObservationPlace.value.latitude}")
                                Text("${appState.currentObservationPlace.value.longitude}")

                                HorizontalDivider()
                            }
                        }

                        items(scienceMissionMap.keys.toList()) { key ->
                            Column(
                                Modifier.fillMaxWidth()
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
                                            navController.navigate(Detail(key))
                                        }
                                    ),
                            ) {
                                Text(key, maxLines = 1)
                            }
                        }

                        item { Spacer(Modifier.height(64.dp)) }
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
                            refreshScienceMissions()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .systemBarsPadding(),
                        enabled = !isLoadingMissions
                    ) {
                        Text("Fetch science missions")
                    }
                }
            }
            composable<Detail> { backStackEntry ->
                val detail: Detail = backStackEntry.toRoute()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Selected mission is: ", maxLines = 1)
                    Text(detail.key, maxLines = 1)

                    val scienceMission = scienceMissionMap[detail.key]
                    var deepLink: String = ""
                    when (scienceMission) {
                        is ScienceMission.Occultation -> deepLink = scienceMission.data.deeplink
                        is ScienceMission.Comet -> deepLink = scienceMission.data.deeplink
                        is ScienceMission.Defense -> deepLink = scienceMission.data.deeplink
                        is ScienceMission.Transit -> deepLink = scienceMission.data.deeplink
                        else -> deepLink = ""
                    }

                    if (deepLink.isNotEmpty()) {
                        val uriHandler = LocalUriHandler.current

                        Button(
                            onClick = {
                                uriHandler.openUri(deepLink)
                            },
                            modifier = Modifier
                                .systemBarsPadding()
                        ) {
                            Text("Open mission in Unistellar app")
                        }
                    }
                }
            }
        }
    }
}