package com.domnis.nebuni.repository

import com.domnis.nebuni.data.ObservationPlace
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ObservationPlaceRepositoryTest {
    private val a = ObservationPlace(id = "a", name = "A", lastSelectedAt = 10)
    private val b = ObservationPlace(id = "b", name = "B", lastSelectedAt = 5)

    private fun repo(dao: FakeObservationPlaceDao, now: Long = 100) =
        ObservationPlaceRepository(dao) { now }

    @Test
    fun addSelectsNewPlace() = runTest {
        val dao = FakeObservationPlaceDao(listOf(a, b))
        val repository = repo(dao)
        repository.add(ObservationPlace(id = "c", name = "C"))
        assertEquals("c", repository.selectedPlace().first()?.id)
        assertEquals(100, dao.getById("c")?.lastSelectedAt)
    }

    @Test
    fun selectMarksPlaceWithClock() = runTest {
        val dao = FakeObservationPlaceDao(listOf(a, b))
        val repository = repo(dao)
        repository.select("b")
        assertEquals("b", repository.selectedPlace().first()?.id)
    }

    @Test
    fun placesAreListed() = runTest {
        val repository = repo(FakeObservationPlaceDao(listOf(b, a)))
        assertEquals(listOf("a", "b"), repository.places().first().map { it.id })
    }

    @Test
    fun nameOnlyUpdateKeepsCache() = runTest {
        val dao = FakeObservationPlaceDao(listOf(a, b))
        repo(dao).update(a.copy(name = "Renamed"))
        assertEquals("Renamed", dao.getById("a")?.name)
        assertEquals(listOf("update:a"), dao.operations)
    }

    @Test
    fun coordinatesOrSkyChangeClearsCache() = runTest {
        val changes = listOf<(ObservationPlace) -> ObservationPlace>(
            { it.copy(latitude = it.latitude + 1) },
            { it.copy(longitude = it.longitude + 1) },
            { it.copy(altMin = it.altMin + 1) },
            { it.copy(altMax = it.altMax - 1) },
            { it.copy(azMin = it.azMin + 1) },
            { it.copy(azMax = it.azMax - 1) },
        )
        for (change in changes) {
            val dao = FakeObservationPlaceDao(listOf(a, b))
            repo(dao).update(change(a))
            assertTrue("deleteMissionsOf:a" in dao.operations)
            assertTrue("deleteEphemeridesOf:a" in dao.operations)
        }
    }

    @Test
    fun updateKeepsSelectionDate() = runTest {
        val dao = FakeObservationPlaceDao(listOf(a, b))
        repo(dao).update(a.copy(name = "X", lastSelectedAt = 0))
        assertEquals(10, dao.getById("a")?.lastSelectedAt)
    }

    @Test
    fun deleteRefusesLastPlace() = runTest {
        val dao = FakeObservationPlaceDao(listOf(a))
        assertFalse(repo(dao).delete("a"))
        assertEquals(1, dao.count())
        assertTrue(dao.operations.isEmpty())
    }

    @Test
    fun deleteRemovesPlaceWithCache() = runTest {
        val dao = FakeObservationPlaceDao(listOf(a, b))
        assertTrue(repo(dao).delete("a"))
        assertEquals(listOf("b"), dao.places.value.map { it.id })
        assertTrue("deleteMissionsOf:a" in dao.operations)
    }
}
