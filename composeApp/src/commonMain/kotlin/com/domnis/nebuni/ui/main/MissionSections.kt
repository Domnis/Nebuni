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

data class MissionSection(
    val date: String,
    val isToday: Boolean,
    val missions: List<ScienceMission>
)

/**
 * Sorts [missions] by start time and groups them into consecutive sections by local start date.
 *
 * Comet and planetary defense missions that have already started (start time not after
 * [nowEpochMillis]) are placed under [today], since they can still be observed.
 */
fun groupMissionsByDay(
    missions: List<ScienceMission>,
    today: String,
    nowEpochMillis: Long
): List<MissionSection> {
    val result = mutableListOf<MissionSection>()
    var currentDate = ""
    var currentList = mutableListOf<ScienceMission>()

    fun closeSection() {
        if (currentDate.isEmpty() || currentList.isEmpty()) return
        result.add(MissionSection(currentDate, currentDate == today, currentList))
    }

    missions
        .sortedBy { it.getMissionStartTimestamp() }
        .forEach { mission ->
            val startDate = missionSectionDate(mission, today, nowEpochMillis)

            if (startDate != currentDate) {
                closeSection()
                currentDate = startDate
                currentList = mutableListOf()
            }

            currentList.add(mission)
        }

    closeSection()

    return result
}

/**
 * Local start date (ISO `YYYY-MM-DD`) under which [mission] is listed: [today] for ongoing
 * comet and planetary defense missions, its own local start date otherwise.
 */
internal fun missionSectionDate(mission: ScienceMission, today: String, nowEpochMillis: Long): String =
    if (isOngoingLongRunningMission(mission, nowEpochMillis)) {
        today
    } else {
        mission.getMissionStartDateOnly()
    }

private fun isOngoingLongRunningMission(mission: ScienceMission, nowEpochMillis: Long): Boolean {
    val type = mission.getMissionType()
    return (type == ScienceMissionType.CometaryActivity || type == ScienceMissionType.PlanetaryDefense)
            && mission.getMissionStartTimestamp() <= nowEpochMillis
}
