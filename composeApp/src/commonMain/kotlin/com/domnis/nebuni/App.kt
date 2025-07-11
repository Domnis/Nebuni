package com.domnis.nebuni

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.domnis.nebuni.data.ScienceMission
import com.domnis.nebuni.network.ScienceAPI
import com.domnis.nebuni.ui.theme.NebuniTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.ui.tooling.preview.Preview

import nebuni.composeapp.generated.resources.Res
import nebuni.composeapp.generated.resources.compose_multiplatform

@Serializable
object List
@Serializable
data class Detail(val key: String)

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    NebuniTheme {
        val scope = rememberCoroutineScope()
        var isLoadingMissions by remember { mutableStateOf(false) }

        var scienceMissionMap by remember { mutableStateOf(emptyMap<String, ScienceMission>()) }

        val navController = rememberNavController()
        val bottomSheetState = rememberModalBottomSheetState()
        val sheetScaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

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
                    Box (
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .fillMaxSize(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                isLoadingMissions = true
                                scope.launch(Dispatchers.Default) {
                                    val apiResult = ScienceAPI().listScienceMissions()

                                    launch(Dispatchers.Main) {
                                        isLoadingMissions = false
                                        scienceMissionMap = apiResult
                                    }
                                }
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
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text("Selected mission is: ", maxLines = 1)
                        Text(detail.key, maxLines = 1)
                    }
                }
            }
        }
    }
}