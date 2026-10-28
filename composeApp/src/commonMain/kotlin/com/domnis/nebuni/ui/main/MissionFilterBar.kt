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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.domnis.nebuni.data.ScienceMissionType
import nebuni.composeapp.generated.resources.Res
import nebuni.composeapp.generated.resources.e911_emergency
import nebuni.composeapp.generated.resources.filter_chip_label
import nebuni.composeapp.generated.resources.filter_date_all
import nebuni.composeapp.generated.resources.filter_date_next_3_days
import nebuni.composeapp.generated.resources.filter_date_today
import nebuni.composeapp.generated.resources.filter_priority
import nebuni.composeapp.generated.resources.grouping_date
import nebuni.composeapp.generated.resources.grouping_type
import nebuni.composeapp.generated.resources.mission_type_asteroid_occultation
import nebuni.composeapp.generated.resources.mission_type_cometary_activity
import nebuni.composeapp.generated.resources.mission_type_exoplanet_transit
import nebuni.composeapp.generated.resources.mission_type_planetary_defense
import nebuni.composeapp.generated.resources.mission_type_satellite
import nebuni.composeapp.generated.resources.mission_type_unknown
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Horizontally scrollable row with a priority chip, single-select date range chips,
 * one filter chip per mission type in [availableTypes], and a Date/Type grouping selector.
 */
@Composable
fun MissionFilterBar(
    availableTypes: List<MissionTypeCount>,
    filter: MissionFilter,
    onToggleType: (ScienceMissionType) -> Unit,
    onTogglePriorityOnly: () -> Unit,
    onSelectDateRange: (DateRange) -> Unit,
    grouping: Grouping,
    onSelectGrouping: (Grouping) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item(key = "priority") {
            FilterChip(
                selected = filter.priorityOnly,
                onClick = onTogglePriorityOnly,
                label = { Text(stringResource(Res.string.filter_priority)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.e911_emergency),
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            )
        }

        items(DateRange.entries, key = { "date-${it.name}" }) { dateRange ->
            FilterChip(
                selected = filter.dateRange == dateRange,
                onClick = { onSelectDateRange(dateRange) },
                label = { Text(stringResource(dateRange.labelResource())) }
            )
        }

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

        item(key = "grouping_divider") {
            VerticalDivider(modifier = Modifier.height(24.dp))
        }

        items(Grouping.entries, key = { "grouping_${it.name}" }) { option ->
            FilterChip(
                selected = option == grouping,
                onClick = { onSelectGrouping(option) },
                label = { Text(stringResource(option.labelResource())) }
            )
        }
    }
}

fun DateRange.labelResource(): StringResource = when (this) {
    DateRange.All -> Res.string.filter_date_all
    DateRange.Today -> Res.string.filter_date_today
    DateRange.Next3Days -> Res.string.filter_date_next_3_days
}

fun Grouping.labelResource(): StringResource = when (this) {
    Grouping.Date -> Res.string.grouping_date
    Grouping.Type -> Res.string.grouping_type
}

fun ScienceMissionType.labelResource(): StringResource = when (this) {
    ScienceMissionType.AsteroidOccultation -> Res.string.mission_type_asteroid_occultation
    ScienceMissionType.CometaryActivity -> Res.string.mission_type_cometary_activity
    ScienceMissionType.ExoplanetTransit -> Res.string.mission_type_exoplanet_transit
    ScienceMissionType.PlanetaryDefense -> Res.string.mission_type_planetary_defense
    ScienceMissionType.Satellite -> Res.string.mission_type_satellite
    ScienceMissionType.Unknown -> Res.string.mission_type_unknown
}
