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
package com.domnis.nebuni

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class UtilsTest {
    @Test
    fun parseDateToInstantUsesUtcByDefault() {
        assertEquals(1743292800000L, parseDateToInstant("2025-03-30").toEpochMilliseconds())
    }

    @Test
    fun parseDateToInstantRespectsTimeZone() {
        val paris = TimeZone.of("Europe/Paris")

        assertEquals(
            kotlin.time.Instant.parse("2025-03-29T23:00:00Z"),
            parseDateToInstant("2025-03-30", paris)
        )
    }

    @Test
    fun parseDateToInstantAcrossDstChange() {
        val paris = TimeZone.of("Europe/Paris")
        val start = parseDateToInstant("2025-03-30", paris)
        val next = parseDateToInstant("2025-03-31", paris)

        // Spring-forward day only lasts 23 hours.
        assertEquals(23.hours, next - start)
    }

    @Test
    fun parseDateToInstantRejectsMalformedDates() {
        assertFails { parseDateToInstant("not-a-date") }
        assertFails { parseDateToInstant("2025-02-30") }
        assertFails { parseDateToInstant("") }
    }

    @Test
    fun displayDateTimeFormatOmitsZeroSeconds() {
        assertEquals("2025-12-31 23:30", LocalDateTime(2025, 12, 31, 23, 30, 0).format(customDisplayDateTimeFormat()))
        assertEquals("2025-12-31 23:30:15", LocalDateTime(2025, 12, 31, 23, 30, 15).format(customDisplayDateTimeFormat()))
    }

    @Test
    fun displayDateFormatIsIso() {
        assertEquals("2026-01-01", LocalDateTime(2026, 1, 1, 0, 5, 0).format(customDisplayDateFormat()))
    }

    @Test
    fun displayDateTimeFormatRoundTripsAtMidnight() {
        assertEquals("2026-01-01 00:00", LocalDateTime(2026, 1, 1, 0, 0, 0).format(customDisplayDateTimeFormat()))
    }

    @Test
    fun convertToLocalTimeZoneMatchesSystemZone() {
        val input = "2025-12-31T23:30:15"
        val expected = kotlin.time.Instant.parse("2025-12-31T23:30:15Z")
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(customDisplayDateTimeFormat())

        assertEquals(expected, convertCurrentDateAndTimeToLocalTimeZone(input))
    }

    @Test
    fun convertToLocalTimeZoneAcceptsMissingSecondsAndFraction() {
        val expectedNoSeconds = kotlin.time.Instant.parse("2025-03-30T01:30:00Z")
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(customDisplayDateTimeFormat())

        assertEquals(expectedNoSeconds, convertCurrentDateAndTimeToLocalTimeZone("2025-03-30T01:30"))
        assertEquals(expectedNoSeconds, convertCurrentDateAndTimeToLocalTimeZone("2025-03-30t01:30:00.000"))
    }

    @Test
    fun convertToLocalTimeZoneRejectsMalformedInput() {
        assertFails { convertCurrentDateAndTimeToLocalTimeZone("garbage") }
        assertFails { convertCurrentDateAndTimeToLocalTimeZone("2025-13-01T00:00") }
    }

    @Test
    fun currentDateHelpersProduceExpectedShapes() {
        assertEquals(10, getCurrentDate().length)
        LocalDate.parse(getCurrentDate())
        assertEquals(16, getCurrentDateAndTime().length)
        assertEquals(19, getCurrentDateAndTime(withSecond = true).length)
        assertEquals(16, getCurrentDateAndTimeWithOffset(1).length)
    }
}
