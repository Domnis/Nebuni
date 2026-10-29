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
        assertEquals(missions, filterMissions(missions, MissionFilter(), TODAY, NOW))
    }

    @Test
    fun singleTypeKeepsOnlyThatTypeInOrder() {
        val filtered = filterMissions(
            missions,
            MissionFilter(setOf(ScienceMissionType.AsteroidOccultation)),
            TODAY,
            NOW
        )

        assertEquals(listOf("occultation-1", "occultation-2"), filtered.keys())
    }

    @Test
    fun multipleTypesKeepUnionInOriginalOrder() {
        val filtered = filterMissions(
            missions,
            MissionFilter(setOf(ScienceMissionType.Satellite, ScienceMissionType.CometaryActivity)),
            TODAY,
            NOW
        )

        assertEquals(listOf("comet-1", "satellite-1"), filtered.keys())
    }

    @Test
    fun typeWithoutMissionGivesEmptyResult() {
        val filtered = filterMissions(
            missions,
            MissionFilter(setOf(ScienceMissionType.PlanetaryDefense)),
            TODAY,
            NOW
        )

        assertTrue(filtered.isEmpty())
    }

    @Test
    fun emptyInputGivesEmptyResult() {
        assertTrue(filterMissions(emptyList(), MissionFilter(setOf(ScienceMissionType.Satellite)), TODAY, NOW).isEmpty())
        assertTrue(filterMissions(emptyList(), MissionFilter(), TODAY, NOW).isEmpty())
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
            filterMissions(dated, MissionFilter(setOf(ScienceMissionType.AsteroidOccultation)), TODAY, NOW),
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

    // Date-only start dates are used as-is as the local start date, which keeps these tests
    // independent from the time zone of the machine running them.
    private val scheduled = listOf(
        ScienceMission(missionKey = "o-yesterday", pipeline_type = "o", tstart = "2026-10-27", priority = true),
        ScienceMission(missionKey = "c-ongoing", pipeline_type = "c", tstart = "2026-10-20"),
        ScienceMission(missionKey = "p-ongoing", pipeline_type = "p", tstart = "2026-10-25", priority = true),
        ScienceMission(missionKey = "o-today", pipeline_type = "o", tstart = "2026-10-28", priority = true),
        ScienceMission(missionKey = "e-today", pipeline_type = "e", tstart = "2026-10-28"),
        ScienceMission(missionKey = "o-day2", pipeline_type = "o", tstart = "2026-10-29"),
        ScienceMission(missionKey = "e-day3", pipeline_type = "e", tstart = "2026-10-30", priority = true),
        ScienceMission(missionKey = "o-day4", pipeline_type = "o", tstart = "2026-10-31", priority = true),
        ScienceMission(missionKey = "c-future", pipeline_type = "c", tstart = "2026-11-02"),
    )

    @Test
    fun priorityOnlyKeepsOnlyPriorityMissions() {
        val filtered = filterMissions(scheduled, MissionFilter(priorityOnly = true), TODAY, NOW)

        assertEquals(listOf("o-yesterday", "p-ongoing", "o-today", "e-day3", "o-day4"), filtered.keys())
    }

    @Test
    fun allDateRangeKeepsEveryDate() {
        assertEquals(scheduled, filterMissions(scheduled, MissionFilter(dateRange = DateRange.All), TODAY, NOW))
    }

    @Test
    fun todayKeepsMissionsStartingTodayAndOngoingLongRunningMissions() {
        val filtered = filterMissions(scheduled, MissionFilter(dateRange = DateRange.Today), TODAY, NOW)

        assertEquals(listOf("c-ongoing", "p-ongoing", "o-today", "e-today"), filtered.keys())
    }

    @Test
    fun next3DaysKeepsTodayAndTheTwoFollowingDays() {
        val filtered = filterMissions(scheduled, MissionFilter(dateRange = DateRange.Next3Days), TODAY, NOW)

        assertEquals(
            listOf("c-ongoing", "p-ongoing", "o-today", "e-today", "o-day2", "e-day3"),
            filtered.keys()
        )
    }

    @Test
    fun todayFilterMatchesTodaySection() {
        val todayMissions = groupMissionsByDay(scheduled, TODAY, NOW)
            .filter { it.isToday }
            .flatMap { it.missions }

        val sections = groupMissionsByDay(
            filterMissions(scheduled, MissionFilter(dateRange = DateRange.Today), TODAY, NOW),
            TODAY,
            NOW
        )

        assertTrue(sections.all { it.isToday })
        assertEquals(todayMissions.keys(), sections.flatMap { it.missions }.keys())
    }

    @Test
    fun allCriteriaCombine() {
        val filtered = filterMissions(
            scheduled,
            MissionFilter(
                types = setOf(ScienceMissionType.AsteroidOccultation, ScienceMissionType.PlanetaryDefense),
                priorityOnly = true,
                dateRange = DateRange.Next3Days
            ),
            TODAY,
            NOW
        )

        assertEquals(listOf("p-ongoing", "o-today"), filtered.keys())
    }

    @Test
    fun typeAndDateRangeCombine() {
        val filtered = filterMissions(
            scheduled,
            MissionFilter(types = setOf(ScienceMissionType.ExoplanetTransit), dateRange = DateRange.Today),
            TODAY,
            NOW
        )

        assertEquals(listOf("e-today"), filtered.keys())
    }

    @Test
    fun typeAndPriorityCombine() {
        val filtered = filterMissions(
            scheduled,
            MissionFilter(types = setOf(ScienceMissionType.ExoplanetTransit), priorityOnly = true),
            TODAY,
            NOW
        )

        assertEquals(listOf("e-day3"), filtered.keys())
    }

    @Test
    fun priorityAndDateRangeMakeFilterActive() {
        assertTrue(MissionFilter(priorityOnly = true).isActive)
        assertTrue(MissionFilter(dateRange = DateRange.Today).isActive)
        assertFalse(MissionFilter(dateRange = DateRange.All).isActive)
    }

    private companion object {
        const val TODAY = "2026-10-28"

        // 2026-10-28T12:00:00Z
        const val NOW = 1_793_188_800_000L
    }
}
