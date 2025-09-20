/*
 *    Copyright 2025 The Cardinal Authors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package earth.maps.cardinal.transit

import android.util.Log
import earth.maps.cardinal.data.AppPreferenceRepository
import earth.maps.cardinal.data.LatLng
import earth.maps.cardinal.data.StringUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json


class TransitousService(private val appPreferenceRepository: AppPreferenceRepository) {

    private val client = HttpClient(Android) {
        install(UserAgent) {
            agent = "Cardinal Maps https://github.com/ellenhp/cardinal ellen.h.poe@gmail.com"
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging)
    }

    fun reverseGeocode(
        name: String,
        latitude: Double,
        longitude: Double,
        type: String = "STOP"
    ): Flow<List<TransitStop>> = flow {
        try {
            Log.d(TAG, "Reverse geocoding: $latitude, $longitude, type: $type")

            val response = client.get("https://api.transitous.org/api/v1/reverse-geocode") {
                parameter("place", "$latitude,$longitude")
                parameter("type", type)
            }

            val result = response.body<List<TransitStop>>().sortedBy {
                val editDistanceHeuristic = StringUtils.levenshteinDistance(it.name, name) * 10.0
                editDistanceHeuristic + LatLng(it.lat, it.lon).distanceTo(
                    LatLng(latitude, longitude)
                )
            }
            Log.d(TAG, "Reverse geocode response: ${result.size} stops")

            emit(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error during reverse geocoding", e)
            emit(emptyList())
        }
    }

    fun getStopTimes(stopId: String, n: Int = 10): Flow<StopTimesResponse> = flow {
        try {
            Log.d(TAG, "Fetching stop times for stop: $stopId, count: $n")

            val response = client.get("https://api.transitous.org/api/v1/stoptimes") {
                parameter("stopId", stopId)
                parameter("n", n)
            }

            val result = response.body<StopTimesResponse>()
            Log.d(TAG, "Stop times response: ${result.stopTimes.size} entries")

            emit(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching stop times", e)
            emit(StopTimesResponse(emptyList(), StopPlace("", stopId, 0.0, 0.0, 0.0, "", "")))
        }
    }

    companion object {
        private const val TAG = "TransitousService"
    }
}
