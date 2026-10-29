package com.domnis.nebuni.network

import com.domnis.nebuni.data.EphemerisArgs
import com.domnis.nebuni.data.ObservationPlace
import com.domnis.nebuni.data.ScienceMission
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ScienceAPITest {
    private val place = ObservationPlace(id = "place-1")
    private val mission = ScienceMission(
        missionKey = "mission-1",
        observationPlaceID = "place-1",
        ephemeris_args = EphemerisArgs(
            name = "C/2025 A1",
            loc = "43.28,5.37",
            tstart = "2025-01-01T00:00:00Z",
            duration = "120.0",
            gain = "20.0",
            exp_time = "500.0"
        )
    )

    private val missionsJson = """
        {
          "query": {"foo": "bar"},
          "m1": {"pipeline_type": "c", "target_name": "Comet", "alt": 45, "unknown_field": 1},
          "m2": {"pipeline_type": "p", "target_name": "Asteroid"}
        }
    """.trimIndent()

    private val ephemerisJson = """
        {
          "loc": "43.28,5.37",
          "2025-01-01T00:00:00": {"ra": "1", "dec": "2", "alt": 30, "az": 90, "is_visible": true},
          "2025-01-01T00:10:00": {"ra": "3", "dec": "4", "alt": 31, "az": 91, "is_visible": false}
        }
    """.trimIndent()

    // Invalid JSON, then valid JSON that is not an object (e.g. admin-ajax's "0").
    private val malformedBodies = listOf("not json", "<html>", "oops", "0", "[]")

    private fun api(handler: MockRequestHandler) = ScienceAPI(HttpClient(MockEngine(handler)))

    private fun MockRequestHandleScope.respondJson(body: String) =
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun json(body: String): MockRequestHandler = { respondJson(body) }

    @Test
    fun listScienceMissions_parsesNominalResponse() = runTest {
        var sentBody = ""
        val result = api { request ->
            sentBody = request.body.toByteArray().decodeToString()
            respondJson(missionsJson)
        }.listScienceMissions(place, "2025-01-01", "2025-01-02")

        assertEquals(listOf("m1", "m2"), result.map { it.missionKey })
        assertTrue(result.all { it.observationPlaceID == "place-1" })
        assertEquals(45, result[0].alt)
        assertTrue(sentBody.contains("action=get-science-events"))
    }

    @Test
    fun listScienceMissions_emptyObjectGivesEmptyList() = runTest {
        assertEquals(emptyList(), api(json("{}")).listScienceMissions(place, "a", "b"))
    }

    @Test
    fun listScienceMissions_httpErrorGivesEmptyList() = runTest {
        val result = api { respondError(HttpStatusCode.InternalServerError) }
            .listScienceMissions(place, "a", "b")
        assertEquals(emptyList(), result)
    }

    @Test
    fun listScienceMissions_networkExceptionGivesEmptyList() = runTest {
        val result = api { throw IllegalStateException("offline") }
            .listScienceMissions(place, "a", "b")
        assertEquals(emptyList(), result)
    }

    @Test
    fun listScienceMissions_malformedBodyGivesEmptyList() = runTest {
        for (body in malformedBodies) {
            assertEquals(emptyList(), api(json(body)).listScienceMissions(place, "a", "b"), body)
        }
    }

    @Test
    fun listScienceMissions_skipsNonObjectEntries() = runTest {
        val body = """
            {
              "query": {"foo": "bar"},
              "m1": {"pipeline_type": "c", "target_name": "Comet"},
              "broken": "not an object",
              "list": [1, 2],
              "m2": {"pipeline_type": "p", "target_name": "Asteroid"}
            }
        """.trimIndent()
        val result = api(json(body)).listScienceMissions(place, "a", "b")
        assertEquals(listOf("m1", "m2"), result.map { it.missionKey })
    }

    @Test
    fun listScienceMissions_cancellationIsRethrown() = runTest {
        assertFailsWith<CancellationException> {
            api { throw CancellationException("cancelled") }.listScienceMissions(place, "a", "b")
        }
    }

    @Test
    fun getCometMissionsEphemeris_parsesNominalResponse() = runTest {
        var sentBody = ""
        val result = api { request ->
            sentBody = request.body.toByteArray().decodeToString()
            respondJson(ephemerisJson)
        }.getCometMissionsEphemeris(mission, "")

        assertEquals(2, result.size)
        assertTrue(result.all { it.missionKey == "mission-1" && it.observationPlaceID == "place-1" })
        assertTrue(result[0].timestamp < result[1].timestamp)
        assertTrue(sentBody.contains("is_comet=true"))
        assertTrue(sentBody.contains("step=10"))
        assertTrue(sentBody.contains("duration=120"))
    }

    @Test
    fun getCometMissionsEphemeris_emptyResponseAndMissingArgs() = runTest {
        assertEquals(emptyList(), api(json("{}")).getCometMissionsEphemeris(mission, ""))
        val noArgs = mission.copy(ephemeris_args = null)
        assertEquals(emptyList(), api(json(ephemerisJson)).getCometMissionsEphemeris(noArgs, ""))
    }

    @Test
    fun getCometMissionsEphemeris_httpErrorGivesEmptyList() = runTest {
        val result = api { respondError(HttpStatusCode.NotFound) }.getCometMissionsEphemeris(mission, "")
        assertEquals(emptyList(), result)
    }

    @Test
    fun getCometMissionsEphemeris_malformedBodyGivesEmptyList() = runTest {
        for (body in malformedBodies) {
            assertEquals(emptyList(), api(json(body)).getCometMissionsEphemeris(mission, ""), body)
        }
    }

    @Test
    fun getCometMissionsEphemeris_cancellationIsRethrown() = runTest {
        assertFailsWith<CancellationException> {
            api { throw CancellationException("cancelled") }.getCometMissionsEphemeris(mission, "")
        }
    }

    @Test
    fun getPlanetaryDefenseMissionsEphemeris_parsesNominalResponse() = runTest {
        var sentBody = ""
        val result = api { request ->
            sentBody = request.body.toByteArray().decodeToString()
            respondJson(ephemerisJson)
        }.getPlanetaryDefenseMissionsEphemeris(mission, "2025-02-01T00:00:00Z")

        assertEquals(2, result.size)
        assertTrue(sentBody.contains("is_comet=false"))
        assertTrue(sentBody.contains("step=60"))
    }

    @Test
    fun getPlanetaryDefenseMissionsEphemeris_emptyResponseAndMissingArgs() = runTest {
        assertEquals(emptyList(), api(json("{}")).getPlanetaryDefenseMissionsEphemeris(mission, ""))
        val noArgs = mission.copy(ephemeris_args = null)
        assertEquals(emptyList(), api(json(ephemerisJson)).getPlanetaryDefenseMissionsEphemeris(noArgs, ""))
    }

    @Test
    fun getPlanetaryDefenseMissionsEphemeris_httpErrorGivesEmptyList() = runTest {
        val result = api { respondError(HttpStatusCode.BadGateway) }
            .getPlanetaryDefenseMissionsEphemeris(mission, "")
        assertEquals(emptyList(), result)
    }

    @Test
    fun getPlanetaryDefenseMissionsEphemeris_malformedBodyGivesEmptyList() = runTest {
        for (body in malformedBodies) {
            assertEquals(emptyList(), api(json(body)).getPlanetaryDefenseMissionsEphemeris(mission, ""), body)
        }
    }

    @Test
    fun getPlanetaryDefenseMissionsEphemeris_cancellationIsRethrown() = runTest {
        assertFailsWith<CancellationException> {
            api { throw CancellationException("cancelled") }.getPlanetaryDefenseMissionsEphemeris(mission, "")
        }
    }
}
