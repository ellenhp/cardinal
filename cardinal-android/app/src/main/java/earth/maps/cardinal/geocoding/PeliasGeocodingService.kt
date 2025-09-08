package earth.maps.cardinal.geocoding

import android.util.Log
import earth.maps.cardinal.data.Address
import earth.maps.cardinal.data.GeocodeResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val TAG = "PeliasGeocoding"

class PeliasGeocodingService : GeocodingService {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging)
    }

    private val baseUrl = "https://maps.earth/pelias/v1"

    override suspend fun geocode(query: String): Flow<List<GeocodeResult>> = flow {
        try {
            Log.d(TAG, "Geocoding query: $query")
            val response = client.get("$baseUrl/autocomplete") {
                parameter("text", query)
                parameter("size", "10")
            }

            val result = response.body<JsonObject>()
            Log.d(TAG, "Response: $result")
            val features = result["features"]?.jsonArray ?: JsonArray(emptyList())
            Log.d(TAG, "Number of features: ${features.size}")

            val geocodeResults = features.mapNotNull { element ->
                parseGeocodeResult(element)
            }
            Log.d(TAG, "Parsed results: ${geocodeResults.size}")

            emit(geocodeResults)
        } catch (e: Exception) {
            Log.e(TAG, "Error during geocoding", e)
            emit(emptyList())
        }
    }

    override suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double
    ): Flow<List<GeocodeResult>> = flow {
        try {
            Log.d(TAG, "Reverse geocoding: $latitude, $longitude")
            val response = client.get("$baseUrl/reverse") {
                parameter("point.lat", latitude.toString())
                parameter("point.lon", longitude.toString())
                parameter("size", "10")
            }

            val result = response.body<JsonObject>()
            Log.d(TAG, "Reverse response: $result")
            val features = result["features"]?.jsonArray ?: JsonArray(emptyList())
            Log.d(TAG, "Number of reverse features: ${features.size}")

            val geocodeResults = features.mapNotNull { element ->
                parseGeocodeResult(element)
            }
            Log.d(TAG, "Parsed reverse results: ${geocodeResults.size}")

            emit(geocodeResults)
        } catch (e: Exception) {
            Log.e(TAG, "Error during reverse geocoding", e)
            emit(emptyList())
        }
    }

    private fun parseGeocodeResult(element: JsonElement): GeocodeResult? {
        return try {
            val obj = element.jsonObject
            val geometry = obj["geometry"]?.jsonObject
            val coordinates = geometry?.get("coordinates")?.jsonArray

            val lon = coordinates?.getOrNull(0)?.jsonPrimitive?.doubleOrNull
            val lat = coordinates?.getOrNull(1)?.jsonPrimitive?.doubleOrNull

            val properties = obj["properties"]?.jsonObject
            val displayName = properties?.get("label")?.jsonPrimitive?.content ?: ""

            if (lat != null && lon != null) {
                val address = if (properties != null) {
                    Address(
                        houseNumber = properties["housenumber"]?.jsonPrimitive?.content,
                        road = properties["street"]?.jsonPrimitive?.content,
                        city = properties["locality"]?.jsonPrimitive?.content,
                        state = properties["region"]?.jsonPrimitive?.content,
                        postcode = properties["postalcode"]?.jsonPrimitive?.content,
                        country = properties["country"]?.jsonPrimitive?.content
                    )
                } else {
                    null
                }

                GeocodeResult(
                    latitude = lat,
                    longitude = lon,
                    displayName = displayName,
                    address = address
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
