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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domnis.nebuni.AppState
import com.domnis.nebuni.data.ScienceMission
import com.domnis.nebuni.data.ScienceMissionType
import com.domnis.nebuni.database.AppDatabase
import com.domnis.nebuni.getCurrentDateAndTime
import com.domnis.nebuni.getCurrentDateAndTimeWithOffset
import com.domnis.nebuni.network.ScienceAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(var appState: AppState, val database: AppDatabase): ViewModel() {
    var isLoadingMissions = mutableStateOf(false)
    var scienceMissionList = mutableStateOf(emptyList<ScienceMission>())
    var selectedMission = mutableStateOf<ScienceMission?>(null)

    var startTime = mutableStateOf(getCurrentDateAndTime())
    var endTime = mutableStateOf(getCurrentDateAndTimeWithOffset(12))

    init {
        viewModelScope.launch {
            database.getScienceMissionDao().getAllAsFlow().collect { missions ->
                //sort missions by date for now
                scienceMissionList.value = missions.sortedBy { it.getMissionStartTimestamp() }
            }
        }

        refreshScienceMissions()
    }

    fun refreshScienceMissions() {
        isLoadingMissions.value = true

        val newStartTime = getCurrentDateAndTime()
        val newEndTime = getCurrentDateAndTimeWithOffset(12)

        startTime.value = newStartTime
        endTime.value = newEndTime

        viewModelScope.launch(Dispatchers.Default) {
            val apiResult = ScienceAPI().listScienceMissions(
                observationPlace = appState.currentObservationPlace.value,
                startDateTime = newStartTime,
                endDateTime = newEndTime
            )
                .filter { it.getMissionType() != ScienceMissionType.Unknown } // remove missions with unknown type

            launch {
                val scienceMissionDao = database.getScienceMissionDao()
                if (apiResult.isNotEmpty()) {
                    scienceMissionDao.clearAll()

                    scienceMissionDao.insertAll(apiResult)
                }
            }

            launch(Dispatchers.Main) {
                isLoadingMissions.value = false
            }
        }
    }

    fun selectMission(mission: ScienceMission) {
        selectedMission.value = mission
    }

    fun unselectMission() {
        selectedMission.value = null
    }
}