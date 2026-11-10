package com.domnis.nebuni.ui.splash

import com.domnis.nebuni.AppState
import com.domnis.nebuni.Screen
import com.domnis.nebuni.data.ObservationPlace
import com.domnis.nebuni.repository.FakeObservationPlaceDao
import com.domnis.nebuni.repository.ObservationPlaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {
    private val appState = AppState()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun restartShowsThePlaceWithTheGreatestLastSelectedAt() = runTest {
        val older = ObservationPlace(id = "older", name = "A", lastSelectedAt = 5)
        val latest = ObservationPlace(id = "latest", name = "Z", lastSelectedAt = 20)
        val other = ObservationPlace(id = "other", name = "B", lastSelectedAt = 10)
        SplashViewModel(appState, ObservationPlaceRepository(FakeObservationPlaceDao(listOf(older, latest, other))))

        advanceUntilIdle()

        assertEquals(latest, appState.currentObservationPlace.value)
        assertEquals(Screen.Main, appState.currentRootScreen.value)
    }

    @Test
    fun noPlaceKeepsDefaultPlaceAndGoesToMain() = runTest {
        val defaultPlace = appState.currentObservationPlace.value
        SplashViewModel(appState, ObservationPlaceRepository(FakeObservationPlaceDao()))

        advanceUntilIdle()

        assertEquals(defaultPlace, appState.currentObservationPlace.value)
        assertEquals(Screen.Main, appState.currentRootScreen.value)
    }
}
