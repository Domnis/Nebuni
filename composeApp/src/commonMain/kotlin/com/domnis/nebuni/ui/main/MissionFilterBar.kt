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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.domnis.nebuni.data.ScienceMissionType
import nebuni.composeapp.generated.resources.Res
import nebuni.composeapp.generated.resources.filter_chip_label
import nebuni.composeapp.generated.resources.mission_type_asteroid_occultation
import nebuni.composeapp.generated.resources.mission_type_cometary_activity
import nebuni.composeapp.generated.resources.mission_type_exoplanet_transit
import nebuni.composeapp.generated.resources.mission_type_planetary_defense
import nebuni.composeapp.generated.resources.mission_type_satellite
import nebuni.composeapp.generated.resources.mission_type_unknown
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Horizontally scrollable row with one filter chip per mission type in [availableTypes].
 */
@Composable
fun MissionFilterBar(
    availableTypes: List<MissionTypeCount>,
    filter: MissionFilter,
    onToggleType: (ScienceMissionType) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(availableTypes, key = { it.type.name }) { typeCount ->
            FilterChip(
                selected = typeCount.type in filter.types,
                onClick = { onToggleType(typeCount.type) },
                label = {
                    Text(
                        stringResource(
                            Res.string.filter_chip_label,
                            stringResource(typeCount.type.labelResource()),
                            typeCount.count
                        )
                    )
                }
            )
        }
    }
}

fun ScienceMissionType.labelResource(): StringResource = when (this) {
    ScienceMissionType.AsteroidOccultation -> Res.string.mission_type_asteroid_occultation
    ScienceMissionType.CometaryActivity -> Res.string.mission_type_cometary_activity
    ScienceMissionType.ExoplanetTransit -> Res.string.mission_type_exoplanet_transit
    ScienceMissionType.PlanetaryDefense -> Res.string.mission_type_planetary_defense
    ScienceMissionType.Satellite -> Res.string.mission_type_satellite
    ScienceMissionType.Unknown -> Res.string.mission_type_unknown
}
