package com.puprojects.weathertrack.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WeatherDao {
    @Insert
    fun insertWeather(weather: WeatherEntity)

    @Query("SELECT * FROM weather_searches ORDER BY timestamp DESC LIMIT 10")
    fun getRecentSearches(): List<WeatherEntity>
}
