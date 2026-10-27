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
import com.domnis.nebuni.parseDateToInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class MissionSectionsTest {

    // Date-only tstart values are returned as-is by getMissionStartDateOnly, which keeps
    // these tests independent of the system time zone.
    private val today = "2026-10-27"
    private val now = parseDateToInstant(today).toEpochMilliseconds() + 12 * 3600 * 1000L

    private fun mission(key: String, tstart: String, pipelineType: String = "o") =
        ScienceMission(missionKey = key, pipeline_type = pipelineType, tstart = tstart)

    private fun List<MissionSection>.keys() = map { section -> section.missions.map { it.missionKey } }

    @Test
    fun emptyInputGivesNoSection() {
        assertTrue(groupMissionsByDay(emptyList(), today, now).isEmpty())
    }

    @Test
    fun missionsAreSortedByStartTime() {
        val sections = groupMissionsByDay(
            listOf(
                mission("c", "2026-10-29"),
                mission("a", "2026-10-28"),
                mission("b", "2026-10-28"),
            ),
            today,
            now
        )

        assertEquals(listOf("2026-10-28", "2026-10-29"), sections.map { it.date })
        assertEquals(listOf(listOf("a", "b"), listOf("c")), sections.keys())
    }

    @Test
    fun missionsAreGroupedAcrossDayBoundary() {
        val sections = groupMissionsByDay(
            listOf(
                mission("tomorrow", "2026-10-28"),
                mission("today", today),
            ),
            today,
            now
        )

        assertEquals(
            listOf(
                MissionSection(today, isToday = true, missions = listOf(mission("today", today))),
                MissionSection("2026-10-28", isToday = false, missions = listOf(mission("tomorrow", "2026-10-28"))),
            ),
            sections
        )
    }

    @Test
    fun alreadyStartedCometIsGroupedUnderToday() {
        val sections = groupMissionsByDay(
            listOf(
                mission("comet", "2026-10-20", pipelineType = "c"),
                mission("occultation", today),
            ),
            today,
            now
        )

        assertEquals(1, sections.size)
        assertEquals(today, sections.single().date)
        assertTrue(sections.single().isToday)
        assertEquals(listOf(listOf("comet", "occultation")), sections.keys())
    }

    @Test
    fun alreadyStartedPlanetaryDefenseIsGroupedUnderToday() {
        val sections = groupMissionsByDay(
            listOf(mission("defense", "2026-10-25", pipelineType = "p")),
            today,
            now
        )

        assertEquals(listOf(MissionSection(today, isToday = true, missions = listOf(mission("defense", "2026-10-25", "p")))), sections)
    }

    @Test
    fun pastNonCometMissionKeepsItsOwnDate() {
        val sections = groupMissionsByDay(
            listOf(mission("occultation", "2026-10-25")),
            today,
            now
        )

        assertEquals(listOf("2026-10-25"), sections.map { it.date })
        assertEquals(false, sections.single().isToday)
    }

    @Test
    fun futureCometIsGroupedUnderItsOwnDate() {
        val sections = groupMissionsByDay(
            listOf(
                mission("comet", "2026-10-30", pipelineType = "c"),
                mission("occultation", today),
            ),
            today,
            now
        )

        assertEquals(listOf(today, "2026-10-30"), sections.map { it.date })
        assertEquals(listOf(true, false), sections.map { it.isToday })
        assertEquals(listOf(listOf("occultation"), listOf("comet")), sections.keys())
    }
}
