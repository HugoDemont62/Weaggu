package fr.hugodemont.weaggu

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),1)
        fetchCurrencyData().start()
    }

    private fun hereLocation(lat: Double, lon: Double): String? {
        var cityName: String? = null
        val gcd = Geocoder(applicationContext, Locale.getDefault())
        val addresses: List<Address>?
        try {
            addresses = gcd.getFromLocation(lat, lon, 1)
            if (addresses?.isNotEmpty() == true) {
                println(addresses[0].locality)
                cityName = addresses[0].locality
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return cityName
    }

    private fun generateRandomLocation(): Pair<Double, Double> {
        val random = Random()
        val lat = random.nextDouble() * 58 - 35 + (Math.random() * 0.2 - 0.1)
        val lon = random.nextDouble() * 72 - 13 + (Math.random() * 0.2 - 0.1)
        println("generateRandomLocation: $lat, $lon")
        return Pair(lat, lon)
    }

    //private fun generateLocationWithEuropeanCity(): Pair<Double, Double> {
    //    val cities = mapOf(
    //        "Paris" to Pair(48.8566, 2.3522),
    //        // ajouter d'autres villes européennes ici
    //    )
    //    val randomCity = cities.entries.random()
    //    val cityCoordinates = randomCity.value
    //    val lat = cityCoordinates.first + (Math.random() * 0.2 - 0.1)
    //    val lon = cityCoordinates.second + (Math.random() * 0.2 - 0.1)
    //    return Pair(lat, lon)
    //}

    private fun getLocation(): Pair<Double, Double>? {
        try {

            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                // Affiche un message d'erreur et retourne null.
                val builder = AlertDialog.Builder(this)
                builder.setMessage("Veuillez activer votre GPS pour utiliser cette fonctionnalité.")
                    .setCancelable(false)
                    .setPositiveButton("Paramètres") { _, _ ->
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                    .setNegativeButton("Annuler") { dialog, _ ->
                        dialog.cancel()
                    }
                val alert = builder.create()
                alert.show()
                return null
            }
            // Vérifie si le GPS est activé.
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                // Affiche un message d'erreur et retourne null.
                val builder = AlertDialog.Builder(this)
                builder.setMessage("Veuillez activer votre GPS pour utiliser cette fonctionnalité.")
                    .setCancelable(false)
                    .setPositiveButton("Paramètres") { _, _ ->
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                    .setNegativeButton("Annuler") { dialog, _ ->
                        dialog.cancel()
                    }
                val alert = builder.create()
                alert.show()
                return null
            }

            // Vérifie si l'application a l'autorisation d'accéder à la localisation de l'utilisateur.
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Demande l'autorisation d'accéder à la localisation de l'utilisateur.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1
                )
                return null
            }
            // Récupère la dernière position connue.
            val lastKnownLocation =
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: return null
            // Demande les mises à jour de la position en temps réel.
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0,
                0f
            ) { location ->
                println("Location: ${location.latitude}, ${location.longitude}")
            }
            // Récupère les coordonnées de la position de l'utilisateur.
            val lat = lastKnownLocation.latitude
            val long = lastKnownLocation.longitude
            println("Location getlocation: $lat, $long")
            return Pair(lat, long)
        } catch (ex: SecurityException) {
            // Gérer l'exception SecurityException levée si la permission ACCESS_FINE_LOCATION n'est pas accordée
            println("Erreur de sécurité lors de l'obtention de la position : ${ex.message}")
            return null
        } catch (ex: Exception) {
            // Gérer les autres exceptions levées par le LocationManager
            println("Erreur lors de l'obtention de la position : ${ex.message}")
            return null
        }
    }


    private fun fetchCurrencyData(): Thread {
        return Thread {
            //GPS location
            val locationPair = getLocation()
            val lat = locationPair?.first
            val lon = locationPair?.second
            println("lat and lon: $lat, $lon")
            val location = if (lat != null && lon != null) {
                hereLocation(lat, lon)
            } else {
                generateRandomLocation().let { hereLocation(it.first, it.second) }
            }
            //if (location == null) {
            //    location = generateLocationWithEuropeanCity().let { hereLocation(it.first, it.second) }
            //}
            println("Location: $location")
            if (location == null) {
                println("Error: location is null")
                return@Thread
            }
            val url =
                URL("http://api.weatherapi.com/v1/current.json?key=4be4887a07e34f968cd95452232203&q=$location&aqi=no")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode == 200) {
                val inputSystem = connection.inputStream
                val inputStreamReader = InputStreamReader(inputSystem, "UTF-8")
                val response = Gson().fromJson(inputStreamReader, WeatherResponse::class.java)
                updateUI(
                    response.location.name,
                    response.current.temp_c,
                    response.current.condition.text,
                    response.location.lat,
                    response.location.lon
                )
                inputStreamReader.close()
                inputSystem.close()
                println("Success: ${connection.responseCode}")
            } else {
                println("Error: ${connection.responseCode}")
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(
        cityName: String,
        temperature: Double,
        condition: String,
        lat: Double,
        lon: Double,
    ) {
        runOnUiThread {
            kotlin.run {
                val cityNameTextView = findViewById<TextView>(R.id.idCityName)
                cityNameTextView.text = cityName

                val temperatureTextView = findViewById<TextView>(R.id.idTemperature)
                temperatureTextView.text = "$temperature °C $lat $lon"

                val conditionTextView = findViewById<TextView>(R.id.idCondition)
                conditionTextView.text = condition
            }
        }
    }

    data class WeatherResponse(
        val location: Location,
        val current: Current,
    )

    data class Location(
        val name: String,
        val region: String,
        val country: String,
        val lat: Double,
        val lon: Double,
        val tz_id: String,
        val localtime_epoch: Long,
        val localtime: String,
    )

    data class Current(
        val last_updated_epoch: Long,
        val last_updated: String,
        val temp_c: Double,
        val temp_f: Double,
        val is_day: Int,
        val condition: Condition,
        val wind_mph: Double,
        val wind_kph: Double,
        val wind_degree: Int,
        val wind_dir: String,
        val pressure_mb: Double,
        val pressure_in: Double,
        val precip_mm: Double,
        val precip_in: Double,
        val humidity: Int,
        val cloud: Int,
        val feelslike_c: Double,
        val feelslike_f: Double,
        val vis_km: Double,
        val vis_miles: Double,
        val uv: Double,
        val gust_mph: Double,
        val gust_kph: Double,
    )

    data class Condition(
        val text: String,
        val icon: String,
        val code: Int,
    )

}

