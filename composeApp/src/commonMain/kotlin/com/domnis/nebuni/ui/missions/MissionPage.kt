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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.domnis.nebuni.data.ScienceMission
import com.domnis.nebuni.data.ScienceMissionType
import com.domnis.nebuni.ui.theme.fontStyle_header
import nebuni.composeapp.generated.resources.Res
import nebuni.composeapp.generated.resources.e911_emergency
import nebuni.composeapp.generated.resources.nebuni
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
        if (mission.getMissionType() == ScienceMissionType.Unknown) {
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            DataMissionPage(mission)
        }

        val uriHandler = LocalUriHandler.current
        val missionDeeplinkIsNotEmpty = mission.deeplink.isNotEmpty()
        val size = ButtonDefaults.MediumContainerHeight
        Button(
            onClick = {
                uriHandler.openUri(mission.deeplink)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(size)
                .padding(bottom = 16.dp),
            enabled = missionDeeplinkIsNotEmpty,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
            contentPadding = ButtonDefaults.contentPaddingFor(size)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Open in external app's icon",
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(size))
            )

            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))

            Text("Open in Unistellar app", style = ButtonDefaults.textStyleFor(size))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DataMissionPage(
    mission: ScienceMission,
) {
    val missionHasEphemerisArgs = mission.ephemeris_args != null
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (mission.priority) {
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
                        contentDescription = "A priority icon",
                    )

                    Text("This mission is a priority!")
                }
            }
        }

        Column {
            Text("Mission type: ${mission.getMissionType().displayName}")
            Text("Target: ${mission.target_name}")
        }

        HorizontalDivider()

        Column {
            Text("Date and duration:", style = fontStyle_header)

            Spacer(modifier = Modifier.height(8.dp))

            Text("From: ${mission.getMissionStartDate()}")
            Text("To: ${mission.getMissionEndDate()}")
            if (!missionHasEphemerisArgs) {
                Text("Duration: ${mission.duration}")
            }
        }

        HorizontalDivider()

        if (!missionHasEphemerisArgs) {
            Column {
                Text("Position:", style = fontStyle_header)

                Spacer(modifier = Modifier.height(8.dp))

                Text("RA: ${mission.ra_hms} (${mission.ra})")
                Text("Dec: ${mission.dec_dms} (${mission.dec})")
                Text("Alt/Az: ${mission.alt}° / ${mission.az}° (${mission.cardinal_direction})")
                Text("Constellation: ${mission.constellation}")
            }

            HorizontalDivider()
        }

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
            Text("Check event info on web site", style = ButtonDefaults.textStyleFor(size))
        }

        Spacer(
            modifier = Modifier
                .height(128.dp)
                .navigationBarsPadding()
        )
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
                contentDescription = "Nebuni's logo",
                modifier = Modifier.size(200.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )

            Text(
                "No mission is currently selected...\nChoose one on the side menu!",
                textAlign = TextAlign.Center
            )
        }
    }
}