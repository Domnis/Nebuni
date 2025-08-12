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

import com.domnis.nebuni.customDisplayDateTimeFormat
import com.domnis.nebuni.customInstantParseFormat
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
sealed class ScienceMission {
    @Serializable
    data class Occultation(val data: OccultationData) : ScienceMission()

    @Serializable
    data class Comet(val data: CometData) : ScienceMission()

    @Serializable
    data class Defense(val data: DefenseData) : ScienceMission()

    @Serializable
    data class Transit(val data: TransitData) : ScienceMission()

    @Serializable
    data class Unknown(val content: String) : ScienceMission()
}

@Serializable
data class OccultationData(
    val pipeline_type: String,
    val target_name: String,
    val target_number: Int,
    val orbit_type: String,
    val ra: String,
    val dec: String,
    val ra_hms: String,
    val dec_dms: String,
    val alt: Int,
    val az: Int,
    val cardinal_direction: String,
    val constellation: String,
    val kml_url: String,
    val deeplink: String,
    val duration: String,
    val et: Int,
    val gain: Int,
    val priority: Boolean,
    val tstart: String,
    val tend: String
) {
    fun getStartDateTimeInLocalTimeZone(): String {
        return Instant.parse(tstart, customInstantParseFormat())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(customDisplayDateTimeFormat())
    }

    fun getEndDateTimeInLocalTimeZone(): String {
        return Instant.parse(tend, customInstantParseFormat())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(customDisplayDateTimeFormat())
    }
}

@Serializable
data class CometData(
    val pipeline_type: String,
    val target_name: String,
    val deeplink: String = "", //in fact, does not exist here...
    val tstart: String,
    val tend: String,
    val priority: Boolean,
    val ephemeris_url: String,
    val ephemeris_args: EphemerisArgs
) // no parsing for comet as date and time format is a bit different there.
// Seems like tstart and tend are in year-month-day format
// and define a range of multiple month of visibility.

@Serializable
data class DefenseData(
    val pipeline_type: String,
    val target_name: String,
    val deeplink: String = "", //in fact, does not exist here...
    val target_number: String,
    val orbit_type: String,
    val tstart: String,
    val tend: String,
    val priority: Boolean,
    val ephemeris_url: String,
    val ephemeris_args: EphemerisArgs
) {
    fun getStartDateTimeInLocalTimeZone(): String {
        if (!tstart.contains('T', true)) return tstart

        return Instant.parse(tstart, customInstantParseFormat())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(customDisplayDateTimeFormat())
    }

    fun getEndDateTimeInLocalTimeZone(): String {
        if (!tend.contains('T', true)) return tend

        return Instant.parse(tend, customInstantParseFormat())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(customDisplayDateTimeFormat())
    }
}

@Serializable
data class EphemerisArgs(
    val name: String,
    val loc: String,
    val tstart: String,
    val auto_step: String,
    val duration: String,
    val gain: String,
    val exp_time: String,
    val is_comet: String? = "false"
)

@Serializable
data class TransitData(
    val pipeline_type: String,
    val target_name: String,
    val deeplink: String,
    val ra: String,
    val dec: String,
    val ra_hms: String,
    val dec_dms: String,
    val alt: Int,
    val az: Int,
    val cardinal_direction: String,
    val constellation: String,
    val kml_url: String,
    val duration: String,
    val et: Int,
    val gain: Int,
    val cadence: Int,
    val priority: Boolean,
    val tstart: String,
    val tend: String,
    val category: String
) {
    fun getStartDateTimeInLocalTimeZone(): String {
        return Instant.parse(tstart, customInstantParseFormat())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(customDisplayDateTimeFormat())
    }

    fun getEndDateTimeInLocalTimeZone(): String {
        return Instant.parse(tend, customInstantParseFormat())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(customDisplayDateTimeFormat())
    }
}

class SimpleScienceMissionJsonParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parseJson(jsonString: String): Map<String, ScienceMission> {
        // Parse as generic JsonObject first
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        val result = mutableMapOf<String, ScienceMission>()

        jsonObject.forEach { (key, value) ->
            if (key != "query") {
                val content = determineTypeAndParse(key, value.jsonObject)
                result[key] = content
            }
        }

        return result
    }

    private fun determineTypeAndParse(key: String, jsonObject: JsonObject): ScienceMission {
        return try {
            when {
                // Check if key contains some wording
                key.contains("_occultation_") -> {
                    ScienceMission.Occultation(json.decodeFromJsonElement<OccultationData>(jsonObject))
                }
                key.contains("_comet_") -> {
                    ScienceMission.Comet(json.decodeFromJsonElement<CometData>(jsonObject))
                }
                key.contains("_transit_") -> {
                    ScienceMission.Transit(json.decodeFromJsonElement<TransitData>(jsonObject))
                }
                key.contains("_defense_") -> {
                    ScienceMission.Defense(json.decodeFromJsonElement<DefenseData>(jsonObject))
                }
                else -> {
                    ScienceMission.Unknown(jsonObject.toString())
                }
            }
        } catch (e: SerializationException) {
            e.printStackTrace()
            // If parsing fails, store as unknown
            ScienceMission.Unknown(jsonObject.toString())
        }
    }
}