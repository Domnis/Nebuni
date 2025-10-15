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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.domnis.nebuni.data.EphemerisData

@Dao
interface EphemerisDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<EphemerisData>)

    @Query("DELETE FROM EphemerisData WHERE missionKey = :missionKey AND observationPlaceID = :observationPlaceID")
    suspend fun clearAll(missionKey: String, observationPlaceID: String)

    @Query("DELETE FROM EphemerisData WHERE missionKey NOT IN (:missionKeys) AND observationPlaceID = :observationPlaceID")
    suspend fun clearEphemerisData(missionKeys: List<String>, observationPlaceID: String)

    @Query("SELECT * FROM EphemerisData WHERE missionKey = :missionKey AND observationPlaceID = :observationPlaceID AND timestamp > :timestamp")
    suspend fun getAllEphemerisData(missionKey: String, observationPlaceID: String, timestamp: Long): List<EphemerisData>
}