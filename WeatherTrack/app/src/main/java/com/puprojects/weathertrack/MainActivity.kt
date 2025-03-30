package com.puprojects.weathertrack

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.puprojects.weathertrack.api.WeatherApiService
import com.puprojects.weathertrack.api.WeatherResponse
import com.puprojects.weathertrack.ui.theme.WeatherTrackTheme
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale
import android.Manifest
import android.app.Activity
import android.os.Looper
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.puprojects.weathertrack.db.AppDatabase
import com.puprojects.weathertrack.db.WeatherDao
import com.puprojects.weathertrack.db.WeatherEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeatherTrackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background) {
                    WeatherAppScreen()
                }
            }
        }
    }
}

@Composable
fun WeatherAppScreen() {
    var city by remember { mutableStateOf("") }
    var currentCity by remember { mutableStateOf("") }
    var weatherInfo by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var recentSearches: List<WeatherEntity> by remember { mutableStateOf(emptyList()) }

    val db = remember { AppDatabase.getInstance(context) }
    val dao = remember { db.weatherDao() }

    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.weatherapi.com/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val weatherApi = retrofit.create(WeatherApiService::class.java)
    val apiKey = "0a658f762ddb4c2faa5191325251903"
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                recentSearches = withContext(Dispatchers.IO) { dao.getRecentSearches() }
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                fetchLocationAndWeather(context, fusedLocationClient, weatherApi, apiKey, scope, dao) { cityName, info ->
                    currentCity = cityName
                    weatherInfo = info
                }
            } else {
                weatherInfo = "Разрешение за локация е отказано."
            }
        }
    )

    // Заявка за местоположение при първоначално зареждане
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fetchLocationAndWeather(context, fusedLocationClient, weatherApi, apiKey, scope, dao) { cityName, info ->
                currentCity = cityName
                weatherInfo = info
            }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    recentSearches = withContext(Dispatchers.IO) { dao.getRecentSearches() }
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // UI Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("Въведи град") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (city.isNotEmpty()) {
                    scope.launch {
                        try {
                            val response = weatherApi.getWeather(apiKey, city)
                            val weatherEntity = WeatherEntity(
                                city = response.location.name,
                                region = response.location.region,
                                country = response.location.country,
                                temperature = response.current.temp_c,
                                condition = response.current.condition.text
                            )
                            withContext(Dispatchers.IO) { dao.insertWeather(weatherEntity) }

                            val intent = Intent(context, DetailActivity::class.java).apply {
                                putExtra("CITY", response.location.name)
                                putExtra("REGION", response.location.region)
                                putExtra("COUNTRY", response.location.country)
                                putExtra("TEMPERATURE", response.current.temp_c.toString())
                                putExtra("CONDITION", response.current.condition.text)
                                putExtra("HUMIDITY", response.current.humidity.toString())
                                putExtra("WIND_SPEED", response.current.wind_kph.toString())
                                putExtra("PRESSURE", response.current.pressure_mb.toString())
                                putExtra("FEELS_LIKE", response.current.feelslike_c.toString())
                                putExtra("VISIBILITY", response.current.vis_km.toString())
                                putExtra("UV", response.current.uv.toString())
                                putExtra("ICON_URL", response.current.condition.icon)
                            }
                            launcher.launch(intent)
                        } catch (e: Exception) {
                            weatherInfo = "Грешка при търсене."
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Покажи прогнозата")
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (weatherInfo.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = weatherInfo,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (recentSearches.isNotEmpty()) {
            Text("Последни търсения:", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(8.dp))

            recentSearches.take(10).forEach {
                Text(
                    text = "${it.city}, ${it.country} - ${it.temperature}°C, ${it.condition}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

fun fetchLocationAndWeather(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    api: WeatherApiService,
    apiKey: String,
    scope: CoroutineScope,
    dao: WeatherDao,
    onResult: (String, String) -> Unit
) {
    getCurrentLocation(context, fusedLocationClient) { cityName ->
        if (cityName == "Неизвестно място" || cityName.isBlank()) {
            onResult(cityName, "Не можа да се определи текущият град.")
        } else {
            scope.launch {
                try {
                    val response = api.getWeather(apiKey, cityName)
                    val info = "Град: ${response.location.name} | Температура: ${response.current.temp_c}°C | Състояние: ${response.current.condition.text}"
                    val weatherEntity = WeatherEntity(
                        city = response.location.name,
                        region = response.location.region,
                        country = response.location.country,
                        temperature = response.current.temp_c,
                        condition = response.current.condition.text
                    )
                    withContext(Dispatchers.IO) { dao.insertWeather(weatherEntity) }
                    onResult(cityName, info)
                } catch (e: Exception) {
                    onResult(cityName, "Неуспешно зареждане на времето.")
                }
            }
        }
    }
}


@SuppressLint("MissingPermission")
fun getCurrentLocation(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    onLocationReceived: (String) -> Unit
) {
    fusedLocationClient.lastLocation
        .addOnSuccessListener { location: Location? ->
            if (location != null) {
                getCityFromLocation(context, location.latitude, location.longitude, onLocationReceived)
            } else {
                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(2000)
                    .setMaxUpdateDelayMillis(5000)
                    .build()

                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        locationResult.lastLocation?.let { newLocation ->
                            getCityFromLocation(context, newLocation.latitude, newLocation.longitude, onLocationReceived)
                            fusedLocationClient.removeLocationUpdates(this)
                        } ?: run {
                            onLocationReceived("Грешка при получаване на нова локация.")
                        }
                    }
                }

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        }
        .addOnFailureListener {
            onLocationReceived("Грешка при получаване на местоположението.")
        }
}

fun getCityFromLocation(
    context: Context,
    latitude: Double,
    longitude: Double,
    onCityReceived: (String) -> Unit
) {
    val geocoder = Geocoder(context, Locale.getDefault())
    try {
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)

        if (!addresses.isNullOrEmpty()) {
            val cityName = addresses[0].locality ?: addresses[0].subAdminArea ?: addresses[0].adminArea ?: "Неизвестно място"
            onCityReceived(cityName)
        } else {
            onCityReceived("Неизвестно място")
        }
    } catch (e: Exception) {
        onCityReceived("Неизвестно място")
    }
}

