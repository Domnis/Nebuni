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
import com.domnis.nebuni.database.ObservationPlaceDao
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class ObservationPlaceRepository(
    private val observationPlaceDao: ObservationPlaceDao,
    @OptIn(ExperimentalTime::class)
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    fun places(): Flow<List<ObservationPlace>> = observationPlaceDao.getAllAsFlow()

    /** The most recently selected place, or null when there is no place yet. */
    fun selectedPlace(): Flow<ObservationPlace?> = observationPlaceDao.getSelectedAsFlow()

    /** Inserts [place] and makes it the selected one. */
    suspend fun add(place: ObservationPlace) {
        observationPlaceDao.insert(place.copy(lastSelectedAt = now()))
    }

    suspend fun select(id: String) {
        observationPlaceDao.markSelected(id, now())
    }

    /**
     * Persists [place]. The cached missions and ephemerides are cleared when the coordinates or the
     * sky area changed; a name-only change keeps them. The stored selection date is preserved.
     */
    suspend fun update(place: ObservationPlace) {
        val previous = observationPlaceDao.getById(place.id)
        observationPlaceDao.update(
            if (previous == null) place else place.copy(lastSelectedAt = previous.lastSelectedAt)
        )
        if (previous != null && previous.hasDifferentSkyThan(place)) {
            observationPlaceDao.clearCache(place.id)
        }
    }

    /**
     * Deletes the place with its cache. Returns false, and deletes nothing, when it is the last place.
     */
    suspend fun delete(id: String): Boolean {
        if (observationPlaceDao.count() <= 1) return false
        observationPlaceDao.deleteWithCache(id)
        return true
    }

    private fun ObservationPlace.hasDifferentSkyThan(other: ObservationPlace): Boolean =
        latitude != other.latitude || longitude != other.longitude ||
            altMin != other.altMin || altMax != other.altMax ||
            azMin != other.azMin || azMax != other.azMax
}
