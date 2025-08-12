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
import com.domnis.nebuni.parseDateToInstant
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
sealed class ScienceMission(open val missionKey: String) {
    @Serializable
    data class Occultation(val key: String, val data: OccultationData) : ScienceMission(key)

    @Serializable
    data class Comet(val key: String, val data: CometData) : ScienceMission(key)

    @Serializable
    data class Defense(val key: String, val data: DefenseData) : ScienceMission(key)

    @Serializable
    data class Transit(val key: String, val data: TransitData) : ScienceMission(key)

    @Serializable
    data class Unknown(val key: String, val content: String) : ScienceMission(key)

    fun getMissionName() : String {
        return when (this) {
            is Occultation -> this.data.target_name
            is Comet -> this.data.target_name
            is Defense -> this.data.target_name
            is Transit -> this.data.target_name
            else -> this.missionKey
        }
    }

    fun isPriority(): Boolean {
        return when (this) {
            is Occultation -> this.data.priority
            is Comet -> this.data.priority
            is Defense -> this.data.priority
            is Transit -> this.data.priority
            else -> false
        }
    }

    fun getMissionStartDate() : String {
        return when (this) {
            is Occultation -> this.data.getStartDateTimeInLocalTimeZone()
            is Comet -> this.data.tstart
            is Defense -> this.data.getStartDateTimeInLocalTimeZone()
            is Transit -> this.data.getStartDateTimeInLocalTimeZone()
            else -> ""
        }
    }

    fun getMissionStartTimestamp(): Long {
        return when (this) {
            is Occultation -> this.data.getStartTimestamp()
            is Comet -> this.data.getStartTimestamp()
            is Defense -> this.data.getStartTimestamp()
            is Transit -> this.data.getStartTimestamp()
            else -> Long.MAX_VALUE
        }
    }
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

    fun getStartTimestamp(): Long {
        return Instant.parse(tstart, customInstantParseFormat()).toEpochMilliseconds()
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

    fun getStartTimestamp(): Long {
        if (!tstart.contains('T', true))
            return parseDateToInstant(tstart, TimeZone.currentSystemDefault()).toEpochMilliseconds()

        return Instant.parse(tstart, customInstantParseFormat()).toEpochMilliseconds()
    }
}

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

    fun getStartTimestamp(): Long {
        if (!tstart.contains('T', true))
            return parseDateToInstant(tstart, TimeZone.currentSystemDefault()).toEpochMilliseconds()

        return Instant.parse(tstart, customInstantParseFormat()).toEpochMilliseconds()
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

    fun getStartTimestamp(): Long {
        return Instant.parse(tstart, customInstantParseFormat()).toEpochMilliseconds()
    }
}

class SimpleScienceMissionJsonParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parseJson(jsonString: String): List<ScienceMission> {
        // Parse as generic JsonObject first
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        val result = mutableListOf<ScienceMission>()

        jsonObject.forEach { (key, value) ->
            if (key != "query") {
                val content = determineTypeAndParse(key, value.jsonObject)
//                result[key] = content
                result.add(content)
            }
        }

        return result
    }

    private fun determineTypeAndParse(key: String, jsonObject: JsonObject): ScienceMission {
        return try {
            when {
                // Check if key contains some wording
                key.contains("_occultation_") -> {
                    ScienceMission.Occultation(key, json.decodeFromJsonElement<OccultationData>(jsonObject))
                }
                key.contains("_comet_") -> {
                    ScienceMission.Comet(key, json.decodeFromJsonElement<CometData>(jsonObject))
                }
                key.contains("_transit_") -> {
                    ScienceMission.Transit(key, json.decodeFromJsonElement<TransitData>(jsonObject))
                }
                key.contains("_defense_") -> {
                    ScienceMission.Defense(key, json.decodeFromJsonElement<DefenseData>(jsonObject))
                }
                else -> {
                    ScienceMission.Unknown(key, jsonObject.toString())
                }
            }
        } catch (e: SerializationException) {
            e.printStackTrace()
            // If parsing fails, store as unknown
            ScienceMission.Unknown(key, jsonObject.toString())
        }
    }
}