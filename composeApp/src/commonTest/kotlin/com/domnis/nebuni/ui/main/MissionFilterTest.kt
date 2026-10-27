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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MissionFilterTest {

    private fun mission(key: String, pipelineType: String) =
        ScienceMission(missionKey = key, pipeline_type = pipelineType)

    private val missions = listOf(
        mission("occultation-1", "o"),
        mission("comet-1", "c"),
        mission("exoplanet-1", "e"),
        mission("occultation-2", "o"),
        mission("satellite-1", "s"),
    )

    private fun List<ScienceMission>.keys() = map { it.missionKey }

    @Test
    fun emptyFilterKeepsAllMissions() {
        assertEquals(missions, filterMissions(missions, MissionFilter()))
    }

    @Test
    fun singleTypeKeepsOnlyThatTypeInOrder() {
        val filtered = filterMissions(
            missions,
            MissionFilter(setOf(ScienceMissionType.AsteroidOccultation))
        )

        assertEquals(listOf("occultation-1", "occultation-2"), filtered.keys())
    }

    @Test
    fun multipleTypesKeepUnionInOriginalOrder() {
        val filtered = filterMissions(
            missions,
            MissionFilter(setOf(ScienceMissionType.Satellite, ScienceMissionType.CometaryActivity))
        )

        assertEquals(listOf("comet-1", "satellite-1"), filtered.keys())
    }

    @Test
    fun typeWithoutMissionGivesEmptyResult() {
        val filtered = filterMissions(
            missions,
            MissionFilter(setOf(ScienceMissionType.PlanetaryDefense))
        )

        assertTrue(filtered.isEmpty())
    }

    @Test
    fun emptyInputGivesEmptyResult() {
        assertTrue(filterMissions(emptyList(), MissionFilter(setOf(ScienceMissionType.Satellite))).isEmpty())
        assertTrue(filterMissions(emptyList(), MissionFilter()).isEmpty())
    }

    @Test
    fun filteredMissionsKeepDateSections() {
        val dated = listOf(
            ScienceMission(missionKey = "o-day1", pipeline_type = "o", tstart = "2026-10-28"),
            ScienceMission(missionKey = "e-day1", pipeline_type = "e", tstart = "2026-10-28"),
            ScienceMission(missionKey = "o-day2", pipeline_type = "o", tstart = "2026-10-29"),
            ScienceMission(missionKey = "e-day3", pipeline_type = "e", tstart = "2026-10-30"),
        )

        val sections = groupMissionsByDay(
            filterMissions(dated, MissionFilter(setOf(ScienceMissionType.AsteroidOccultation))),
            today = "2026-10-27",
            nowEpochMillis = 0L
        )

        assertEquals(listOf("2026-10-28", "2026-10-29"), sections.map { it.date })
        assertEquals(listOf(listOf("o-day1"), listOf("o-day2")), sections.map { it.missions.keys() })
    }

    @Test
    fun toggleAddsThenRemovesType() {
        val selected = MissionFilter().toggle(ScienceMissionType.Satellite)
        assertEquals(setOf(ScienceMissionType.Satellite), selected.types)
        assertTrue(selected.isActive)

        val cleared = selected.toggle(ScienceMissionType.Satellite)
        assertEquals(MissionFilter(), cleared)
        assertFalse(cleared.isActive)
    }

    @Test
    fun countMissionTypesListsOnlyPresentTypesWithCounts() {
        assertEquals(
            listOf(
                MissionTypeCount(ScienceMissionType.AsteroidOccultation, 2),
                MissionTypeCount(ScienceMissionType.CometaryActivity, 1),
                MissionTypeCount(ScienceMissionType.ExoplanetTransit, 1),
                MissionTypeCount(ScienceMissionType.Satellite, 1),
            ),
            countMissionTypes(missions)
        )
    }

    @Test
    fun countMissionTypesOfEmptyListIsEmpty() {
        assertTrue(countMissionTypes(emptyList()).isEmpty())
    }
}
