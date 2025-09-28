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
import io.ktor.client.statement.request
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


class TransitousService @Inject constructor(private val appPreferenceRepository: AppPreferenceRepository) {

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
        name: String?,
        latitude: Double,
        longitude: Double,
        type: String = "STOP"
    ): Flow<List<TransitStop>> = flow {
        if (!appPreferenceRepository.allowTransitInOfflineMode.value) {
            return@flow
        }
        try {
            Log.d(TAG, "Reverse geocoding: $latitude, $longitude, type: $type")

            val response = client.get("https://api.transitous.org/api/v1/reverse-geocode") {
                parameter("place", "$latitude,$longitude")
                parameter("type", type)
            }

            val result = response.body<List<TransitStop>>().sortedBy { transitStop ->
                val editDistanceHeuristic =
                    if (name.isNullOrBlank()) 0.0 else StringUtils.levenshteinDistance(
                        transitStop.name,
                        name
                    ) * 10.0
                editDistanceHeuristic + LatLng(transitStop.lat, transitStop.lon).distanceTo(
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

    fun getStopTimes(stopId: String, n: Int = 50, radius: Int = 0): Flow<StopTimesResponse> = flow {
        if (!appPreferenceRepository.allowTransitInOfflineMode.value) {
            return@flow
        }
        try {
            Log.d(TAG, "Fetching stop times for stop: $stopId, count: $n")

            val response = client.get("https://api.transitous.org/api/v1/stoptimes") {
                parameter("stopId", stopId)
                parameter("n", n)
                parameter("radius", radius)
                parameter("exactRadius", true)
            }

            val result = response.body<StopTimesResponse>()
            Log.d(TAG, "Stop times response: ${result.stopTimes.size} entries")

            emit(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching stop times", e)
            emit(StopTimesResponse(emptyList(), StopPlace("", stopId, 0.0, 0.0, 0.0, "", "")))
        }
    }

    @OptIn(ExperimentalTime::class)
    fun getPlan(
        from: LatLng,
        to: LatLng,
        via: List<LatLng>? = null,
        viaMinimumStay: List<Int>? = null,
        time: Instant? = null,
        maxTransfers: Int? = null,
        maxTravelTime: Int? = null,
        minTransferTime: Int? = null,
        additionalTransferTime: Int? = null,
        transferTimeFactor: Float? = null,
        maxMatchingDistance: Float? = null,
        pedestrianProfile: String? = null,
        elevationCosts: String? = null,
        useRoutedTransfers: Boolean? = null,
        detailedTransfers: Boolean = true,
        joinInterlinedLegs: Boolean = true,
        transitModes: List<String>? = null,
        directModes: List<String>? = null,
        preTransitModes: List<String>? = null,
        postTransitModes: List<String>? = null,
        directRentalFormFactors: List<String>? = null,
        preTransitRentalFormFactors: List<String>? = null,
        postTransitRentalFormFactors: List<String>? = null,
        directRentalPropulsionTypes: List<String>? = null,
        preTransitRentalPropulsionTypes: List<String>? = null,
        postTransitRentalPropulsionTypes: List<String>? = null,
        numItineraries: Int = 5,
        pageCursor: String? = null,
        timetableView: Boolean = true,
        withFares: Boolean = false,
        language: String? = null
    ): Flow<PlanResponse> = flow {
        if (!appPreferenceRepository.allowTransitInOfflineMode.value) {
            return@flow
        }
        try {
            Log.d(
                TAG,
                "Fetching plan from ${from.latitude},${from.longitude} to ${to.latitude},${to.longitude}"
            )

            val response = client.get("https://api.transitous.org/api/v3/plan") {
                parameter("fromPlace", "${from.latitude},${from.longitude}")
                parameter("toPlace", "${to.latitude},${to.longitude}")
                via?.let { via ->
                    parameter("via", via.map {
                        "${from.latitude},${from.longitude}"
                    })
                }
                viaMinimumStay?.let { parameter("viaMinimumStay", it.joinToString(",")) }
                time?.let { parameter("time", it.toString()) }
                maxTransfers?.let { parameter("maxTransfers", it) }
                maxTravelTime?.let { parameter("maxTravelTime", it) }
                minTransferTime?.let { parameter("minTransferTime", it) }
                additionalTransferTime?.let { parameter("additionalTransferTime", it) }
                transferTimeFactor?.let { parameter("transferTimeFactor", it) }
                maxMatchingDistance?.let { parameter("maxMatchingDistance", it) }
                pedestrianProfile?.let { parameter("pedestrianProfile", it) }
                elevationCosts?.let { parameter("elevationCosts", it) }
                useRoutedTransfers?.let { parameter("useRoutedTransfers", it) }
                parameter("detailedTransfers", detailedTransfers)
                parameter("joinInterlinedLegs", joinInterlinedLegs)
                transitModes?.let { parameter("transitModes", it) }
                directModes?.let { parameter("directModes", it) }
                preTransitModes?.let { parameter("preTransitModes", it) }
                postTransitModes?.let { parameter("postTransitModes", it) }
                directRentalFormFactors?.let { parameter("directRentalFormFactors", it) }
                preTransitRentalFormFactors?.let { parameter("preTransitRentalFormFactors", it) }
                postTransitRentalFormFactors?.let { parameter("postTransitRentalFormFactors", it) }
                directRentalPropulsionTypes?.let { parameter("directRentalPropulsionTypes", it) }
                preTransitRentalPropulsionTypes?.let {
                    parameter(
                        "preTransitRentalPropulsionTypes",
                        it
                    )
                }
                postTransitRentalPropulsionTypes?.let {
                    parameter(
                        "postTransitRentalPropulsionTypes",
                        it
                    )
                }
                parameter("numItineraries", numItineraries)
                pageCursor?.let { parameter("pageCursor", it) }
                parameter("timetableView", timetableView)
                parameter("withFares", withFares)
                language?.let { parameter("language", it) }
            }
            Log.d("TAG", "${response.request.url}")

            val result = response.body<PlanResponse>()
            Log.d(TAG, "Plan response: ${result.itineraries.size} itineraries")

            emit(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching plan", e)
            emit(
                PlanResponse(
                    Place("", null, 0.0, 0.0, 0.0),
                    Place("", null, 0.0, 0.0, 0.0),
                    emptyList(),
                    emptyList(),
                    "",
                    ""
                )
            )
        }
    }

    companion object {
        private const val TAG = "TransitousService"
    }
}
