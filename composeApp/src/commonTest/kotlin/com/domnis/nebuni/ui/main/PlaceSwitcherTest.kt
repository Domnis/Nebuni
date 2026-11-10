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

import com.domnis.nebuni.data.ScienceMissionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaceSwitcherTest {
    @Test
    fun formatsTheAbsoluteDegreesWithFourDecimals() {
        assertEquals("43.2841", formatAbsoluteDegrees(43.284055))
        assertEquals("5.3713", formatAbsoluteDegrees(5.371309))
        assertEquals("33.8688", formatAbsoluteDegrees(-33.86882))
        assertEquals("0.0500", formatAbsoluteDegrees(-0.05))
        assertEquals("180.0000", formatAbsoluteDegrees(179.99999))
    }

    @Test
    fun aPlaceWithoutMissionAndNoFilterShowsTheEmptyPlaceMessage() {
        assertTrue(isPlaceWithoutMissions(MissionFilter(), emptyList(), isLoadingMissions = false))
    }

    @Test
    fun theEmptyPlaceMessageIsHiddenWhileRefreshing() {
        assertFalse(isPlaceWithoutMissions(MissionFilter(), emptyList(), isLoadingMissions = true))
    }

    @Test
    fun theEmptyPlaceMessageIsHiddenWhenAFilterIsActiveOrMissionsExist() {
        assertFalse(isPlaceWithoutMissions(MissionFilter(priorityOnly = true), emptyList(), isLoadingMissions = false))
        assertFalse(
            isPlaceWithoutMissions(
                MissionFilter(),
                listOf(MissionTypeCount(ScienceMissionType.CometaryActivity, 1)),
                isLoadingMissions = false
            )
        )
    }
}
