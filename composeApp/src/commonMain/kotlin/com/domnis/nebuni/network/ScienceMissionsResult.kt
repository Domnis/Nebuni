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

package com.domnis.nebuni.network

import com.domnis.nebuni.data.ScienceMission

/** Why a science API call gave no usable answer. */
enum class ScienceApiFailure {
    /** The request failed or the server answered with a non-200 status. */
    Network,

    /** The server answered 200 but the body is not a JSON object (blank, HTML, "0", array...). */
    MalformedResponse
}

/** Answer of [ScienceAPI.listScienceMissions]: an empty [Success] is a real "no mission" answer. */
sealed interface ScienceMissionsResult {
    data class Success(val missions: List<ScienceMission>) : ScienceMissionsResult
    data class Failure(val reason: ScienceApiFailure) : ScienceMissionsResult
}
