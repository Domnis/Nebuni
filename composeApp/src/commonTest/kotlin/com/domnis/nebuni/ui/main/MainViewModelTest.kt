package com.domnis.nebuni.ui.main

import com.domnis.nebuni.AppState
import com.domnis.nebuni.data.ObservationPlace
import com.domnis.nebuni.data.ScienceMission
import com.domnis.nebuni.network.ScienceAPI
import com.domnis.nebuni.repository.FakeEphemerisDataDao
import com.domnis.nebuni.repository.FakeObservationPlaceDao
import com.domnis.nebuni.repository.FakeScienceMissionDao
import com.domnis.nebuni.repository.ObservationPlaceRepository
import com.domnis.nebuni.repository.ScienceMissionRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val placeA = ObservationPlace(id = "a", name = "A", latitude = 1.0, lastSelectedAt = 10)
    private val placeB = ObservationPlace(id = "b", name = "B", latitude = 2.0, lastSelectedAt = 5)

    private val oneMissionJson = """{"m1": {"pipeline_type": "o", "target_name": "Occultation"}}"""

    private val appState = AppState()
    private var clock = 100L

    /** Latitudes of the mission list requests, in the order they reached the HTTP engine. */
    private val requests = MutableStateFlow<List<String>>(emptyList())
    private val cancelled = MutableStateFlow<List<String>>(emptyList())

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun placeRepository(dao: FakeObservationPlaceDao) = ObservationPlaceRepository(dao) { clock++ }

    /** [respond] answers a mission list request; it receives the latitude and its index for that latitude. */
    private fun TestScope.viewModel(
        placeRepository: ObservationPlaceRepository,
        missionDao: FakeScienceMissionDao = FakeScienceMissionDao(),
        respond: suspend MockRequestHandleScope.(latitude: String, index: Int) -> HttpResponseData = { _, _ ->
            respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
    ) = MainViewModel(
        appState = appState,
        observationPlaceRepository = placeRepository,
        scienceMissionRepository = ScienceMissionRepository(
            scienceMissionDao = missionDao,
            ephemerisDataDao = FakeEphemerisDataDao(),
            scienceAPI = ScienceAPI(HttpClient(MockEngine { request ->
                val body = request.body.toByteArray().decodeToString()
                val latitude = body.split("&").first { it.startsWith("lat=") }.removePrefix("lat=")
                var index = 0
                requests.update { list ->
                    index = list.count { it == latitude }
                    list + latitude
                }
                respond(latitude, index)
            })),
            ioDispatcher = StandardTestDispatcher(testScheduler)
        ),
        refreshDispatcher = StandardTestDispatcher(testScheduler)
    )

    @Test
    fun noPlaceIsInvalidAndDoesNotRefresh() = runTest {
        val viewModel = viewModel(placeRepository(FakeObservationPlaceDao()))
        advanceUntilIdle()

        assertEquals(MainViewModel.ObservationPlaceConfigurationState.invalid, viewModel.currentObservationPlaceConfigurationState.value)
        assertFalse(viewModel.isLoadingMissions.value)
        assertEquals(emptyList(), requests.value)
    }

    @Test
    fun showsAndRefreshesTheMostRecentlySelectedPlace() = runTest {
        val viewModel = viewModel(placeRepository(FakeObservationPlaceDao(listOf(placeB, placeA))))
        advanceUntilIdle()

        assertEquals(MainViewModel.ObservationPlaceConfigurationState.valid, viewModel.currentObservationPlaceConfigurationState.value)
        assertEquals(placeA, appState.currentObservationPlace.value)
        assertEquals(listOf("1.0"), requests.first { it.isNotEmpty() })
    }

    @Test
    fun addingAPlaceRefreshesOnlyTheNewPlace() = runTest {
        val viewModel = viewModel(placeRepository(FakeObservationPlaceDao(listOf(placeA))))
        requests.first { it.size == 1 }
        viewModel.isLoadingMissionsSettled()

        viewModel.addObservationPlace(ObservationPlace(id = "c", name = "C", latitude = 3.0))

        assertEquals(listOf("1.0", "3.0"), requests.first { it.size == 2 })
        assertEquals("c", appState.currentObservationPlace.value.id)
        viewModel.isLoadingMissionsSettled()
        assertEquals(listOf("1.0", "3.0"), requests.value)
    }

    @Test
    fun deletingTheSelectedPlaceSelectsTheMostRecentlyUsedRemainingOne() = runTest {
        val placeC = ObservationPlace(id = "c", name = "C", latitude = 3.0, lastSelectedAt = 1)
        val dao = FakeObservationPlaceDao(listOf(placeA, placeB, placeC))
        val viewModel = viewModel(placeRepository(dao))
        requests.first { it.isNotEmpty() }

        viewModel.deletePlace("a")
        advanceUntilIdle()

        assertEquals(listOf("delete:a"), dao.operations.filter { it.startsWith("delete:") })
        assertEquals("b", appState.currentObservationPlace.value.id)
        assertEquals(listOf("b", "c"), viewModel.places.value.map { it.id })
    }

    @Test
    fun deletingAnotherPlaceLeavesTheSelectedRefreshUntouched() = runTest {
        val dao = FakeObservationPlaceDao(listOf(placeA, placeB))
        val viewModel = viewModel(placeRepository(dao)) { latitude, index ->
            if (latitude == "1.0" && index > 0) {
                // the next chunks of place A stay in flight until cancelled
                try {
                    awaitCancellation()
                } finally {
                    cancelled.update { it + latitude }
                }
            }
            respond(oneMissionJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        // first call of place A, then its 3 chunks (in flight)
        requests.first { it.size == 4 }
        viewModel.isLoadingMissionsSettled()

        viewModel.deletePlace("b")
        advanceUntilIdle()

        assertEquals(listOf("delete:b"), dao.operations.filter { it.startsWith("delete:") })
        assertEquals(emptyList(), cancelled.value)
        assertFalse(viewModel.isLoadingMissions.value)
        assertEquals("a", appState.currentObservationPlace.value.id)
        assertEquals(listOf("a"), viewModel.places.value.map { it.id })
        // the chunks are still in flight: stop them so the test can complete
        viewModel.viewModelScope.cancel()
        cancelled.first { it.size == 3 }
    }

    @Test
    fun theLastPlaceIsNotDeleted() = runTest {
        val dao = FakeObservationPlaceDao(listOf(placeA))
        val viewModel = viewModel(placeRepository(dao))
        advanceUntilIdle()

        viewModel.deletePlace("a")
        advanceUntilIdle()

        assertEquals(listOf(placeA), dao.places.value)
        assertEquals("a", appState.currentObservationPlace.value.id)
    }

    @Test
    fun renamingTheSelectedPlaceUpdatesAppStateWithoutRefreshing() = runTest {
        val placeRepository = placeRepository(FakeObservationPlaceDao(listOf(placeA)))
        val viewModel = viewModel(placeRepository)
        requests.first { it.size == 1 }
        viewModel.isLoadingMissionsSettled()

        placeRepository.update(placeA.copy(name = "Renamed"))
        advanceUntilIdle()

        assertEquals("Renamed", appState.currentObservationPlace.value.name)
        // a refresh would have raised the loading flag synchronously
        assertFalse(viewModel.isLoadingMissions.value)
        assertEquals(listOf("1.0"), requests.value)
    }

    @Test
    fun movingTheSelectedPlaceRefreshesIt() = runTest {
        val placeRepository = placeRepository(FakeObservationPlaceDao(listOf(placeA)))
        viewModel(placeRepository)
        requests.first { it.size == 1 }

        placeRepository.update(placeA.copy(latitude = 4.0))

        assertEquals(listOf("1.0", "4.0"), requests.first { it.size == 2 })
    }

    @Test
    fun switchingPlaceCancelsInFlightRefreshChunksAndClearsSelectedMission() = runTest {
        val viewModel = viewModel(placeRepository(FakeObservationPlaceDao(listOf(placeA, placeB)))) { latitude, index ->
            if (latitude == "1.0" && index > 0) {
                // the next chunks of place A never answer until they are cancelled
                try {
                    awaitCancellation()
                } finally {
                    cancelled.update { it + latitude }
                }
            }
            val json = if (latitude == "1.0") oneMissionJson else "{}"
            respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        // first call of place A, then its 3 chunks
        requests.first { it.size == 4 }
        viewModel.selectMission(ScienceMission(missionKey = "m1", observationPlaceID = "a", pipeline_type = "o"))

        viewModel.selectPlace("b")

        assertEquals(listOf("1.0", "1.0", "1.0"), cancelled.first { it.size == 3 })
        assertEquals(listOf("1.0", "1.0", "1.0", "1.0", "2.0"), requests.first { it.size == 5 })
        assertEquals(placeB.id, appState.currentObservationPlace.value.id)
        assertNull(viewModel.selectedMission.value)
    }

    @Test
    fun exposesEveryPlaceSortedByName() = runTest {
        val placeC = ObservationPlace(id = "c", name = "c place", latitude = 3.0)
        val viewModel = viewModel(placeRepository(FakeObservationPlaceDao(listOf(placeB, placeC, placeA))))
        advanceUntilIdle()

        assertEquals(listOf("a", "b", "c"), viewModel.places.value.map { it.id })
    }

    @Test
    fun selectingAPlaceOfflineShowsItsCachedMissionsAtOnce() = runTest {
        val missionDao = FakeScienceMissionDao()
        missionDao.missions.value = listOf(
            ScienceMission(missionKey = "mA", observationPlaceID = "a", pipeline_type = "o", tstart = "2026-10-27"),
            ScienceMission(missionKey = "mB", observationPlaceID = "b", pipeline_type = "o", tstart = "2026-10-27")
        )
        // offline: every request fails
        val viewModel = viewModel(placeRepository(FakeObservationPlaceDao(listOf(placeA, placeB))), missionDao) { _, _ ->
            respond("", HttpStatusCode.ServiceUnavailable)
        }
        viewModel.awaitMissionKeys(listOf("mA"))

        viewModel.selectPlace("b")

        viewModel.awaitMissionKeys(listOf("mB"))
        assertEquals(placeB.id, appState.currentObservationPlace.value.id)
        viewModel.isLoadingMissionsSettled()
        // the failed refresh keeps the cache
        assertEquals(listOf("mB"), viewModel.missionKeys())
    }

    private fun MainViewModel.missionKeys() = scienceMissionList.value.flatMap { it.missions }.map { it.missionKey }

    /** Waits for the shown missions to be [keys]: the HTTP engine runs on its own threads. */
    private suspend fun MainViewModel.awaitMissionKeys(keys: List<String>) = withContext(Dispatchers.Default) {
        while (missionKeys() != keys) delay(10)
    }

    /** Waits for the current refresh to answer: the HTTP engine runs on its own threads. */
    private suspend fun MainViewModel.isLoadingMissionsSettled() = withContext(Dispatchers.Default) {
        while (isLoadingMissions.value) delay(10)
    }
}
