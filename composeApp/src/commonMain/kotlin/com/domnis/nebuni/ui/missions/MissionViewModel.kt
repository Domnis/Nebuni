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

package com.domnis.nebuni.ui.missions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domnis.nebuni.data.EphemerisData
import com.domnis.nebuni.data.ScienceMission
import com.domnis.nebuni.data.ScienceMissionType
import com.domnis.nebuni.repository.ScienceMissionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MissionViewModel(
    private val scienceMissionRepository: ScienceMissionRepository
) : ViewModel() {
    data class EphemerisState(
        val isLoading: Boolean = false,
        val ephemerisData: List<EphemerisData> = listOf()
    )

    private val _ephemerisState = MutableStateFlow(EphemerisState())
    val ephemerisState: StateFlow<EphemerisState> = _ephemerisState.asStateFlow()

    private var loadingJob: Job? = null

    /**
     * Loads the ephemeris data of [mission], replacing the data of any previously loaded mission.
     * Only comet and planetary defense missions have ephemeris data.
     */
    fun loadEphemeris(mission: ScienceMission) {
        loadingJob?.cancel()

        val missionType = mission.getMissionType()
        if (
            missionType != ScienceMissionType.CometaryActivity
            && missionType != ScienceMissionType.PlanetaryDefense
        ) {
            _ephemerisState.value = EphemerisState()
            return
        }

        _ephemerisState.value = EphemerisState(isLoading = true)
        loadingJob = viewModelScope.launch {
            // the repository (Room and Ktor) already moves its work off the main thread
            val ephemerisData = try {
                if (missionType == ScienceMissionType.CometaryActivity) {
                    scienceMissionRepository.getCometEphemerisData(cometScienceMission = mission)
                } else {
                    scienceMissionRepository.getPlanetaryDefenseData(planetaryDefenseScienceMission = mission)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("Ephemeris loading failed for mission ${mission.missionKey}: $e")
                listOf()
            }
            _ephemerisState.value = EphemerisState(ephemerisData = ephemerisData)
        }
    }
}
