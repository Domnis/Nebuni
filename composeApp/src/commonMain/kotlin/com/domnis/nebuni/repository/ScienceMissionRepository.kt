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

import com.domnis.nebuni.data.ObservationPlace
import com.domnis.nebuni.data.ScienceMission
import com.domnis.nebuni.data.ScienceMissionType
import com.domnis.nebuni.database.AppDatabase
import com.domnis.nebuni.network.ScienceAPI
import kotlinx.coroutines.flow.Flow

class ScienceMissionRepository(private val database: AppDatabase) {
    fun getScienceMissionFor(observationPlace: ObservationPlace): Flow<List<ScienceMission>> {
        return database.getScienceMissionDao().getAllAsFlow(observationPlace.id)
    }

    suspend fun refreshScienceMissions(
        observationPlace: ObservationPlace,
        startTime: String,
        endDate: String
    ) {
        val apiResult = ScienceAPI().listScienceMissions(
            observationPlace = observationPlace,
            startDateTime = startTime,
            endDateTime = endDate
        ).filter { it.getMissionType() != ScienceMissionType.Unknown }

        val scienceMissionDao = database.getScienceMissionDao()
        if (apiResult.isNotEmpty()) {
            scienceMissionDao.clearAll(observationPlace.id)

            scienceMissionDao.insertAll(apiResult)
        }
    }
}