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

package com.domnis.nebuni.ui.main

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domnis.nebuni.AppState
import com.domnis.nebuni.data.ObservationPlace
import com.domnis.nebuni.data.ScienceMission
import com.domnis.nebuni.data.ScienceMissionType
import com.domnis.nebuni.getCurrentDate
import com.domnis.nebuni.getCurrentDateAndTime
import com.domnis.nebuni.getCurrentDateAndTimeWithOffset
import com.domnis.nebuni.repository.ObservationPlaceRepository
import com.domnis.nebuni.repository.ScienceMissionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class MainViewModel(
    var appState: AppState,
    private val observationPlaceRepository: ObservationPlaceRepository,
    private val scienceMissionRepository: ScienceMissionRepository,
    private val refreshDispatcher: CoroutineDispatcher = Dispatchers.Default
): ViewModel() {
    enum class ObservationPlaceConfigurationState {
        loading,
        invalid,
        valid
    }

    var currentObservationPlaceConfigurationState = mutableStateOf(ObservationPlaceConfigurationState.loading)
    var isLoadingMissions = mutableStateOf(false)
    var scienceMissionList = mutableStateOf<List<MissionSection>>(emptyList())
    var selectedMission = mutableStateOf<ScienceMission?>(null)

    /** Every saved place, sorted by name, for the place switcher. */
    var places = mutableStateOf<List<ObservationPlace>>(emptyList())

    // In memory only: kept by the ViewModel across configuration changes, not persisted.
    var filter = mutableStateOf(MissionFilter())
    var availableTypes = mutableStateOf<List<MissionTypeCount>>(emptyList())
    var grouping = mutableStateOf(Grouping.Date)

    var startTime = mutableStateOf(getCurrentDateAndTime())
    var endTime = mutableStateOf(getCurrentDateAndTimeWithOffset(12))

    private var missionJob: Job? = null
    private var refreshJob: Job? = null

    /** The values a mission refresh depends on: a change of any other field (the name) is ignored. */
    private data class RefreshKey(
        val id: String,
        val latitude: Double,
        val longitude: Double,
        val altMin: Int,
        val altMax: Int,
        val azMin: Int,
        val azMax: Int
    )

    private fun ObservationPlace.refreshKey() = RefreshKey(id, latitude, longitude, altMin, altMax, azMin, azMax)

    init {
        viewModelScope.launch {
            observationPlaceRepository.selectedPlace()
                .onEach { place ->
                    // AppState.currentObservationPlace is only written from the persisted selection
                    if (place == null) {
                        currentObservationPlaceConfigurationState.value = ObservationPlaceConfigurationState.invalid
                    } else {
                        appState.updateObservationPlace(place)
                        currentObservationPlaceConfigurationState.value = ObservationPlaceConfigurationState.valid
                    }
                }
                .filterNotNull()
                .distinctUntilChangedBy { it.refreshKey() }
                .collect { place -> onPlaceChanged(place) }
        }

        viewModelScope.launch {
            observationPlaceRepository.places().collect { places.value = it }
        }
    }

    private fun onPlaceChanged(place: ObservationPlace) {
        selectedMission.value = null
        observeMissions(place)
        refreshScienceMissions(place)
    }

    private fun observeMissions(place: ObservationPlace) {
        missionJob?.cancel()

        missionJob = viewModelScope.launch {
            scienceMissionRepository
                .getScienceMissionFor(place)
                .combine(snapshotFlow { filter.value }) { missions, filter ->
                    missions to filter
                }
                .combine(snapshotFlow { grouping.value }) { (missions, filter), grouping ->
                    Triple(missions, filter, grouping)
                }
                .collect { (missions, filter, grouping) ->
                    availableTypes.value = countMissionTypes(missions)
                    val today = getCurrentDate(TimeZone.currentSystemDefault())
                    val nowEpochMillis = kotlin.time.Clock.System.now().toEpochMilliseconds()
                    val filteredMissions = filterMissions(missions, filter, today, nowEpochMillis)
                    scienceMissionList.value = when (grouping) {
                        Grouping.Date -> groupMissionsByDay(
                            filteredMissions,
                            today = today,
                            nowEpochMillis = nowEpochMillis
                        )
                        Grouping.Type -> groupMissionsByType(filteredMissions)
                    }
                }
        }
    }

    fun refreshScienceMissions() {
        refreshScienceMissions(appState.currentObservationPlace.value)
    }

    /** Refreshes the missions of [place]; any refresh still in flight is cancelled first. */
    private fun refreshScienceMissions(place: ObservationPlace) {
        refreshJob?.cancel()
        isLoadingMissions.value = true

        refreshJob = viewModelScope.launch {
            // first call need to be sync to clean up database if needed
            val isSucceed = withContext(refreshDispatcher) {
                scienceMissionRepository.refreshScienceMissions(
                    place,
                    getCurrentDateAndTimeWithOffset(0),
                    getCurrentDateAndTimeWithOffset(2),
                    true
                )
            }

            //update UI to notify end of loading state
            isLoadingMissions.value = false

            if (isSucceed) { // if first call is success, perform next calls
                // get next chunks of data 3 days by 3 days, as children of this job so they are
                // cancelled with it
                withContext(refreshDispatcher) {
                    val maxIteration = 3
                    for (i in 1..maxIteration) {
                        //2 because only 3 day of asteroids occultation data per request
                        val dayOffset = i + 2 * i
                        val newStartTime = getCurrentDateAndTimeWithOffset(dayOffset)
                        val newEndTime = getCurrentDateAndTimeWithOffset(dayOffset + 2)

                        launch {
                            scienceMissionRepository.refreshScienceMissions(place, newStartTime, newEndTime)
                        }
                    }
                }
            }
        }
    }

    /** Persists [newObservationPlace] as the selected place; the selection flow then switches to it. */
    fun addObservationPlace(newObservationPlace: ObservationPlace) {
        viewModelScope.launch {
            observationPlaceRepository.add(newObservationPlace)
        }
    }

    /** Makes the place [id] the selected one; the selection flow then switches to it. */
    fun selectPlace(id: String) {
        viewModelScope.launch {
            observationPlaceRepository.select(id)
        }
    }

    /**
     * Deletes the place [id] with its cached data. When it is the selected place, the refresh in
     * flight is cancelled and awaited first so it cannot write rows back for the deleted place; the
     * selection flow then falls back to the most recently selected remaining place. The refresh of
     * another place is left untouched. The last place is never deleted.
     */
    fun deletePlace(id: String) {
        if (places.value.size <= 1) return
        val isSelected = id == appState.currentObservationPlace.value.id
        viewModelScope.launch {
            if (isSelected) {
                refreshJob?.cancelAndJoin()
                isLoadingMissions.value = false
            }
            observationPlaceRepository.delete(id)
        }
    }

    fun selectMission(mission: ScienceMission) {
        selectedMission.value = mission
    }

    fun unselectMission() {
        selectedMission.value = null
    }

    fun toggleType(type: ScienceMissionType) {
        filter.value = filter.value.toggle(type)
    }

    fun togglePriorityOnly() {
        filter.value = filter.value.copy(priorityOnly = !filter.value.priorityOnly)
    }

    fun selectDateRange(dateRange: DateRange) {
        filter.value = filter.value.copy(dateRange = dateRange)
    }

    fun selectGrouping(newGrouping: Grouping) {
        grouping.value = newGrouping
    }

    fun clearFilters() {
        filter.value = MissionFilter()
    }
}