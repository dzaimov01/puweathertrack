package com.puprojects.weathertrack.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_searches")
data class WeatherEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val city: String,
    val region: String,
    val country: String,
    val temperature: Double,
    val condition: String,
    val timestamp: Long = System.currentTimeMillis()
)
