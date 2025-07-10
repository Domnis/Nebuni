package com.domnis.nebuni.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.collections.forEach

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
)

@Serializable
data class CometData(
    val pipeline_type: String,
    val target_name: String
)

@Serializable
data class DefenseData(
    val pipeline_type: String,
    val target_name: String
)

@Serializable
data class TransitData(
    val pipeline_type: String,
    val target_name: String
)

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
            // If parsing fails, store as unknown
            ScienceMission.Unknown(jsonObject.toString())
        }
    }
}