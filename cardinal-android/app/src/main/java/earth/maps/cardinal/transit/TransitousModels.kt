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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransitStop(
    val type: String,
    val tokens: List<String>,
    val name: String,
    val id: String,
    val lat: Double,
    val lon: Double,
    val level: Double,
    val tz: String,
    val areas: List<Area>,
    val score: Double
)

@Serializable
data class Area(
    val name: String,
    @SerialName("adminLevel") val adminLevel: Double,
    val matched: Boolean,
    val unique: Boolean,
    val default: Boolean
)

@Serializable
data class StopTimesResponse(
    val stopTimes: List<StopTime>,
    val place: StopPlace,
    val previousPageCursor: String? = null,
    val nextPageCursor: String? = null
)

@Serializable
data class StopTime(
    val place: StopPlace,
    val mode: String,
    @SerialName("realTime") val realTime: Boolean,
    val headsign: String,
    @SerialName("agencyId") val agencyId: String,
    @SerialName("agencyName") val agencyName: String,
    @SerialName("agencyUrl") val agencyUrl: String,
    @SerialName("routeColor") val routeColor: String,
    @SerialName("tripId") val tripId: String,
    @SerialName("routeType") val routeType: Int,
    @SerialName("routeShortName") val routeShortName: String,
    @SerialName("routeLongName") val routeLongName: String,
    @SerialName("tripShortName") val tripShortName: String,
    @SerialName("displayName") val displayName: String,
    @SerialName("pickupDropoffType") val pickupDropoffType: String,
    @SerialName("cancelled") val cancelled: Boolean,
    @SerialName("tripCancelled") val tripCancelled: Boolean,
    @SerialName("source") val source: String
)

@Serializable
data class StopPlace(
    val name: String,
    @SerialName("stopId") val stopId: String,
    val lat: Double,
    val lon: Double,
    val level: Double,
    val tz: String,
    val vertexType: String,
    val arrival: String? = null,
    val departure: String? = null,
    @SerialName("scheduledArrival") val scheduledArrival: String? = null,
    @SerialName("scheduledDeparture") val scheduledDeparture: String? = null,
    @SerialName("pickupType") val pickupType: String? = null,
    @SerialName("dropoffType") val dropoffType: String? = null,
    @SerialName("cancelled") val cancelled: Boolean? = null
)
