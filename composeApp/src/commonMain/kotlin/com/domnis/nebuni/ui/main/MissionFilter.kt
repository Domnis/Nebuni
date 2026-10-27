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

package com.domnis.nebuni.ui.main

import com.domnis.nebuni.data.ScienceMission
import com.domnis.nebuni.data.ScienceMissionType

/**
 * Filter applied to the mission list. An empty [types] set means every type is shown.
 */
data class MissionFilter(val types: Set<ScienceMissionType> = emptySet()) {
    val isActive: Boolean get() = types.isNotEmpty()

    /** Returns a copy of this filter with [type] added if absent, removed otherwise. */
    fun toggle(type: ScienceMissionType): MissionFilter =
        copy(types = if (type in types) types - type else types + type)
}

data class MissionTypeCount(
    val type: ScienceMissionType,
    val count: Int
)

/**
 * Keeps the [missions] whose type is in [filter], preserving their order.
 * All missions are kept when the filter is not active.
 */
fun filterMissions(missions: List<ScienceMission>, filter: MissionFilter): List<ScienceMission> {
    if (!filter.isActive) return missions
    return missions.filter { it.getMissionType() in filter.types }
}

/**
 * Lists the mission types present in [missions] with their number of missions,
 * ordered by [ScienceMissionType] declaration order.
 */
fun countMissionTypes(missions: List<ScienceMission>): List<MissionTypeCount> =
    missions
        .groupingBy { it.getMissionType() }
        .eachCount()
        .map { (type, count) -> MissionTypeCount(type, count) }
        .sortedBy { it.type.ordinal }
