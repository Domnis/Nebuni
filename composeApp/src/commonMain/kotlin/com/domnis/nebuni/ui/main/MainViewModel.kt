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
import com.domnis.nebuni.customInstantParseFormat
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.parse
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
    var scienceMissionList = mutableStateOf(arrayListOf<Pair<String, List<ScienceMission>>>())
    var selectedMission = mutableStateOf<ScienceMission?>(null)

    var startTime = mutableStateOf(getCurrentDateAndTime())
    var endTime = mutableStateOf(getCurrentDateAndTimeWithOffset(12))

    private val scienceMissionRepository = ScienceMissionRepository(database)
    private var missionJob: Job? = null

    private fun getSection(today: String, sectionDate: String, sectionItems: ArrayList<ScienceMission>): Pair<String, List<ScienceMission>>? {
        if (sectionDate.isEmpty() || sectionItems.isEmpty()) return null

        val sectionTitle = if (sectionDate == today) "Today" else sectionDate
        return Pair(sectionTitle, sectionItems)
    }

    init {
        viewModelScope.launch {
            snapshotFlow { appState.currentObservationPlace.value }
                .onEach {
                    missionJob?.cancel()

                    missionJob = viewModelScope.launch {
                        scienceMissionRepository
                            .getScienceMissionFor(appState.currentObservationPlace.value)
                            .collect { missions ->
                                //sort missions by date for now
                                var today = getCurrentDate(TimeZone.currentSystemDefault())
                                var todayNearMidnight = "${today}T18:00:00"
                                val todayEpochMilliseconds = kotlin.time.Instant.parse(
                                    todayNearMidnight,
                                    customInstantParseFormat()
                                ).toEpochMilliseconds()
                                val nowEpochMilliseconds = kotlin.time.Clock.System.now().toEpochMilliseconds()

                                var currentDate = ""
                                var currentList = arrayListOf<ScienceMission>()
                                var result = arrayListOf<Pair<String, List<ScienceMission>>>()
                                missions
                                    .sortedBy {
                                        it.getMissionStartTimestamp()
                                    }
//                                    .sortedBy {
//                                        val missionTimestamp = it.getMissionStartTimestamp()
//                                        if (
//                                            it.getMissionType() != ScienceMissionType.CometaryActivity
//                                            || missionTimestamp > nowEpochMilliseconds
//                                            ) {
//                                            missionTimestamp
//                                        } else {
//                                            nowEpochMilliseconds
//                                        }
//                                    }
                                    .forEach {
                                        val missionTimestamp = it.getMissionStartTimestamp()
                                        val startDate = if (
                                            it.getMissionType() != ScienceMissionType.CometaryActivity
                                            || missionTimestamp > nowEpochMilliseconds
                                            ) {
                                            it.getMissionStartDateOnly()
                                        } else today

                                        if (startDate != currentDate) {
                                            getSection(
                                                today,
                                                currentDate,
                                                currentList)?.let { section ->
                                                result.add(section)
                                            }

                                            currentDate = startDate
                                            currentList = arrayListOf<ScienceMission>()
                                        }

                                        currentList.add(it)
                                    }

                                getSection(
                                    today,
                                    currentDate,
                                    currentList)?.let { section ->
                                    result.add(section)
                                }

                                scienceMissionList.value = result
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
            val newEndTime = getCurrentDateAndTimeWithOffset(0 + 2)

            scienceMissionRepository.refreshScienceMissions(
                appState.currentObservationPlace.value,
                newStartTime,
                newEndTime,
                true
            )

            //update UI to notify end of loading state
            launch(Dispatchers.Main) {
                isLoadingMissions.value = false
            }

            // get next chunk of data 3 days by 3 days
            val maxIteration = 3
            for (i in 1..maxIteration) {
                //2 because only 3 day of asteroids occultation data per request
                val dayOffset = i + 2 * i //2 because only 3 day of asteroids occultation data per request
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
}