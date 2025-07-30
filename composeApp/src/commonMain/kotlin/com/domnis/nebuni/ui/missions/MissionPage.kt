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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.domnis.nebuni.data.CometData
import com.domnis.nebuni.data.DefenseData
import com.domnis.nebuni.data.OccultationData
import com.domnis.nebuni.data.ScienceMission
import com.domnis.nebuni.data.TransitData
import nebuni.composeapp.generated.resources.Res
import nebuni.composeapp.generated.resources.nebuni
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun MissionPage(
    missionKey: String,
    mission: ScienceMission
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
    ) {

        when (mission) {
            is ScienceMission.Occultation -> OccultationMissionPage(mission.data)
            is ScienceMission.Comet -> CometMissionPage(mission.data)
            is ScienceMission.Defense -> DefenseMissionPage(mission.data)
            is ScienceMission.Transit -> TransitMissionPage(mission.data)
            else -> Spacer(modifier = Modifier.height(12.dp))
        }

        val deepLink = when (mission) {
            is ScienceMission.Occultation -> mission.data.deeplink
            is ScienceMission.Comet -> mission.data.deeplink
            is ScienceMission.Defense -> mission.data.deeplink
            is ScienceMission.Transit -> mission.data.deeplink
            else -> ""
        }

        if (deepLink.isNotEmpty()) {
            val uriHandler = LocalUriHandler.current

            Button(
                onClick = {
                    uriHandler.openUri(deepLink)
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .systemBarsPadding()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Open mission in Unistellar app")
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Open in external app's icon"
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.systemBarsPadding()
            ) {
                Text("Deeplink is not available for this kind of mission...")
            }
        }
    }
}

@Composable
fun OccultationMissionPage(
    data: OccultationData
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Target: ${data.target_name}")

        HorizontalDivider()

        Column {
            Text("Date and duration:")

            Spacer(modifier = Modifier.height(8.dp))

            Text("From: ${data.tstart}")
            Text("To: ${data.tend}")
            Text("Duration: ${data.duration}")
        }

        HorizontalDivider()

        Column {
            Text("Position:")

            Spacer(modifier = Modifier.height(8.dp))

            Text("RA: ${data.ra_hms} (${data.ra})")
            Text("Dec: ${data.dec_dms} (${data.dec})")
            Text("Alt/Az: ${data.alt} / ${data.az} (${data.cardinal_direction})")
            Text("Constellation: ${data.constellation}")
        }

        HorizontalDivider()
    }
}

@Composable
fun CometMissionPage(
    data: CometData
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Target: ${data.target_name}")

        HorizontalDivider()

        Column {
            Text("Date and duration:")

            Spacer(modifier = Modifier.height(8.dp))

            Text("From: ${data.tstart}")
            Text("To: ${data.tend}")
        }

        HorizontalDivider()
    }
}

@Composable
fun DefenseMissionPage(
    data: DefenseData
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Target: ${data.target_name}")

        HorizontalDivider()

        Column {
            Text("Date and duration:")

            Spacer(modifier = Modifier.height(8.dp))

            Text("From: ${data.tstart}")
            Text("To: ${data.tend}")
        }

        HorizontalDivider()
    }
}

@Composable
fun TransitMissionPage(
    data: TransitData
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Target: ${data.target_name}")

        HorizontalDivider()

        Column {
            Text("Date and duration:")

            Spacer(modifier = Modifier.height(8.dp))

            Text("From: ${data.tstart}")
            Text("To: ${data.tend}")
            Text("Duration: ${data.duration}")
        }

        HorizontalDivider()

        Column {
            Text("Position:")

            Spacer(modifier = Modifier.height(8.dp))

            Text("RA: ${data.ra_hms} (${data.ra})")
            Text("Dec: ${data.dec_dms} (${data.dec})")
            Text("Alt/Az: ${data.alt} / ${data.az} (${data.cardinal_direction})")
            Text("Constellation: ${data.constellation}")
        }

        HorizontalDivider()
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

            Text("No mission is currently selected...\nChoose one on the side menu!")
        }
    }
}