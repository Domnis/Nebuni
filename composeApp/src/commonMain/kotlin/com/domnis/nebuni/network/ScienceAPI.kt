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

import com.domnis.nebuni.data.EphemerisData
import com.domnis.nebuni.data.ObservationPlace
import com.domnis.nebuni.data.ScienceMission
import com.domnis.nebuni.data.SimpleCometEphemeridsJsonParser
import com.domnis.nebuni.data.SimpleScienceMissionJsonParser
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ScienceAPI {
    private val httpClient = HttpClient {
        install(HttpTimeout) {
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 60_000
            requestTimeoutMillis = 70_000
        }

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

    suspend fun listScienceMissions(
        observationPlace: ObservationPlace,
        startDateTime: String,
        endDateTime: String
    ): List<ScienceMission> {
        val response = try {
            httpClient.submitForm(
                "https://science.unistellar.com/wp-admin/admin-ajax.php",
                formParameters = parameters {
                    append("action", "get-science-events")
                    append("date", startDateTime) // start date?
                    append("pipeline", "o,e,c") // type of science events => o = occultation, c = comet, e = exoplanet, p = planetary defense
                    append("lat", "${observationPlace.latitude}")
                    append("long", "${observationPlace.longitude}")
                    append("tend", endDateTime) // end date for fetch?
                    append("alt", "${observationPlace.altMin},${observationPlace.altMax}") // Full Alt possibles
                    append("az", "${observationPlace.azMin},${observationPlace.azMax}") // Full Az possibles
                }
            )
        } catch(e: Exception) {
            e.printStackTrace()
            null
        }

        if (response == null || response.status != HttpStatusCode.OK) {
            return emptyList()
        }

        val parser = SimpleScienceMissionJsonParser()

        return parser.parseJson(response.body(), observationPlace.id)
    }

    suspend fun getCometMissionsEphemeris(
        scienceMission: ScienceMission
    ): List<EphemerisData> {
        val args = scienceMission.ephemeris_args
        if (args == null) {
            return emptyList()
        }

        val response = try {
            httpClient.submitForm(
                "https://science.unistellar.com/wp-admin/admin-ajax.php",
                formParameters = parameters {
                    append("action", "get-ephemerid")
                    append("name", args.name)
                    append("date", args.tstart)
                    append("lat", args.loc.split(",")[0])
                    append("lng", args.loc.split(",")[1])
                    append("step", "10")
                    append("duration", args.duration.split(".")[0])
                    append("et", args.exp_time.split(".")[0])
                    append("gain", args.gain.split(".")[0])
                    append("is_comet", "true")
                }
            )
        } catch(e: Exception) {
            e.printStackTrace()
            null
        }

        if (response == null || response.status != HttpStatusCode.OK) {
            print("comet eph no response or bad response: ${response?.status}")
            return emptyList()
        }

        val parser = SimpleCometEphemeridsJsonParser()

        return parser.parseJson(
            response.body(),
            scienceMission.missionKey,
            scienceMission.observationPlaceID
        )
    }
}