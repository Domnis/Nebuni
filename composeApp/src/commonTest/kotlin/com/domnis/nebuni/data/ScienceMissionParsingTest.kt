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

import com.domnis.nebuni.customDisplayDateTimeFormat
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ScienceMissionParsingTest {
    private val missionParser = SimpleScienceMissionJsonParser()
    private val ephemerisParser = SimpleEphemeridsJsonParser()

    private fun localDisplay(utc: String): String =
        kotlin.time.Instant.parse(utc)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(customDisplayDateTimeFormat())

    @Test
    fun parsesNominalMissions() {
        val json = """
            {
              "query": {"loc": "x"},
              "abc123": {
                "pipeline_type": "o", "target_name": "Asteroid X", "alt": 45, "az": 180,
                "priority": true, "tstart": "2025-01-01T23:30:00", "tend": "2025-01-02T00:10:00",
                "unknown_field": 42,
                "ephemeris_args": {"name": "C/2025 A1", "loc": "p"}
              },
              "def456": {"pipeline_type": "e", "target_name": "Planet Y"}
            }
        """.trimIndent()

        val missions = missionParser.parseJson(json, "place1")

        assertEquals(2, missions.size)
        val first = missions[0]
        assertEquals("abc123", first.missionKey)
        assertEquals("place1", first.observationPlaceID)
        assertEquals("Asteroid X", first.target_name)
        assertEquals(45, first.alt)
        assertEquals(true, first.priority)
        assertEquals("C/2025 A1", first.ephemeris_args?.name)
        assertEquals(ScienceMissionType.AsteroidOccultation, first.getMissionType())
        assertEquals(ScienceMissionType.ExoplanetTransit, missions[1].getMissionType())
    }

    @Test
    fun unknownMissionTypeIsReportedAsUnknownWithoutLink() {
        val missions = missionParser.parseJson("""{"k1": {"pipeline_type": "zzz"}}""", "p")

        assertEquals(1, missions.size)
        assertEquals(ScienceMissionType.Unknown, missions[0].getMissionType())
        assertNull(missions[0].getWebsiteEventLink())
    }

    @Test
    fun missingFieldsFallBackToDefaults() {
        val missions = missionParser.parseJson("""{"k1": {}}""", "p")

        assertEquals(1, missions.size)
        val mission = missions[0]
        assertEquals("k1", mission.missionKey)
        assertEquals("", mission.target_name)
        assertEquals(0, mission.alt)
        assertEquals(false, mission.priority)
        assertNull(mission.ephemeris_args)
    }

    @Test
    fun wrongFieldTypeYieldsPlaceholderMission() {
        val missions = missionParser.parseJson("""{"k1": {"alt": "not-a-number", "target_name": "X"}}""", "p")

        assertEquals(1, missions.size)
        assertEquals("k1", missions[0].missionKey)
        assertEquals("p", missions[0].observationPlaceID)
        assertEquals("", missions[0].target_name)
    }

    @Test
    fun nonObjectEntriesAreIgnored() {
        val missions = missionParser.parseJson("""{"query": {}, "a": 1, "b": "str", "c": {"pipeline_type": "s"}}""", "p")

        assertEquals(listOf("c"), missions.map { it.missionKey })
    }

    @Test
    fun invalidTopLevelJsonThrows() {
        assertFails { missionParser.parseJson("not json", "p") }
    }

    @Test
    fun websiteEventLinks() {
        assertEquals(
            "https://science.unistellar.com/events/occultations/k1",
            ScienceMission(missionKey = "k1", pipeline_type = "o").getWebsiteEventLink()
        )
        assertEquals(
            "https://science.unistellar.com/events/comets/C2025A1",
            ScienceMission(
                missionKey = "k1", pipeline_type = "c",
                ephemeris_args = EphemerisArgs(name = "C/2025 A1")
            ).getWebsiteEventLink()
        )
    }

    @Test
    fun missionDatesAreConvertedToLocalTimeZoneAcrossDayBoundary() {
        val mission = ScienceMission(tstart = "2025-12-31T23:30:00", tend = "2026-01-01T00:10:00")

        // The parser format has no offset, so the value is interpreted as UTC.
        assertEquals(localDisplay("2025-12-31T23:30:00Z"), mission.getMissionStartDate())
        assertEquals(localDisplay("2026-01-01T00:10:00Z"), mission.getMissionEndDate())
        assertEquals(kotlin.time.Instant.parse("2025-12-31T23:30:00Z").toEpochMilliseconds(), mission.getMissionStartTimestamp())
    }

    @Test
    fun dateOnlyValuesAreReturnedAsIs() {
        val mission = ScienceMission(tstart = "2025-03-30", tend = "2025-03-31")

        assertEquals("2025-03-30", mission.getMissionStartDate())
        assertEquals("2025-03-30", mission.getMissionStartDateOnly())
        assertEquals("2025-03-31", mission.getMissionEndDate())
        assertEquals(1743292800000L, mission.getMissionStartTimestamp())
    }

    @Test
    fun malformedMissionDateThrows() {
        assertFails { ScienceMission(tstart = "Tuesday").getMissionStartDate() }
        assertFails { ScienceMission(tstart = "2025-13-45T99:99:99").getMissionStartTimestamp() }
        assertFails { ScienceMission(tstart = "not-a-date").getMissionStartTimestamp() }
    }

    @Test
    fun parsesEphemerides() {
        val json = """
            {
              "loc": "somewhere",
              "2025-01-01T00:00:00": {"alt": 10, "az": 20, "is_visible": true, "ra": "1.5"},
              "2025-01-01T00:01:00": {"alt": 11}
            }
        """.trimIndent()

        val result = ephemerisParser.parseJson(json, "m1", "p1")

        assertEquals(2, result.size)
        assertEquals("m1", result[0].missionKey)
        assertEquals("p1", result[0].observationPlaceID)
        assertEquals("2025-01-01T00:00:00", result[0].date)
        assertEquals(1735689600000L, result[0].timestamp)
        assertEquals(10, result[0].alt)
        assertEquals(true, result[0].is_visible)
        assertEquals(1735689660000L, result[1].timestamp)
        assertEquals(false, result[1].is_visible)
        assertEquals("", result[1].ra)
    }

    @Test
    fun ephemerisEntriesWithMalformedDateAreSkipped() {
        val json = """{"garbage": {"alt": 1}, "2025-01-01T00:00:00": {"alt": 2}, "x": 3}"""

        val result = ephemerisParser.parseJson(json, "m", "p")

        assertEquals(listOf(2), result.map { it.alt })
    }

    @Test
    fun ephemerisParsedDateIsLocalized() {
        val data = EphemerisData(date = "2025-03-30T01:30:00")

        assertEquals(localDisplay("2025-03-30T01:30:00Z"), data.getParsedDate())
        assertEquals("2025-03-30", EphemerisData(date = "2025-03-30").getParsedDate())
        assertFails { EphemerisData(date = "Tbad").getParsedDate() }
    }
}
