package fr.hugodemont.weaggu

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

            if (connection.responseCode == 200) {
                val inputSystem = connection.inputStream
                val inputStreamReader = InputStreamReader(inputSystem, "UTF-8")
                val request = Gson().fromJson(inputStreamReader, Request::class.java)
                updateUI(request)
                inputStreamReader.close()
                inputSystem.close()
            } else {
                println("Error: ${connection.responseCode}")
            }
        }
    }

    private fun updateUI(request: Request) {
        runOnUiThread {
            kotlin.run {
                val cityName = findViewById<TextView>(R.id.idCityName)
                cityName.text = request.name
            }
        }
    }
}

