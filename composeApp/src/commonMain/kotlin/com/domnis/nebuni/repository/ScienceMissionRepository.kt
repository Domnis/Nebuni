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

package com.domnis.nebuni.repository

import com.domnis.nebuni.data.EphemerisData
import com.domnis.nebuni.data.ObservationPlace
import com.domnis.nebuni.data.ScienceMission
import com.domnis.nebuni.data.ScienceMissionType
import com.domnis.nebuni.database.AppDatabase
import com.domnis.nebuni.getCurrentDateAndTime
import com.domnis.nebuni.network.ScienceAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlin.time.ExperimentalTime

class ScienceMissionRepository(private val database: AppDatabase) {
    fun getScienceMissionFor(observationPlace: ObservationPlace): Flow<List<ScienceMission>> {
        return database.getScienceMissionDao().getAllAsFlow(observationPlace.id)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun refreshScienceMissions(
        observationPlace: ObservationPlace,
        startTime: String,
        endDate: String,
        canCleanDatabase: Boolean = false
    ) : Boolean {
        val scienceAPI = ScienceAPI()
        val apiResult = scienceAPI.listScienceMissions(
            observationPlace = observationPlace,
            startDateTime = startTime,
            endDateTime = endDate
        ).filter { it.getMissionType() != ScienceMissionType.Unknown }

        val scienceMissionDao = database.getScienceMissionDao()
        if (apiResult.isNotEmpty()) {
            if (canCleanDatabase) {
                scienceMissionDao.clearAll(observationPlace.id)
            }

            scienceMissionDao.insertAll(apiResult)

            // clean up ephemeris data for mission which no longer exist
            // not great as we'll clean data from possible next request
            database.getEphemerisDataDao()
                .clearEphemerisData(
                    apiResult.map { it.missionKey },
                    observationPlace.id
                )

            // get all comet data async if needed
            apiResult.filter { it.getMissionType() == ScienceMissionType.CometaryActivity || it.getMissionType() == ScienceMissionType.PlanetaryDefense }
                .map {
                    CoroutineScope(Dispatchers.IO).async {
                        if (it.getMissionType() == ScienceMissionType.CometaryActivity) {
                            getCometEphemerisData(it)
                        } else {
                            // should be PlanetaryDefense type
                            getPlanetaryDefenseData(it)
                        }
                    }
                }
        }

        // return true if API call is succeed
        return apiResult.isNotEmpty()
    }

    @OptIn(ExperimentalTime::class)
    suspend fun getCometEphemerisData(
        cometScienceMission: ScienceMission
    ) : List<EphemerisData> {
        val start = kotlin.time.Clock.System.now()
        val ephemerisDao = database.getEphemerisDataDao()

        val result = ephemerisDao.getAllEphemerisData(
            missionKey = cometScienceMission.missionKey,
            observationPlaceID = cometScienceMission.observationPlaceID,
            timestamp = start.toEpochMilliseconds()
        )

        if (result.isNotEmpty()) return result

        // no result => clear old data if any
        ephemerisDao.clearAll(
            cometScienceMission.missionKey,
            cometScienceMission.observationPlaceID
        )

        // get new data from API
        val scienceAPI = ScienceAPI()
        val apiResult = scienceAPI.getCometMissionsEphemeris(
            scienceMission = cometScienceMission,
            fromStartDateTime = getCurrentDateAndTime(true)
        )

        if (apiResult.isNotEmpty()) {
            // insert new data in DB if any
            ephemerisDao.insertAll(apiResult)
        }

        return apiResult
    }

    @OptIn(ExperimentalTime::class)
    suspend fun getPlanetaryDefenseData(
        planetaryDefenseScienceMission: ScienceMission
    ) : List<EphemerisData> {
        val start = kotlin.time.Clock.System.now()
        val ephemerisDao = database.getEphemerisDataDao()

        val result = ephemerisDao.getAllEphemerisData(
            missionKey = planetaryDefenseScienceMission.missionKey,
            observationPlaceID = planetaryDefenseScienceMission.observationPlaceID,
            timestamp = start.toEpochMilliseconds()
        )

        if (result.isNotEmpty()) return result

        // no result => clear old data if any
        ephemerisDao.clearAll(
            planetaryDefenseScienceMission.missionKey,
            planetaryDefenseScienceMission.observationPlaceID
        )

        // get new data from API
        val scienceAPI = ScienceAPI()
        val apiResult = scienceAPI.getPlanetaryDefenseMissionsEphemeris(
            scienceMission = planetaryDefenseScienceMission,
            fromStartDateTime = getCurrentDateAndTime(true)
        )

        if (apiResult.isNotEmpty()) {
            // insert new data in DB if any
            ephemerisDao.insertAll(apiResult)
        }

        return apiResult
    }
}