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
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.DateTimeComponents.Companion.Format
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.alternativeParsing
import kotlinx.datetime.format.char
import kotlinx.datetime.format.optional
import kotlinx.datetime.parse
import kotlinx.datetime.toLocalDateTime
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

@OptIn(ExperimentalTime::class)
fun getCurrentDate(
    timeZone: TimeZone = TimeZone.UTC
) : String {
    val now = kotlin.time.Clock.System.now()
    return now.toLocalDateTime(timeZone).format(LocalDateTime.Format {
        date(LocalDate.Formats.ISO)
    })
}
@OptIn(ExperimentalTime::class)
fun getCurrentDateAndTime(withSecond: Boolean = false) : String {
    val now = kotlin.time.Clock.System.now()
    val zone = TimeZone.UTC
    return now.toLocalDateTime(zone).format(LocalDateTime.Format {
        date(LocalDate.Formats.ISO)
        char('T')
        hour(); char(':'); minute()
        if (withSecond) {
            char(':'); second()
        }
    })
}

@OptIn(ExperimentalTime::class)
fun getCurrentDateAndTimeWithOffset(offsetInDay: Int) : String {
    val now = kotlin.time.Clock.System.now().plus(offsetInDay.toDuration(DurationUnit.DAYS))
    val zone = TimeZone.UTC
    return now.toLocalDateTime(zone).format(LocalDateTime.Format {
        date(LocalDate.Formats.ISO)
        char('T')
        hour(); char(':'); minute()
    })
}

@OptIn(ExperimentalTime::class)
fun convertCurrentDateAndTimeToLocalTimeZone(currentDateAndTime: String): String {
    return kotlin.time.Instant.parse(
        currentDateAndTime,
        Format {
            date(LocalDate.Formats.ISO)
            alternativeParsing({
                char('t')
            }) {
                char('T')
            }
            hour(); char(':'); minute()
            optional {
                char(':')
                second()
            }
            optional {
                char('.')
                secondFraction(1, 9)
            }
        }
    )
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .format(customDisplayDateTimeFormat())
}

fun customInstantParseFormat(): DateTimeFormat<DateTimeComponents> {
    return Format {
        date(LocalDate.Formats.ISO)
        alternativeParsing({
            char('t')
        }) {
            char('T')
        }
        hour(); char(':'); minute(); char(':'); second()
        optional {
            char('.')
            secondFraction(1, 9)
        }
    }
}

@OptIn(ExperimentalTime::class)
fun parseDateToInstant(
    dateString: String,
    timeZone: TimeZone = TimeZone.UTC
): kotlin.time.Instant { //use to parse "YYYY-MM-DD" date format
    val localDate = LocalDate.parse(dateString)
    return localDate.atStartOfDayIn(timeZone)
}

fun customDisplayDateTimeFormat(): DateTimeFormat<LocalDateTime> {
    return LocalDateTime.Format {
        date(LocalDate.Formats.ISO)
        char(' ')
        hour(); char(':'); minute()
        optional {
            char(':'); second()
        }
    }
}

fun customDisplayDateFormat(): DateTimeFormat<LocalDateTime> {
    return LocalDateTime.Format {
        date(LocalDate.Formats.ISO)
    }
}