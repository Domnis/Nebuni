package com.domnis.nebuni.network

import com.domnis.nebuni.data.ScienceMission
import com.domnis.nebuni.data.SimpleScienceMissionJsonParser
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ScienceAPI {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                useAlternativeNames = false
                encodeDefaults = true
                isLenient = true
                coerceInputValues = true
                allowSpecialFloatingPointValues = true
            })
        }
    }

    suspend fun listScienceMissions(): Map<String, ScienceMission> {
        val response: String = httpClient.submitForm(
            "https://science.unistellar.com/wp-admin/admin-ajax.php",
            formParameters = parameters {
                    append("action", "get-science-events")
                    append("date", "2025-07-10T14:16") // start date?
                    append("pipeline", "o,e,c,p") // type of science events => o = occultation, c = comet, e = exoplanet, p = planetary defense
                    append("lat", "43.268421") // Unistellar HQ lat
                    append("long", "5.395419") // Unistellar HQ long
                    append("tend", "2025-07-22T14:16") // end date for fetch?
                    append("alt", "0,90") // Full Alt possibles
                    append("az", "0,360") // Full Az possibles
                }
        ).body()

        val parser = SimpleScienceMissionJsonParser()

        return parser.parseJson(response)
    }
}