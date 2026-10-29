package com.domnis.nebuni.repository

import com.domnis.nebuni.data.EphemerisArgs
import com.domnis.nebuni.data.EphemerisData
import com.domnis.nebuni.data.ObservationPlace
import com.domnis.nebuni.data.ScienceMission
import com.domnis.nebuni.database.EphemerisDataDao
import com.domnis.nebuni.database.ScienceMissionDao
import com.domnis.nebuni.network.ScienceAPI
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    private class FakeScienceMissionDao : ScienceMissionDao {
        val missions = MutableStateFlow<List<ScienceMission>>(emptyList())

        override suspend fun insert(item: ScienceMission) = insertAll(listOf(item))
        override suspend fun insertAll(items: List<ScienceMission>) {
            val keys = items.map { it.missionKey to it.observationPlaceID }.toSet()
            missions.value = missions.value.filterNot { (it.missionKey to it.observationPlaceID) in keys } + items
        }
        override suspend fun clearAll(observationPlaceID: String) {
            missions.value = missions.value.filterNot { it.observationPlaceID == observationPlaceID }
        }
        override suspend fun count(observationPlaceID: String): Int =
            missions.value.count { it.observationPlaceID == observationPlaceID }
        override fun getAllAsFlow(observationPlaceID: String): Flow<List<ScienceMission>> =
            missions.map { list -> list.filter { it.observationPlaceID == observationPlaceID } }
    }

    private class FakeEphemerisDataDao : EphemerisDataDao {
        val items = mutableListOf<EphemerisData>()

        override suspend fun insertAll(items: List<EphemerisData>) {
            this.items += items
        }
        override suspend fun clearAll(missionKey: String, observationPlaceID: String) {
            items.removeAll { it.missionKey == missionKey && it.observationPlaceID == observationPlaceID }
        }
        override suspend fun clearEphemerisData(missionKeys: List<String>, observationPlaceID: String) {
            items.removeAll { it.missionKey !in missionKeys && it.observationPlaceID == observationPlaceID }
        }
        override suspend fun getAllEphemerisData(
            missionKey: String,
            observationPlaceID: String,
            timestamp: Long
        ): List<EphemerisData> = items.filter {
            it.missionKey == missionKey && it.observationPlaceID == observationPlaceID &&
                it.timestamp >= timestamp && it.timestamp <= timestamp + 36_000_000
        }
    }

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

        val succeeded = repository.refreshScienceMissions(place, "start", "end", canCleanDatabase = true)

        assertTrue(succeeded)
        val stored = repository.getScienceMissionFor(place).first()
        assertEquals(setOf("m1", "m2"), stored.map { it.missionKey }.toSet())
    }

    @Test
    fun refreshScienceMissions_emptyApiResultKeepsDatabaseUntouched() = runTest {
        val existing = ScienceMission(missionKey = "old", observationPlaceID = place.id, pipeline_type = "o")
        missionDao.insert(existing)

        val succeeded = repository(missions = "{}")
            .refreshScienceMissions(place, "start", "end", canCleanDatabase = true)

        assertFalse(succeeded)
        assertEquals(listOf(existing), missionDao.missions.value)
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
}
