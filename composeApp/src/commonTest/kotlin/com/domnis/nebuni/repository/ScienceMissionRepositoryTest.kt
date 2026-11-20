package com.domnis.nebuni.repository

import com.domnis.nebuni.data.EphemerisArgs
import com.domnis.nebuni.data.EphemerisData
import com.domnis.nebuni.data.ObservationPlace
import com.domnis.nebuni.data.ScienceMission
import com.domnis.nebuni.network.ScienceAPI
import com.domnis.nebuni.network.ScienceApiFailure
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ScienceMissionRepositoryTest {
    private val place = ObservationPlace(id = "place-1")
    private val ephemerisArgs = EphemerisArgs(
        name = "C/2025 A1",
        loc = "43.28,5.37",
        tstart = "2025-01-01T00:00:00Z",
        duration = "120.0",
        gain = "20.0",
        exp_time = "500.0"
    )

    private val missionsJson = """
        {
          "m1": {"pipeline_type": "c", "target_name": "Comet"},
          "m2": {"pipeline_type": "o", "target_name": "Occultation"},
          "m3": {"pipeline_type": "zzz", "target_name": "Unknown"}
        }
    """.trimIndent()

    private val ephemerisJson = """
        {
          "loc": "43.28,5.37",
          "2025-01-01T00:00:00": {"ra": "1", "dec": "2", "alt": 30, "az": 90, "is_visible": true}
        }
    """.trimIndent()

    private val missionDao = FakeScienceMissionDao()
    private val ephemerisDao = FakeEphemerisDataDao()
    private val requests = mutableListOf<String>()

    private fun TestScope.repository(missions: String = missionsJson) = ScienceMissionRepository(
        scienceMissionDao = missionDao,
        ephemerisDataDao = ephemerisDao,
        scienceAPI = ScienceAPI(HttpClient(MockEngine { request ->
            val body = request.body.toByteArray().decodeToString()
            requests += body
            val json = if (body.contains("action=get-science-events")) missions else ephemerisJson
            respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })),
        ioDispatcher = StandardTestDispatcher(testScheduler)
    )

    @Test
    fun refreshScienceMissions_storesKnownMissionsThroughInjectedDao() = runTest {
        val repository = repository()

        val outcome = repository.refreshScienceMissions(place, "start", "end", canCleanDatabase = true)

        assertEquals(MissionRefreshOutcome.Success(2), outcome)
        val stored = repository.getScienceMissionFor(place).first()
        assertEquals(setOf("m1", "m2"), stored.map { it.missionKey }.toSet())
    }

    @Test
    fun refreshScienceMissions_emptyAnswerWithCanCleanDatabaseClearsCachedMissionsOfThePlace() = runTest {
        missionDao.insert(ScienceMission(missionKey = "old", observationPlaceID = place.id, pipeline_type = "o"))
        val other = ScienceMission(missionKey = "other", observationPlaceID = "place-2", pipeline_type = "o")
        missionDao.insert(other)

        val outcome = repository(missions = "{}")
            .refreshScienceMissions(place, "start", "end", canCleanDatabase = true)

        assertEquals(MissionRefreshOutcome.Empty, outcome)
        assertEquals(listOf(other), missionDao.missions.value)
    }

    @Test
    fun refreshScienceMissions_emptyAnswerWithoutCanCleanDatabaseKeepsCachedMissions() = runTest {
        val existing = ScienceMission(missionKey = "old", observationPlaceID = place.id, pipeline_type = "o")
        missionDao.insert(existing)

        val outcome = repository(missions = "{}").refreshScienceMissions(place, "start", "end")

        assertEquals(MissionRefreshOutcome.Empty, outcome)
        assertEquals(listOf(existing), missionDao.missions.value)
    }

    @Test
    fun refreshScienceMissions_onlyUnknownMissionsIsEmpty() = runTest {
        val json = """{"weird": ${missionJson("zzz", "WEIRD")}}"""

        val outcome = routedRepository(json).refreshScienceMissions(place, "start", "end")

        assertEquals(MissionRefreshOutcome.Empty, outcome)
        assertTrue(missionDao.missions.value.isEmpty())
    }

    @Test
    fun refreshScienceMissions_malformedBodyIsAFailureAndKeepsCachedMissions() = runTest {
        val existing = ScienceMission(missionKey = "old", observationPlaceID = place.id, pipeline_type = "o")
        missionDao.insert(existing)
        missionDao.operations.clear()

        for (body in listOf("<html>", "0", "")) {
            val outcome = routedRepository(body).refreshScienceMissions(place, "start", "end", canCleanDatabase = true)

            assertEquals(MissionRefreshOutcome.Failure(ScienceApiFailure.MalformedResponse), outcome, body)
        }
        assertEquals(listOf(existing), missionDao.missions.value)
        assertTrue(missionDao.operations.isEmpty())
    }

    @Test
    fun refreshScienceMissions_networkExceptionIsAFailureAndKeepsCachedMissions() = runTest {
        val existing = ScienceMission(missionKey = "old", observationPlaceID = place.id, pipeline_type = "o")
        missionDao.insert(existing)
        missionDao.operations.clear()
        val repository = ScienceMissionRepository(
            scienceMissionDao = missionDao,
            ephemerisDataDao = ephemerisDao,
            scienceAPI = ScienceAPI(HttpClient(MockEngine { throw IllegalStateException("offline") })),
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        val outcome = repository.refreshScienceMissions(place, "start", "end", canCleanDatabase = true)

        assertEquals(MissionRefreshOutcome.Failure(ScienceApiFailure.Network), outcome)
        assertEquals(listOf(existing), missionDao.missions.value)
        assertTrue(missionDao.operations.isEmpty())
    }

    @Test
    fun getCometEphemerisData_fetchesFromInjectedApiAndCaches() = runTest {
        val mission = ScienceMission(
            missionKey = "m1",
            observationPlaceID = place.id,
            pipeline_type = "c",
            ephemeris_args = ephemerisArgs
        )

        val result = repository().getCometEphemerisData(mission)

        assertEquals(1, result.size)
        assertEquals(result, ephemerisDao.items)
        assertTrue(requests.single().contains("is_comet=true"))
    }

    @Test
    fun getPlanetaryDefenseData_returnsCachedDataWithoutCallingApi() = runTest {
        val cached = EphemerisData(
            missionKey = "m1",
            observationPlaceID = place.id,
            timestamp = kotlin.time.Clock.System.now().toEpochMilliseconds() + 60_000
        )
        ephemerisDao.insertAll(listOf(cached))
        val mission = ScienceMission(
            missionKey = "m1",
            observationPlaceID = place.id,
            pipeline_type = "p",
            ephemeris_args = ephemerisArgs
        )

        val result = repository().getPlanetaryDefenseData(mission)

        assertEquals(listOf(cached), result)
        assertTrue(requests.isEmpty())
    }

    private fun TestScope.routedRepository(
        missions: String,
        missionsStatus: HttpStatusCode = HttpStatusCode.OK,
        ephemeris: (name: String) -> String = { ephemerisJson }
    ) = ScienceMissionRepository(
        scienceMissionDao = missionDao,
        ephemerisDataDao = ephemerisDao,
        scienceAPI = ScienceAPI(HttpClient(MockEngine { request ->
            val body = request.body.toByteArray().decodeToString()
            val headers = headersOf(HttpHeaders.ContentType, "application/json")
            if (body.contains("action=get-science-events")) {
                respond(missions, missionsStatus, headers)
            } else {
                val name = body.split("&").first { it.startsWith("name=") }.removePrefix("name=")
                respond(ephemeris(name), HttpStatusCode.OK, headers)
            }
        })),
        ioDispatcher = StandardTestDispatcher(testScheduler)
    )

    private fun missionJson(pipelineType: String, ephemerisName: String) = """
        {"pipeline_type": "$pipelineType", "target_name": "$ephemerisName", "ephemeris_args": {
          "name": "$ephemerisName", "loc": "43.28,5.37", "tstart": "2025-01-01T00:00:00Z",
          "duration": "120.0", "gain": "20.0", "exp_time": "500.0"}}
    """.trimIndent()

    private val twoMissionsJson = """
        {"good": ${missionJson("c", "GOOD")}, "bad": ${missionJson("p", "BAD")}}
    """.trimIndent()

    @Test
    fun refreshScienceMissions_malformedEphemerisDoesNotFailRefreshNorOtherMissions() = runTest {
        val repository = routedRepository(twoMissionsJson) { name -> if (name == "BAD") "<html>" else ephemerisJson }

        val outcome = repository.refreshScienceMissions(place, "start", "end")

        assertEquals(MissionRefreshOutcome.Success(2), outcome)
        assertEquals(setOf("good", "bad"), missionDao.missions.value.map { it.missionKey }.toSet())
        assertEquals(1, ephemerisDao.items.size)
        assertEquals(setOf("good"), ephemerisDao.items.map { it.missionKey }.toSet())
    }

    @Test
    fun refreshScienceMissions_throwingEphemerisFetchDoesNotFailRefreshNorOtherMissions() = runTest {
        ephemerisDao.failingMissionKey = "bad"

        val outcome = routedRepository(twoMissionsJson).refreshScienceMissions(place, "start", "end")

        assertEquals(MissionRefreshOutcome.Success(2), outcome)
        assertEquals(setOf("good", "bad"), missionDao.missions.value.map { it.missionKey }.toSet())
        assertEquals(setOf("good"), ephemerisDao.items.map { it.missionKey }.toSet())
    }

    @Test
    fun refreshScienceMissions_httpErrorIsANetworkFailureAndKeepsCachedMissions() = runTest {
        val existing = ScienceMission(missionKey = "old", observationPlaceID = place.id, pipeline_type = "o")
        missionDao.insert(existing)
        missionDao.operations.clear()

        val outcome = routedRepository(twoMissionsJson, HttpStatusCode.InternalServerError)
            .refreshScienceMissions(place, "start", "end", canCleanDatabase = true)

        assertEquals(MissionRefreshOutcome.Failure(ScienceApiFailure.Network), outcome)
        assertEquals(listOf(existing), missionDao.missions.value)
        assertTrue(missionDao.operations.isEmpty())
        assertTrue(ephemerisDao.items.isEmpty())
    }

    @Test
    fun refreshScienceMissions_emptyAnswerOnlyClearsAndFetchesNoEphemeris() = runTest {
        val outcome = routedRepository("{}").refreshScienceMissions(place, "start", "end", canCleanDatabase = true)

        assertEquals(MissionRefreshOutcome.Empty, outcome)
        assertEquals(listOf("clearAll:${place.id}"), missionDao.operations)
        assertTrue(ephemerisDao.items.isEmpty())
    }

    @Test
    fun refreshScienceMissions_canCleanDatabaseClearsPlaceBeforeInserting() = runTest {
        missionDao.insert(ScienceMission(missionKey = "old", observationPlaceID = place.id, pipeline_type = "o"))
        val other = ScienceMission(missionKey = "other", observationPlaceID = "place-2", pipeline_type = "o")
        missionDao.insert(other)
        missionDao.operations.clear()

        val outcome = routedRepository(twoMissionsJson)
            .refreshScienceMissions(place, "start", "end", canCleanDatabase = true)

        assertEquals(MissionRefreshOutcome.Success(2), outcome)
        assertEquals(listOf("clearAll:${place.id}", "insertAll"), missionDao.operations)
        val stored = missionDao.missions.value
        assertEquals(setOf("good", "bad", "other"), stored.map { it.missionKey }.toSet())
        assertTrue(other in stored)
    }

    @Test
    fun refreshScienceMissions_withoutCanCleanDatabaseKeepsExistingMissions() = runTest {
        missionDao.insert(ScienceMission(missionKey = "old", observationPlaceID = place.id, pipeline_type = "o"))
        missionDao.operations.clear()

        routedRepository(twoMissionsJson).refreshScienceMissions(place, "start", "end")

        assertEquals(listOf("insertAll"), missionDao.operations)
        assertTrue("old" in missionDao.missions.value.map { it.missionKey })
    }

    @Test
    fun refreshScienceMissions_unknownPipelineTypeIsNotInserted() = runTest {
        val json = """{"good": ${missionJson("c", "GOOD")}, "weird": ${missionJson("zzz", "WEIRD")}}"""

        val outcome = routedRepository(json).refreshScienceMissions(place, "start", "end")

        assertEquals(MissionRefreshOutcome.Success(1), outcome)
        assertEquals(listOf("good"), missionDao.missions.value.map { it.missionKey })
    }
}
