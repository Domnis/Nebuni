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

package com.domnis.nebuni.ui.missions

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.domnis.nebuni.data.EphemerisData
import com.domnis.nebuni.data.ScienceMission
import com.domnis.nebuni.data.ScienceMissionType
import com.domnis.nebuni.repository.ScienceMissionRepository
import com.domnis.nebuni.ui.theme.fontStyle_header
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import nebuni.composeapp.generated.resources.Res
import nebuni.composeapp.generated.resources.e911_emergency
import nebuni.composeapp.generated.resources.mission_alt_az
import nebuni.composeapp.generated.resources.mission_alt_az_direction
import nebuni.composeapp.generated.resources.mission_constellation
import nebuni.composeapp.generated.resources.mission_date_and_duration
import nebuni.composeapp.generated.resources.mission_dec
import nebuni.composeapp.generated.resources.mission_duration
import nebuni.composeapp.generated.resources.mission_empty
import nebuni.composeapp.generated.resources.mission_event_info
import nebuni.composeapp.generated.resources.mission_from
import nebuni.composeapp.generated.resources.mission_logo_description
import nebuni.composeapp.generated.resources.mission_open_in_app
import nebuni.composeapp.generated.resources.mission_open_in_app_icon_description
import nebuni.composeapp.generated.resources.mission_position
import nebuni.composeapp.generated.resources.mission_position_at
import nebuni.composeapp.generated.resources.mission_priority
import nebuni.composeapp.generated.resources.mission_priority_icon_description
import nebuni.composeapp.generated.resources.mission_ra
import nebuni.composeapp.generated.resources.mission_target
import nebuni.composeapp.generated.resources.mission_to
import nebuni.composeapp.generated.resources.mission_type_label
import nebuni.composeapp.generated.resources.nebuni
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTime::class)
@Composable
@Preview
fun MissionPage(
    mission: ScienceMission,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        var missionEphemerisData by remember { mutableStateOf(listOf<EphemerisData>()) }
        var isLoadingEphemerisData by remember { mutableStateOf(false) }
        val scienceMissionRepository: ScienceMissionRepository = koinInject()

        LaunchedEffect(mission) {
            if (
                mission.getMissionType() == ScienceMissionType.CometaryActivity
                || mission.getMissionType() == ScienceMissionType.PlanetaryDefense
            ) {
                isLoadingEphemerisData = true
                missionEphemerisData = listOf()
                coroutineScope {
                    async(Dispatchers.IO) {
                        missionEphemerisData =
                            if (mission.getMissionType() == ScienceMissionType.CometaryActivity) {
                                scienceMissionRepository.getCometEphemerisData(
                                    cometScienceMission = mission
                                )
                            } else {
                                scienceMissionRepository.getPlanetaryDefenseData(
                                    planetaryDefenseScienceMission = mission
                                )
                            }
                        isLoadingEphemerisData = false
                    }
                }
            }
        }

        if (mission.getMissionType() == ScienceMissionType.Unknown) {
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            DataMissionPage(mission, missionEphemerisData)
        }

        val uriHandler = LocalUriHandler.current
        val missionDeeplinkIsNotEmpty = mission.deeplink.isNotEmpty() || missionEphemerisData.isNotEmpty()
        val size = ButtonDefaults.MediumContainerHeight
        Button(
            onClick = {
                if (mission.deeplink.isNotEmpty()) {
                    uriHandler.openUri(mission.deeplink)
                } else {
                    val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
                    val firstAfter = missionEphemerisData.indexOfFirst { it.timestamp > now }
                    if (firstAfter >= 0) {
                        val deeplink = if (firstAfter == 0) missionEphemerisData.first().deeplink
                            else missionEphemerisData[firstAfter - 1].deeplink
                        uriHandler.openUri(deeplink)
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(size)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
            enabled = missionDeeplinkIsNotEmpty,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
            contentPadding = ButtonDefaults.contentPaddingFor(size)
        ) {
            if (isLoadingEphemerisData) {
                LoadingIndicator(modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)))
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = stringResource(Res.string.mission_open_in_app_icon_description),
                    modifier = Modifier.size(ButtonDefaults.iconSizeFor(size))
                )

                Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))

                Text(stringResource(Res.string.mission_open_in_app), style = ButtonDefaults.textStyleFor(size))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, DelicateCoroutinesApi::class)
@Composable
fun DataMissionPage(
    mission: ScienceMission,
    ephemerisData: List<EphemerisData>
) {
    val missionHasEphemerisArgs = mission.ephemeris_args != null
    LazyColumn (
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (mission.priority) {
            item {
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
                            painter = painterResource(Res.drawable.e911_emergency),
                            contentDescription = stringResource(Res.string.mission_priority_icon_description),
                        )

                        Text(stringResource(Res.string.mission_priority))
                    }
                }
            }
        }

        item {
            Column {
                Text(stringResource(Res.string.mission_type_label, stringResource(mission.getMissionType().label)))
                Text(stringResource(Res.string.mission_target, mission.target_name))
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider()
        }

        item {
            Column {
                Text(stringResource(Res.string.mission_date_and_duration), style = fontStyle_header)

                Spacer(modifier = Modifier.height(8.dp))

                Text(stringResource(Res.string.mission_from, mission.getMissionStartDate()))
                Text(stringResource(Res.string.mission_to, mission.getMissionEndDate()))
                if (!missionHasEphemerisArgs) {
                    Text(stringResource(Res.string.mission_duration, mission.duration))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider()
        }

        if (!missionHasEphemerisArgs) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(stringResource(Res.string.mission_position), style = fontStyle_header)

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(stringResource(Res.string.mission_ra, mission.ra_hms, mission.ra))
                        Text(stringResource(Res.string.mission_dec, mission.dec_dms, mission.dec))
                        Text(stringResource(Res.string.mission_alt_az_direction, mission.alt, mission.az, mission.cardinal_direction))
                        Text(stringResource(Res.string.mission_constellation, mission.constellation))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider()
            }
        } else if (ephemerisData.isNotEmpty()) {
            items(ephemerisData) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(stringResource(Res.string.mission_position_at, it.getParsedDate()), style = fontStyle_header)

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(stringResource(Res.string.mission_ra, it.ra_hms, it.ra))
                        Text(stringResource(Res.string.mission_dec, it.dec_dms, it.dec))
                        Text(stringResource(Res.string.mission_alt_az, it.alt, it.az))
                    }
                }
            }

            item {
                HorizontalDivider()
            }
        }

        item {
            val uriHandler = LocalUriHandler.current
            val missionEventLink = mission.getWebsiteEventLink()
            val size = ButtonDefaults.MediumContainerHeight

            TextButton(
                onClick = {
                    if (missionEventLink != null) {
                        uriHandler.openUri(missionEventLink)
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(size),
                enabled = !missionEventLink.isNullOrEmpty(),
                contentPadding = ButtonDefaults.contentPaddingFor(size)
            ) {
                Text(stringResource(Res.string.mission_event_info), style = ButtonDefaults.textStyleFor(size))
            }
        }

        item {
            Spacer(
                modifier = Modifier
                    .height(128.dp)
                    .navigationBarsPadding()
            )
        }
    }
}

@Composable
fun EmptyMissionPage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(Res.drawable.nebuni),
                contentDescription = stringResource(Res.string.mission_logo_description),
                modifier = Modifier.size(200.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )

            Text(
                stringResource(Res.string.mission_empty),
                textAlign = TextAlign.Center
            )
        }
    }
}