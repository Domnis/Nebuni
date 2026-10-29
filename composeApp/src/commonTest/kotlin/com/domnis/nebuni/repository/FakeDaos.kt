package com.domnis.nebuni.repository

import com.domnis.nebuni.data.EphemerisData
import com.domnis.nebuni.data.ScienceMission
import com.domnis.nebuni.database.EphemerisDataDao
import com.domnis.nebuni.database.ScienceMissionDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [ScienceMissionDao]; [operations] records the write calls in order. */
class FakeScienceMissionDao : ScienceMissionDao {
    val missions = MutableStateFlow<List<ScienceMission>>(emptyList())
    val operations = mutableListOf<String>()

    override suspend fun insert(item: ScienceMission) = insertAll(listOf(item))

    override suspend fun insertAll(items: List<ScienceMission>) {
        operations += "insertAll"
        val keys = items.map { it.missionKey to it.observationPlaceID }.toSet()
        missions.value = missions.value.filterNot { (it.missionKey to it.observationPlaceID) in keys } + items
    }

    override suspend fun clearAll(observationPlaceID: String) {
        operations += "clearAll:$observationPlaceID"
        missions.value = missions.value.filterNot { it.observationPlaceID == observationPlaceID }
    }

    override suspend fun count(observationPlaceID: String): Int =
        missions.value.count { it.observationPlaceID == observationPlaceID }

    override fun getAllAsFlow(observationPlaceID: String): Flow<List<ScienceMission>> =
        missions.map { list -> list.filter { it.observationPlaceID == observationPlaceID } }
}

/** In-memory [EphemerisDataDao]; reads for [failingMissionKey] throw, to simulate a failing fetch. */
class FakeEphemerisDataDao : EphemerisDataDao {
    val items = mutableListOf<EphemerisData>()
    var failingMissionKey: String? = null

    override suspend fun insertAll(items: List<EphemerisData>) {
        this.items += items
    }

    override suspend fun clearAll(missionKey: String, observationPlaceID: String) {
        items.removeAll { it.missionKey == missionKey && it.observationPlaceID == observationPlaceID }
    }

    override suspend fun clearEphemerisData(missionKeys: List<String>, observationPlaceID: String) {
        items.removeAll { it.missionKey !in missionKeys && it.observationPlaceID == observationPlaceID }
    }

    override suspend fun getAllEphemerisData(
        missionKey: String,
        observationPlaceID: String,
        timestamp: Long
    ): List<EphemerisData> {
        if (missionKey == failingMissionKey) throw IllegalStateException("ephemeris read failed for $missionKey")
        return items.filter {
            it.missionKey == missionKey && it.observationPlaceID == observationPlaceID &&
                it.timestamp >= timestamp && it.timestamp <= timestamp + 36_000_000
        }
    }
}
