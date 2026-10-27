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
import com.domnis.nebuni.database.AppDatabase
import com.domnis.nebuni.getCurrentDate
import com.domnis.nebuni.getCurrentDateAndTime
import com.domnis.nebuni.getCurrentDateAndTimeWithOffset
import com.domnis.nebuni.repository.ScienceMissionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class MainViewModel(var appState: AppState, val database: AppDatabase): ViewModel() {
    enum class ObservationPlaceConfigurationState {
        loading,
        invalid,
        valid
    }

    var currentObservationPlaceConfigurationState = mutableStateOf(ObservationPlaceConfigurationState.loading)
    var isLoadingMissions = mutableStateOf(false)
    var scienceMissionList = mutableStateOf<List<MissionSection>>(emptyList())
    var selectedMission = mutableStateOf<ScienceMission?>(null)

    // In memory only: kept by the ViewModel across configuration changes, not persisted.
    var filter = mutableStateOf(MissionFilter())
    var availableTypes = mutableStateOf<List<MissionTypeCount>>(emptyList())

    var startTime = mutableStateOf(getCurrentDateAndTime())
    var endTime = mutableStateOf(getCurrentDateAndTimeWithOffset(12))

    private val scienceMissionRepository = ScienceMissionRepository(database)
    private var missionJob: Job? = null

    init {
        viewModelScope.launch {
            snapshotFlow { appState.currentObservationPlace.value }
                .onEach {
                    missionJob?.cancel()

                    missionJob = viewModelScope.launch {
                        scienceMissionRepository
                            .getScienceMissionFor(appState.currentObservationPlace.value)
                            .combine(snapshotFlow { filter.value }) { missions, filter ->
                                missions to filter
                            }
                            .collect { (missions, filter) ->
                                availableTypes.value = countMissionTypes(missions)
                                val today = getCurrentDate(TimeZone.currentSystemDefault())
                                val nowEpochMillis = kotlin.time.Clock.System.now().toEpochMilliseconds()
                                scienceMissionList.value = groupMissionsByDay(
                                    filterMissions(missions, filter, today, nowEpochMillis),
                                    today = today,
                                    nowEpochMillis = nowEpochMillis
                                )
                            }
                    }
                }
                .launchIn(viewModelScope)
        }

        viewModelScope.launch {
            database.getObservationPlaceDao().getAllAsFlow().collect { places ->
                if (places.isNotEmpty()) {
                    if (currentObservationPlaceConfigurationState.value != ObservationPlaceConfigurationState.valid) {
                        val selectedObservationPlace = places.first()
                        appState.updateObservationPlace(selectedObservationPlace)

                        currentObservationPlaceConfigurationState.value = ObservationPlaceConfigurationState.valid
                    }

                    refreshScienceMissions()
                } else {
                    currentObservationPlaceConfigurationState.value = ObservationPlaceConfigurationState.invalid
                }
            }
        }
    }

    fun refreshScienceMissions() {
        isLoadingMissions.value = true

        viewModelScope.launch(Dispatchers.Default) {
            // first call need to be sync to clean up database if needed
            val newStartTime = getCurrentDateAndTimeWithOffset(0)
            val newEndTime = getCurrentDateAndTimeWithOffset(2)

            val isSucceed = scienceMissionRepository.refreshScienceMissions(
                appState.currentObservationPlace.value,
                newStartTime,
                newEndTime,
                true
            )

            //update UI to notify end of loading state
            launch(Dispatchers.Main) {
                isLoadingMissions.value = false
            }

            if (isSucceed) { // if first call is success, perform next calls
                // get next chunk of data 3 days by 3 days
                val maxIteration = 3
                for (i in 1..maxIteration) {
                    //2 because only 3 day of asteroids occultation data per request
                    val dayOffset =
                        i + 2 * i //2 because only 3 day of asteroids occultation data per request
                    val newStartTime = getCurrentDateAndTimeWithOffset(dayOffset)
                    val newEndTime = getCurrentDateAndTimeWithOffset(dayOffset + 2)

                    viewModelScope.launch(Dispatchers.Default) {
                        scienceMissionRepository.refreshScienceMissions(
                            appState.currentObservationPlace.value,
                            newStartTime,
                            newEndTime
                        )

                        launch(Dispatchers.Main) {
                            isLoadingMissions.value = false
                        }
                    }
                }
            }
        }
    }

    fun addObservationPlace(newObservationPlace: ObservationPlace) {
        viewModelScope.launch(Dispatchers.Default) {
            database.getObservationPlaceDao().insert(newObservationPlace)
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

    fun clearFilters() {
        filter.value = MissionFilter()
    }
}