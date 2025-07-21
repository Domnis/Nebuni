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

package com.domnis.nebuni.data

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class ObservationPlace @OptIn(ExperimentalUuidApi::class) constructor(
    val id: String = Uuid.random().toString(),
    val name: String = "New Place 01",
    val latitude: Double = 43.284055, // Notre-Dame de la Garde's latitude
    val longitude: Double = 5.371309, // Notre-Dame de la Garde's longitude
    val altMin: Int = 0,
    val altMax: Int = 90,
    val azMin: Int = 0,
    val azMax: Int = 360
)