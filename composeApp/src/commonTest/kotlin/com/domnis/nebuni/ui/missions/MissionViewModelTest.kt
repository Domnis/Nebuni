package com.domnis.nebuni.ui.missions

import com.domnis.nebuni.data.EphemerisArgs
import com.domnis.nebuni.data.EphemerisData
import com.domnis.nebuni.data.ScienceMission
import com.domnis.nebuni.network.ScienceAPI
import com.domnis.nebuni.repository.FakeEphemerisDataDao
import com.domnis.nebuni.repository.FakeScienceMissionDao
import com.domnis.nebuni.repository.ScienceMissionRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class MissionViewModelTest {
    private val ephemerisArgs = EphemerisArgs(
        name = "C/2025 A1",
        loc = "43.28,5.37",
        tstart = "2025-01-01T00:00:00Z",
        duration = "120.0",
        gain = "20.0",
        exp_time = "500.0"
    )

    private val ephemerisJson = """
        {
          "loc": "43.28,5.37",
          "2025-01-01T00:00:00": {"ra": "1", "dec": "2", "alt": 30, "az": 90, "is_visible": true}
        }
    """.trimIndent()

    private val ephemerisDao = FakeEphemerisDataDao()
    private val requests = mutableListOf<String>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel() = MissionViewModel(
        ScienceMissionRepository(
            scienceMissionDao = FakeScienceMissionDao(),
            ephemerisDataDao = ephemerisDao,
            scienceAPI = ScienceAPI(HttpClient(MockEngine { request ->
                requests += request.body.toByteArray().decodeToString()
                respond(ephemerisJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            })),
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )
    )

    private fun mission(key: String, pipelineType: String) = ScienceMission(
        missionKey = key,
        observationPlaceID = "place-1",
        pipeline_type = pipelineType,
        ephemeris_args = ephemerisArgs
    )

    private fun cachedEphemeris(key: String) = EphemerisData(
        missionKey = key,
        observationPlaceID = "place-1",
        timestamp = Clock.System.now().toEpochMilliseconds() + 60_000
    )

    @Test
    fun initialStateIsEmptyAndNotLoading() = runTest {
        assertEquals(MissionViewModel.EphemerisState(), viewModel().ephemerisState.value)
    }

    @Test
    fun cometMissionIsLoadingThenExposesEphemerisFromRepository() = runTest {
        val viewModel = viewModel()

        viewModel.loadEphemeris(mission("m1", "c"))

        assertTrue(viewModel.ephemerisState.value.isLoading)
        // the mock HTTP engine answers on its own threads, so wait for the state rather than idling
        val state = viewModel.ephemerisState.first { !it.isLoading }
        assertFalse(state.isLoading)
        assertEquals(1, state.ephemerisData.size)
        assertTrue(requests.single().contains("is_comet=true"))
    }

    @Test
    fun planetaryDefenseMissionExposesCachedEphemeris() = runTest {
        val cached = cachedEphemeris("m1")
        ephemerisDao.insertAll(listOf(cached))
        val viewModel = viewModel()

        viewModel.loadEphemeris(mission("m1", "p"))
        advanceUntilIdle()

        assertEquals(MissionViewModel.EphemerisState(ephemerisData = listOf(cached)), viewModel.ephemerisState.value)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun missionWithoutEphemerisClearsPreviousDataWithoutLoading() = runTest {
        ephemerisDao.insertAll(listOf(cachedEphemeris("m1")))
        val viewModel = viewModel()
        viewModel.loadEphemeris(mission("m1", "p"))
        advanceUntilIdle()

        viewModel.loadEphemeris(mission("m2", "o"))

        assertEquals(MissionViewModel.EphemerisState(), viewModel.ephemerisState.value)
        advanceUntilIdle()
        assertEquals(MissionViewModel.EphemerisState(), viewModel.ephemerisState.value)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun selectingAnotherMissionWhileLoadingKeepsOnlyTheLatestResult() = runTest {
        val cached = cachedEphemeris("m2")
        ephemerisDao.insertAll(listOf(cached))
        val viewModel = viewModel()

        viewModel.loadEphemeris(mission("m1", "c"))
        viewModel.loadEphemeris(mission("m2", "p"))
        advanceUntilIdle()

        assertEquals(MissionViewModel.EphemerisState(ephemerisData = listOf(cached)), viewModel.ephemerisState.value)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun failingLoadStopsLoadingWithEmptyData() = runTest {
        ephemerisDao.failingMissionKey = "m1"
        val viewModel = viewModel()

        viewModel.loadEphemeris(mission("m1", "c"))
        advanceUntilIdle()

        assertEquals(MissionViewModel.EphemerisState(), viewModel.ephemerisState.value)
    }
}
