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
import com.domnis.nebuni.data.ScienceMission
import kotlinx.coroutines.flow.Flow

@Dao
interface ScienceMissionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ScienceMission)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ScienceMission>)

    @Query("DELETE FROM ScienceMission WHERE observationPlaceID = :observationPlaceID")
    suspend fun clearAll(observationPlaceID: String)

    @Query("SELECT count(*) FROM ScienceMission WHERE observationPlaceID = :observationPlaceID")
    suspend fun count(observationPlaceID: String): Int

    @Query("SELECT * FROM ScienceMission WHERE observationPlaceID = :observationPlaceID")
    fun getAllAsFlow(observationPlaceID: String): Flow<List<ScienceMission>>
}