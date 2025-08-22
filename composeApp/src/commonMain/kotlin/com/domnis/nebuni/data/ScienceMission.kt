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

package com.domnis.nebuni.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import com.domnis.nebuni.customDisplayDateTimeFormat
import com.domnis.nebuni.customInstantParseFormat
import com.domnis.nebuni.parseDateToInstant
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

enum class ScienceMissionType(val displayName: String) {
    AsteroidOccultation("Asteroid occultations"),
    CometaryActivity("Cometary activity"),
    ExoplanetTransit("Exoplanet transits"),
    PlanetaryDefense("Planetary defense"),
    Satellite("Satellite"),
    Unknown("Unknown")
}

@Serializable
@Entity(primaryKeys = ["missionKey", "observationPlaceID"])
data class ScienceMission(
    val missionKey: String = "",
    @ColumnInfo(defaultValue = "") val observationPlaceID: String = "",
    val pipeline_type: String = "",
    val target_name: String = "",
    val target_number: String = "",
    val orbit_type: String = "",
    val ra: String = "",
    val dec: String = "",
    val ra_hms: String = "",
    val dec_dms: String = "",
    val alt: Int = 0,
    val az: Int = 0,
    val cardinal_direction: String = "",
    val constellation: String = "",
    val kml_url: String = "",
    val deeplink: String = "",
    val duration: String = "",
    val et: Int = 0,
    val gain: Int = 0,
    val cadence: Int = 0,
    val priority: Boolean = false,
    val tstart: String = "",
    val tend: String = "",
    val ephemeris_url: String = "",
    @Embedded(prefix = "eph_args_") val ephemeris_args: EphemerisArgs? = null,
    val category: String = ""
) {
    fun getMissionType(): ScienceMissionType {
        return when(pipeline_type) {
            "o" -> ScienceMissionType.AsteroidOccultation
            "e" -> ScienceMissionType.ExoplanetTransit
            "c" -> ScienceMissionType.CometaryActivity
            "p" -> ScienceMissionType.PlanetaryDefense
            "s" -> ScienceMissionType.Satellite
            else -> ScienceMissionType.Unknown
        }
    }

    fun getMissionStartDate() : String {
        if (!tstart.contains('T', true)) return tstart

        return Instant.parse(tstart, customInstantParseFormat())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(customDisplayDateTimeFormat())
    }

    fun getMissionEndDate() : String {
        if (!tend.contains('T', true)) return tend

        return Instant.parse(tend, customInstantParseFormat())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(customDisplayDateTimeFormat())
    }

    fun getMissionStartTimestamp(): Long {
        if (!tstart.contains('T', true))
            return parseDateToInstant(tstart, TimeZone.currentSystemDefault()).toEpochMilliseconds()

        return Instant.parse(tstart, customInstantParseFormat()).toEpochMilliseconds()
    }
}

@Serializable
data class EphemerisArgs(
    val name: String = "",
    val loc: String = "",
    val tstart: String = "",
    val auto_step: String = "",
    val duration: String = "",
    val gain: String = "",
    val exp_time: String = "",
    val is_comet: String? = "false"
)

class SimpleScienceMissionJsonParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parseJson(jsonString: String, observationPlaceID: String): List<ScienceMission> {
        // Parse as generic JsonObject first
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        val result = mutableListOf<ScienceMission>()

        jsonObject.forEach { (key, value) ->
            if (key != "query") {
                val content = determineTypeAndParse(observationPlaceID, key, value.jsonObject)
                result.add(content)
            }
        }

        return result
    }

    private fun determineTypeAndParse(observationPlaceID: String, key: String, jsonObject: JsonObject): ScienceMission {
        return try {
            json.decodeFromJsonElement<ScienceMission>(jsonObject).copy(missionKey = key, observationPlaceID = observationPlaceID)
        } catch (e: Exception) {
            e.printStackTrace()
            ScienceMission(
                missionKey = key,
                observationPlaceID = observationPlaceID
            )
        }
    }
}