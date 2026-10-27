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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** Range of local start dates kept by the mission list filter. */
enum class DateRange(val dayCount: Int?) {
    All(null),
    Today(1),
    Next3Days(3)
}

/**
 * Filter applied to the mission list. An empty [types] set means every type is shown,
 * [priorityOnly] keeps only priority missions and [dateRange] restricts the local start date.
 */
data class MissionFilter(
    val types: Set<ScienceMissionType> = emptySet(),
    val priorityOnly: Boolean = false,
    val dateRange: DateRange = DateRange.All
) {
    val isActive: Boolean get() = types.isNotEmpty() || priorityOnly || dateRange != DateRange.All

    /** Returns a copy of this filter with [type] added if absent, removed otherwise. */
    fun toggle(type: ScienceMissionType): MissionFilter =
        copy(types = if (type in types) types - type else types + type)
}

data class MissionTypeCount(
    val type: ScienceMissionType,
    val count: Int
)

/**
 * Keeps the [missions] matching every criterion of [filter], preserving their order.
 * All missions are kept when the filter is not active.
 *
 * The date range is evaluated on the same local date as the mission list sections
 * (see [groupMissionsByDay]): ongoing comet and planetary defense missions count as [today].
 */
fun filterMissions(
    missions: List<ScienceMission>,
    filter: MissionFilter,
    today: String,
    nowEpochMillis: Long
): List<ScienceMission> {
    if (!filter.isActive) return missions

    // ISO dates compare lexicographically in chronological order.
    val lastDay = filter.dateRange.dayCount?.let { dayCount ->
        LocalDate.parse(today).plus(dayCount - 1, DateTimeUnit.DAY).toString()
    }

    return missions.filter { mission ->
        (filter.types.isEmpty() || mission.getMissionType() in filter.types)
                && (!filter.priorityOnly || mission.priority)
                && (lastDay == null || missionSectionDate(mission, today, nowEpochMillis) in today..lastDay)
    }
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
