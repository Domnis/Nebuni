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

package com.domnis.nebuni.network

import com.domnis.nebuni.data.ObservationPlace
import com.domnis.nebuni.data.ScienceMission
import com.domnis.nebuni.data.SimpleScienceMissionJsonParser
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.http.HttpStatusCode
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

    suspend fun listScienceMissions(observationPlace: ObservationPlace): Map<String, ScienceMission> {
        val response = try {
            httpClient.submitForm(
                "https://science.unistellar.com/wp-admin/admin-ajax.php",
                formParameters = parameters {
                    append("action", "get-science-events")
                    append("date", "2025-07-10T14:16") // start date?
                    append("pipeline", "o,e,c,p") // type of science events => o = occultation, c = comet, e = exoplanet, p = planetary defense
                    append("lat", "${observationPlace.latitude}")
                    append("long", "${observationPlace.longitude}")
                    append("tend", "2025-07-22T14:16") // end date for fetch?
                    append("alt", "${observationPlace.altMin},${observationPlace.altMax}") // Full Alt possibles
                    append("az", "${observationPlace.azMin},${observationPlace.azMax}") // Full Az possibles
                }
            )
        } catch(e: Exception) {
            e.printStackTrace()
            null
        }

        if (response == null || response.status != HttpStatusCode.OK) {
            return emptyMap()
        }

        val parser = SimpleScienceMissionJsonParser()

        return parser.parseJson(response.body())
    }
}