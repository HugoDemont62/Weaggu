package fr.hugodemont.weaggu

import android.annotation.SuppressLint
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
        //val random = Random()
        //val lat = random.nextDouble() * 180 - 90
        //val lon = random.nextDouble() * 360 - 180
        //return Pair(lat, lon)
        val cities = mapOf(
            "Paris" to Pair(48.8566, 2.3522),
            "London" to Pair(51.5072, -0.1276),
            "Berlin" to Pair(52.5200, 13.4050),
            "Madrid" to Pair(40.4168, -3.7038),
            "Rome" to Pair(41.9028, 12.4964),
            // ajouter d'autres villes européennes ici
        )
        val randomCity = cities.entries.random()
        val cityCoordinates = randomCity.value
        val lat = cityCoordinates.first + (Math.random() * 0.2 - 0.1)
        val lon = cityCoordinates.second + (Math.random() * 0.2 - 0.1)
        return Pair(lat, lon)
    }

    private fun fetchCurrencyData(): Thread {
        return Thread {
            //GPS location
            val (lat, lon) = generateRandomLocation()
            val location = hereLocation(lat, lon)
            println("Location: $location")
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
                updateUI(response.location.name, response.current.temp_c, response.current.condition.text)
                inputStreamReader.close()
                inputSystem.close()
                println("Success: ${connection.responseCode}")
            } else {
                println("Error: ${connection.responseCode}")
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(cityName: String, temperature: Double, condition: String) {
        runOnUiThread {
            kotlin.run {
                val cityNameTextView = findViewById<TextView>(R.id.idCityName)
                cityNameTextView.text = cityName

                val temperatureTextView = findViewById<TextView>(R.id.idTemperature)
                temperatureTextView.text = "$temperature °C"

                val conditionTextView = findViewById<TextView>(R.id.idCondition)
                conditionTextView.text = condition
            }
        }
    }

    data class WeatherResponse(
        val location: Location,
        val current: Current
    )

    data class Location(
        val name: String,
        val region: String,
        val country: String,
        val lat: Double,
        val lon: Double,
        val tz_id: String,
        val localtime_epoch: Long,
        val localtime: String
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
        val gust_kph: Double
    )

    data class Condition(
        val text: String,
        val icon: String,
        val code: Int
    )

}

