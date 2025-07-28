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

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun getCurrentDateAndTime() : String {
    val now = Clock.System.now()
    val zone = TimeZone.UTC
    return now.toLocalDateTime(zone).format(LocalDateTime.Format {
        date(LocalDate.Formats.ISO)
        char('T')
        hour(); char(':'); minute()
    })
}

fun getCurrentDateAndTimeWithOffset(offsetInDay: Int) : String {
    val now = Clock.System.now().plus(offsetInDay.toDuration(DurationUnit.DAYS))
    val zone = TimeZone.UTC
    return now.toLocalDateTime(zone).format(LocalDateTime.Format {
        date(LocalDate.Formats.ISO)
        char('T')
        hour(); char(':'); minute()
    })
}