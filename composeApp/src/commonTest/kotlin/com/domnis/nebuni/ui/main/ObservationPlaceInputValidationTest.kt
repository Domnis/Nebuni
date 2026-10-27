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

import com.domnis.nebuni.ui.main.ObservationPlaceInputError.AltitudeMaxFormat
import com.domnis.nebuni.ui.main.ObservationPlaceInputError.AltitudeMaxRange
import com.domnis.nebuni.ui.main.ObservationPlaceInputError.AltitudeMinFormat
import com.domnis.nebuni.ui.main.ObservationPlaceInputError.AltitudeMinRange
import com.domnis.nebuni.ui.main.ObservationPlaceInputError.AzimuthMaxFormat
import com.domnis.nebuni.ui.main.ObservationPlaceInputError.AzimuthMaxRange
import com.domnis.nebuni.ui.main.ObservationPlaceInputError.AzimuthMinFormat
import com.domnis.nebuni.ui.main.ObservationPlaceInputError.AzimuthMinRange
import com.domnis.nebuni.ui.main.ObservationPlaceInputError.LatitudeFormat
import com.domnis.nebuni.ui.main.ObservationPlaceInputError.LatitudeRange
import com.domnis.nebuni.ui.main.ObservationPlaceInputError.LongitudeFormat
import com.domnis.nebuni.ui.main.ObservationPlaceInputError.LongitudeRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ObservationPlaceInputValidationTest {

    private fun validate(
        name: String = "Home",
        latitude: String = "43.28",
        longitude: String = "5.37",
        altMin: String = "0",
        altMax: String = "90",
        azMin: String = "0",
        azMax: String = "360",
    ) = validateObservationPlaceInput(name, latitude, longitude, altMin, altMax, azMin, azMax)

    private fun assertError(expected: ObservationPlaceInputError, result: ObservationPlaceInputResult) {
        assertEquals(ObservationPlaceInputResult.Invalid(expected), result)
    }

    @Test
    fun validInputBuildsTrimmedPlace() {
        val result = validate(
            name = "  Home - Balcony  ",
            latitude = " 43.284055 ",
            longitude = " 5.371309 ",
            altMin = " 10 ",
            altMax = " 80 ",
            azMin = " 20 ",
            azMax = " 300 ",
        )

        val place = assertIs<ObservationPlaceInputResult.Valid>(result).place
        assertEquals("Home - Balcony", place.name)
        assertEquals(43.284055, place.latitude)
        assertEquals(5.371309, place.longitude)
        assertEquals(10, place.altMin)
        assertEquals(80, place.altMax)
        assertEquals(20, place.azMin)
        assertEquals(300, place.azMax)
    }

    @Test
    fun decimalCommaIsAccepted() {
        val place = assertIs<ObservationPlaceInputResult.Valid>(validate(latitude = "43,5", longitude = "-5,25")).place
        assertEquals(43.5, place.latitude)
        assertEquals(-5.25, place.longitude)
    }

    @Test
    fun latitudeFormat() {
        assertError(LatitudeFormat, validate(latitude = ""))
        assertError(LatitudeFormat, validate(latitude = "   "))
        assertError(LatitudeFormat, validate(latitude = "abc"))
    }

    @Test
    fun latitudeRange() {
        assertIs<ObservationPlaceInputResult.Valid>(validate(latitude = "-90"))
        assertIs<ObservationPlaceInputResult.Valid>(validate(latitude = "90"))
        assertError(LatitudeRange, validate(latitude = "-90.01"))
        assertError(LatitudeRange, validate(latitude = "90,01"))
    }

    @Test
    fun longitudeFormat() {
        assertError(LongitudeFormat, validate(longitude = ""))
        assertError(LongitudeFormat, validate(longitude = "east"))
    }

    @Test
    fun longitudeRange() {
        assertIs<ObservationPlaceInputResult.Valid>(validate(longitude = "-180"))
        assertIs<ObservationPlaceInputResult.Valid>(validate(longitude = "180"))
        assertError(LongitudeRange, validate(longitude = "-180.5"))
        assertError(LongitudeRange, validate(longitude = "180.5"))
    }

    @Test
    fun altitudeFormat() {
        assertError(AltitudeMinFormat, validate(altMin = ""))
        // Altitudes are whole degrees: a decimal value is a format error.
        assertError(AltitudeMinFormat, validate(altMin = "10,5"))
        assertError(AltitudeMaxFormat, validate(altMax = "x"))
        assertError(AltitudeMaxFormat, validate(altMax = "80.5"))
    }

    @Test
    fun altitudeRange() {
        assertIs<ObservationPlaceInputResult.Valid>(validate(altMin = "0", altMax = "90"))
        assertError(AltitudeMinRange, validate(altMin = "-1"))
        assertError(AltitudeMinRange, validate(altMin = "91", altMax = "90"))
        assertError(AltitudeMaxRange, validate(altMax = "-1"))
        assertError(AltitudeMaxRange, validate(altMax = "91"))
    }

    @Test
    fun altitudeMaxMustNotBeBelowMin() {
        assertIs<ObservationPlaceInputResult.Valid>(validate(altMin = "45", altMax = "45"))
        assertError(AltitudeMaxRange, validate(altMin = "45", altMax = "44"))
    }

    @Test
    fun azimuthFormat() {
        assertError(AzimuthMinFormat, validate(azMin = ""))
        assertError(AzimuthMinFormat, validate(azMin = "12.5"))
        assertError(AzimuthMaxFormat, validate(azMax = "north"))
        assertError(AzimuthMaxFormat, validate(azMax = "300,5"))
    }

    @Test
    fun azimuthRange() {
        assertIs<ObservationPlaceInputResult.Valid>(validate(azMin = "0", azMax = "360"))
        assertError(AzimuthMinRange, validate(azMin = "-1"))
        assertError(AzimuthMinRange, validate(azMin = "361", azMax = "360"))
        assertError(AzimuthMaxRange, validate(azMax = "-1"))
        assertError(AzimuthMaxRange, validate(azMax = "361"))
    }

    @Test
    fun azimuthMaxMustNotBeBelowMin() {
        assertIs<ObservationPlaceInputResult.Valid>(validate(azMin = "180", azMax = "180"))
        assertError(AzimuthMaxRange, validate(azMin = "180", azMax = "179"))
    }

    @Test
    fun rulesAreCheckedInFormOrder() {
        assertError(LatitudeFormat, validate(latitude = "", longitude = "", altMin = "", azMin = ""))
        assertError(LongitudeRange, validate(longitude = "200", altMin = "-1", azMin = "-1"))
        // Both altitude formats are checked before altitude ranges.
        assertError(AltitudeMaxFormat, validate(altMin = "-1", altMax = ""))
        assertError(AltitudeMaxRange, validate(altMax = "91", azMin = ""))
        assertError(AzimuthMaxFormat, validate(azMin = "-1", azMax = ""))
    }

    @Test
    fun everyErrorMapsToADistinctMessage() {
        val messages = ObservationPlaceInputError.entries.map { it.message }
        assertEquals(messages.size, messages.toSet().size)
    }
}
