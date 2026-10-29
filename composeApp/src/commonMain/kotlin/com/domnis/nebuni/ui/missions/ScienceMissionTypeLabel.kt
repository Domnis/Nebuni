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

import com.domnis.nebuni.data.ScienceMissionType
import nebuni.composeapp.generated.resources.Res
import nebuni.composeapp.generated.resources.mission_type_asteroid_occultation
import nebuni.composeapp.generated.resources.mission_type_cometary_activity
import nebuni.composeapp.generated.resources.mission_type_exoplanet_transit
import nebuni.composeapp.generated.resources.mission_type_planetary_defense
import nebuni.composeapp.generated.resources.mission_type_satellite
import nebuni.composeapp.generated.resources.mission_type_unknown
import org.jetbrains.compose.resources.StringResource

/** User-facing name of a mission type, resolved with `stringResource`. */
val ScienceMissionType.label: StringResource
    get() = when (this) {
        ScienceMissionType.AsteroidOccultation -> Res.string.mission_type_asteroid_occultation
        ScienceMissionType.CometaryActivity -> Res.string.mission_type_cometary_activity
        ScienceMissionType.ExoplanetTransit -> Res.string.mission_type_exoplanet_transit
        ScienceMissionType.PlanetaryDefense -> Res.string.mission_type_planetary_defense
        ScienceMissionType.Satellite -> Res.string.mission_type_satellite
        ScienceMissionType.Unknown -> Res.string.mission_type_unknown
    }
