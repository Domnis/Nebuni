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

package com.domnis.nebuni.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.domnis.nebuni.data.EphemerisData
import com.domnis.nebuni.data.ObservationPlace
import com.domnis.nebuni.data.ScienceMission
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObservationPlaceDaoTest {
    private fun newDb() = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()

    @Test
    fun deleteWithCacheRemovesPlaceAndItsCacheOnly() = runTest {
        val db = newDb()
        val places = db.getObservationPlaceDao()
        val missions = db.getScienceMissionDao()
        val ephemerides = db.getEphemerisDataDao()
        places.insert(ObservationPlace(id = "a", name = "A"))
        places.insert(ObservationPlace(id = "b", name = "B"))
        for (id in listOf("a", "b")) {
            missions.insert(ScienceMission(missionKey = "m", observationPlaceID = id))
            ephemerides.insertAll(listOf(EphemerisData(missionKey = "m", observationPlaceID = id, timestamp = 1)))
        }

        places.deleteWithCache("a")

        assertEquals(listOf("b"), places.getAllAsFlow().first().map { it.id })
        assertEquals(0, missions.count("a"))
        assertEquals(1, missions.count("b"))
        assertEquals(0, ephemerides.getAllEphemerisData("m", "a", 0).size)
        assertEquals(1, ephemerides.getAllEphemerisData("m", "b", 0).size)
        db.close()
    }

    @Test
    fun selectionAndOrdering() = runTest {
        val db = newDb()
        val places = db.getObservationPlaceDao()
        assertNull(places.getSelectedAsFlow().first())
        places.insert(ObservationPlace(id = "1", name = "zeta"))
        places.insert(ObservationPlace(id = "2", name = "Alpha"))
        assertEquals(listOf("2", "1"), places.getAllAsFlow().first().map { it.id })
        assertEquals("1", places.getSelectedAsFlow().first()?.id)
        assertEquals(0L, places.getSelectedAsFlow().first()?.lastSelectedAt)
        places.markSelected("2", 100)
        assertEquals("2", places.getSelectedAsFlow().first()?.id)
        places.update(places.getSelectedAsFlow().first()!!.copy(name = "Renamed"))
        assertEquals("Renamed", places.getSelectedAsFlow().first()?.name)
        db.close()
    }
}
