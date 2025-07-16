package com.domnis.nebuni.data

data class ObservationPlace (
    val latitude: Double = 43.284055, // Notre-Dame de la Garde's latitude
    val longitude: Double = 5.371309, // Notre-Dame de la Garde's longitude
    val altMin: Int = 0,
    val altMax: Int = 90,
    val azMin: Int = 0,
    val azMax: Int = 360
)