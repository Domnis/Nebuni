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
import androidx.room.Query
import com.domnis.nebuni.data.ObservationPlace
import kotlinx.coroutines.flow.Flow

@Dao
interface ObservationPlaceDao {
    @Insert
    suspend fun insert(item: ObservationPlace)

    @Query("SELECT count(*) FROM ObservationPlace")
    suspend fun count(): Int

    @Query("SELECT * FROM ObservationPlace")
    fun getAllAsFlow(): Flow<List<ObservationPlace>>
}