/*
 *     Cardinal Maps
 *     Copyright (C) 2025 Cardinal Maps Authors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package earth.maps.cardinal.geocoding

import android.util.Log
import earth.maps.cardinal.data.Address
import earth.maps.cardinal.data.AppPreferenceRepository
import earth.maps.cardinal.data.GeocodeResult
import earth.maps.cardinal.data.LatLng
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

class PeliasGeocodingService(private val appPreferenceRepository: AppPreferenceRepository) :
    GeocodingService {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging)
    }

    override suspend fun geocode(query: String, focusPoint: LatLng?): Flow<List<GeocodeResult>> =
        flow {
            try {
                Log.d(TAG, "Geocoding query: $query, focusPoint: $focusPoint")
                val config = appPreferenceRepository.peliasApiConfig.value
                val response = client.get("${config.baseUrl}/autocomplete") {
                    parameter("text", query)
                    parameter("size", "10")
                    config.apiKey?.let { parameter("api_key", it) }
                    focusPoint?.let {
                        parameter("focus.point.lat", it.latitude.toString())
                        parameter("focus.point.lon", it.longitude.toString())
                    }
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
            val config = appPreferenceRepository.peliasApiConfig.value
            val response = client.get("${config.baseUrl}/reverse") {
                parameter("point.lat", latitude.toString())
                parameter("point.lon", longitude.toString())
                parameter("size", "10")
                config.apiKey?.let { parameter("api_key", it) }
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

    override suspend fun nearby(latitude: Double, longitude: Double): Flow<List<GeocodeResult>> =
        flow {
            try {
                Log.d(TAG, "Nearby: $latitude, $longitude")
                val config = appPreferenceRepository.peliasApiConfig.value
                val response = client.get("${config.baseUrl}/nearby") {
                    parameter("point.lat", latitude.toString())
                    parameter("point.lon", longitude.toString())
                    parameter("size", "50")
                    parameter("layers", "venue")
                    config.apiKey?.let { parameter("api_key", it) }
                }

                val result = response.body<JsonObject>()
                Log.d(TAG, "Nearby response: $result")
                val features = result["features"]?.jsonArray ?: JsonArray(emptyList())
                Log.d(TAG, "Number of nearby features: ${features.size}")

                val geocodeResults = features.mapNotNull { element ->
                    parseGeocodeResult(element)
                }
                Log.d(TAG, "Parsed nearby results: ${geocodeResults.size}")

                emit(geocodeResults)
            } catch (e: Exception) {
                Log.e(TAG, "Error during nearby", e)
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
            val osmAddendum =
                properties?.get("addendum")?.jsonObject?.get("osm")?.jsonObject?.toMap()
            val tags =
                osmAddendum?.map { (key, value) -> key to value.jsonPrimitive.content }?.toMap()

            if (lat != null && lon != null) {
                val address = if (properties != null) {
                    Address(
                        houseNumber = properties["housenumber"]?.jsonPrimitive?.content,
                        road = properties["street"]?.jsonPrimitive?.content,
                        city = properties["locality"]?.jsonPrimitive?.content,
                        state = properties["region"]?.jsonPrimitive?.content,
                        postcode = properties["postalcode"]?.jsonPrimitive?.content,
                        country = properties["country"]?.jsonPrimitive?.content,
                        countryCode = properties["country_code"]?.jsonPrimitive?.content
                    )
                } else {
                    null
                }

                GeocodeResult(
                    latitude = lat,
                    longitude = lon,
                    displayName = displayName,
                    address = address,
                    properties = tags ?: mapOf()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
