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

import com.domnis.nebuni.data.ObservationPlace
import nebuni.composeapp.generated.resources.Res
import nebuni.composeapp.generated.resources.error_altitude_max_format
import nebuni.composeapp.generated.resources.error_altitude_max_range
import nebuni.composeapp.generated.resources.error_altitude_min_format
import nebuni.composeapp.generated.resources.error_altitude_min_range
import nebuni.composeapp.generated.resources.error_azimuth_max_format
import nebuni.composeapp.generated.resources.error_azimuth_max_range
import nebuni.composeapp.generated.resources.error_azimuth_min_format
import nebuni.composeapp.generated.resources.error_azimuth_min_range
import nebuni.composeapp.generated.resources.error_latitude_format
import nebuni.composeapp.generated.resources.error_latitude_range
import nebuni.composeapp.generated.resources.error_longitude_format
import nebuni.composeapp.generated.resources.error_longitude_range
import org.jetbrains.compose.resources.StringResource

enum class ObservationPlaceInputError {
    LatitudeFormat,
    LatitudeRange,
    LongitudeFormat,
    LongitudeRange,
    AltitudeMinFormat,
    AltitudeMaxFormat,
    AltitudeMinRange,
    AltitudeMaxRange,
    AzimuthMinFormat,
    AzimuthMaxFormat,
    AzimuthMinRange,
    AzimuthMaxRange,
}

sealed interface ObservationPlaceInputResult {
    data class Valid(val place: ObservationPlace) : ObservationPlaceInputResult
    data class Invalid(val error: ObservationPlaceInputError) : ObservationPlaceInputResult
}

/**
 * Validates the raw text of the observation place form.
 *
 * Fields are trimmed and decimal commas are accepted. Checks run in form order and the first
 * failing rule is returned.
 */
fun validateObservationPlaceInput(
    name: String,
    latitude: String,
    longitude: String,
    altMin: String,
    altMax: String,
    azMin: String,
    azMax: String,
): ObservationPlaceInputResult {
    fun invalid(error: ObservationPlaceInputError) = ObservationPlaceInputResult.Invalid(error)

    val lat = latitude.normalized().toDoubleOrNull()
        ?: return invalid(ObservationPlaceInputError.LatitudeFormat)
    if (lat < -90.0 || lat > 90.0) return invalid(ObservationPlaceInputError.LatitudeRange)

    val lon = longitude.normalized().toDoubleOrNull()
        ?: return invalid(ObservationPlaceInputError.LongitudeFormat)
    if (lon < -180.0 || lon > 180.0) return invalid(ObservationPlaceInputError.LongitudeRange)

    val altMinValue = altMin.normalized().toIntOrNull()
        ?: return invalid(ObservationPlaceInputError.AltitudeMinFormat)
    val altMaxValue = altMax.normalized().toIntOrNull()
        ?: return invalid(ObservationPlaceInputError.AltitudeMaxFormat)
    if (altMinValue < 0 || altMinValue > 90) return invalid(ObservationPlaceInputError.AltitudeMinRange)
    if (altMaxValue < 0 || altMaxValue > 90 || altMaxValue < altMinValue) {
        return invalid(ObservationPlaceInputError.AltitudeMaxRange)
    }

    val azMinValue = azMin.normalized().toIntOrNull()
        ?: return invalid(ObservationPlaceInputError.AzimuthMinFormat)
    val azMaxValue = azMax.normalized().toIntOrNull()
        ?: return invalid(ObservationPlaceInputError.AzimuthMaxFormat)
    if (azMinValue < 0 || azMinValue > 360) return invalid(ObservationPlaceInputError.AzimuthMinRange)
    if (azMaxValue < 0 || azMaxValue > 360 || azMaxValue < azMinValue) {
        return invalid(ObservationPlaceInputError.AzimuthMaxRange)
    }

    return ObservationPlaceInputResult.Valid(
        ObservationPlace(
            name = name.trim(),
            latitude = lat,
            longitude = lon,
            altMin = altMinValue,
            altMax = altMaxValue,
            azMin = azMinValue,
            azMax = azMaxValue
        )
    )
}

val ObservationPlaceInputError.message: StringResource
    get() = when (this) {
        ObservationPlaceInputError.LatitudeFormat -> Res.string.error_latitude_format
        ObservationPlaceInputError.LatitudeRange -> Res.string.error_latitude_range
        ObservationPlaceInputError.LongitudeFormat -> Res.string.error_longitude_format
        ObservationPlaceInputError.LongitudeRange -> Res.string.error_longitude_range
        ObservationPlaceInputError.AltitudeMinFormat -> Res.string.error_altitude_min_format
        ObservationPlaceInputError.AltitudeMaxFormat -> Res.string.error_altitude_max_format
        ObservationPlaceInputError.AltitudeMinRange -> Res.string.error_altitude_min_range
        ObservationPlaceInputError.AltitudeMaxRange -> Res.string.error_altitude_max_range
        ObservationPlaceInputError.AzimuthMinFormat -> Res.string.error_azimuth_min_format
        ObservationPlaceInputError.AzimuthMaxFormat -> Res.string.error_azimuth_max_format
        ObservationPlaceInputError.AzimuthMinRange -> Res.string.error_azimuth_min_range
        ObservationPlaceInputError.AzimuthMaxRange -> Res.string.error_azimuth_max_range
    }

private fun String.normalized() = trim().replace(",", ".")
