package com.puprojects.weathertrack

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class DetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val tvCity = findViewById<TextView>(R.id.tvCity)
        val tvRegion = findViewById<TextView>(R.id.tvRegion)
        val tvCountry = findViewById<TextView>(R.id.tvCountry)
        val tvTemperature = findViewById<TextView>(R.id.tvTemperature)
        val tvCondition = findViewById<TextView>(R.id.tvCondition)
        val tvHumidity = findViewById<TextView>(R.id.tvHumidity)
        val tvWindSpeed = findViewById<TextView>(R.id.tvWindSpeed)
        val tvPressure = findViewById<TextView>(R.id.tvPressure)
        val tvFeelsLike = findViewById<TextView>(R.id.tvFeelsLike)
        val tvVisibility = findViewById<TextView>(R.id.tvVisibility)
        val tvUV = findViewById<TextView>(R.id.tvUV)
        val ivWeatherIcon = findViewById<ImageView>(R.id.ivWeatherIcon)
        val btnBack = findViewById<Button>(R.id.btnBack)

        // Получаване на данните от Intent
        val city = intent.getStringExtra("CITY")
        val region = intent.getStringExtra("REGION")
        val country = intent.getStringExtra("COUNTRY")
        val temperature = intent.getStringExtra("TEMPERATURE")
        val condition = intent.getStringExtra("CONDITION")
        val humidity = intent.getStringExtra("HUMIDITY")
        val windSpeed = intent.getStringExtra("WIND_SPEED")
        val pressure = intent.getStringExtra("PRESSURE")
        val feelsLike = intent.getStringExtra("FEELS_LIKE")
        val visibility = intent.getStringExtra("VISIBILITY")
        val uv = intent.getStringExtra("UV")
        val iconUrl = intent.getStringExtra("ICON_URL")

        // Показване на данните
        tvCity.text = "Град: $city"
        tvRegion.text = "Регион: $region"
        tvCountry.text = "Държава: $country"
        tvTemperature.text = "Температура: $temperature °C"
        tvCondition.text = "Състояние: $condition"
        tvHumidity.text = "Влажност: $humidity%"
        tvWindSpeed.text = "Скорост на вятъра: $windSpeed км/ч"
        tvPressure.text = "Атмосферно налягане: $pressure mb"
        tvFeelsLike.text = "Усеща се като: $feelsLike °C"
        tvVisibility.text = "Видимост: $visibility км"
        tvUV.text = "UV индекс: $uv"

        // Зареждане на иконата със Glide
        Glide.with(this).load("https:$iconUrl").into(ivWeatherIcon)

        btnBack.setOnClickListener {
            setResult(Activity.RESULT_OK) // 👈 съобщаваш на MainActivity
            finish()
        }
    }
}