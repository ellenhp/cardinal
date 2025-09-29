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

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
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
    val level: Double? = null,
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
    @SerialName("routeLongName") val routeLongName: String?,
    @SerialName("tripShortName") val tripShortName: String?,
    @SerialName("displayName") val displayName: String,
    @SerialName("pickupDropoffType") val pickupDropoffType: String,
    @SerialName("cancelled") val cancelled: Boolean,
    @SerialName("tripCancelled") val tripCancelled: Boolean,
    @SerialName("source") val source: String
) {
    fun parseRouteColor(): Color? {
        return try {
            Color("#$routeColor".toColorInt())
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}

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

// New classes for plan endpoint

@Serializable
enum class Mode {
    WALK,
    BIKE,
    RENTAL,
    CAR,
    CAR_PARKING,
    CAR_DROPOFF,
    ODM,
    FLEX,
    TRANSIT,
    TRAM,
    SUBWAY,
    FERRY,
    AIRPLANE,
    METRO,
    BUS,
    COACH,
    RAIL,
    HIGHSPEED_RAIL,
    LONG_DISTANCE,
    NIGHT_RAIL,
    REGIONAL_FAST_RAIL,
    REGIONAL_RAIL,
    CABLE_CAR,
    FUNICULAR,
    AREAL_LIFT,
    OTHER
}

@Serializable
enum class VertexType {
    NORMAL,
    BIKESHARE,
    TRANSIT
}

@Serializable
enum class PickupDropoffType {
    NORMAL,
    NOT_ALLOWED
}

@Serializable
enum class FareTransferRule {
    @SerialName("A_AB")
    A_AB,

    @SerialName("A_AB_B")
    A_AB_B,
    AB
}

@Serializable
enum class PedestrianProfile {
    FOOT,
    WHEELCHAIR
}

@Serializable
enum class ElevationCosts {
    NONE,
    LOW,
    HIGH
}

@Serializable
enum class RentalFormFactor {
    BICYCLE,
    CARGO_BICYCLE,
    CAR,
    MOPED,
    SCOOTER_STANDING,
    SCOOTER_SEATED,
    OTHER
}

@Serializable
enum class RentalPropulsionType {
    HUMAN,
    ELECTRIC_ASSIST,
    ELECTRIC,
    COMBUSTION,
    COMBUSTION_DIESEL,
    HYBRID,
    PLUG_IN_HYBRID,
    HYDROGEN_FUEL_CELL
}

@Serializable
data class Alert(
    val id: String? = null,
    val headerText: String? = null,
    val descriptionText: String? = null,
    val url: String? = null,
    val effectiveStartDate: Long? = null,
    val effectiveEndDate: Long? = null
)

@Serializable
data class EncodedPolyline(
    val points: String,
    val precision: Int,
)

@Serializable
data class StepInstruction(
    val relativeDirection: String,
    val distance: Double,
    val polyline: EncodedPolyline,
    val streetName: String,
)

@Serializable
data class Rental(
    val id: String,
    val networks: String,
    val lon: Double,
    val lat: Double,
    val name: String,
    val allowPickup: Boolean,
    val allowDropoff: Boolean
)

@Serializable
data class FareProduct(
    val id: String? = null,
    val name: String? = null
)

@Serializable
data class FareTransfer(
    val rule: FareTransferRule,
    val effectiveFareLegProducts: List<List<FareProduct>>
)

@Serializable
data class TransitPlace(
    val name: String,
    val stopId: String? = null,
    val lat: Double,
    val lon: Double,
    val level: Double,
    val arrival: String? = null,
    val departure: String? = null,
    val scheduledArrival: String? = null,
    val scheduledDeparture: String? = null,
    val scheduledTrack: String? = null,
    val track: String? = null,
    val description: String? = null,
    val vertexType: String? = null,
    val pickupType: String? = null,
    val dropoffType: String? = null,
    val cancelled: Boolean? = null,
    val alerts: List<Alert>? = null,
    val flex: String? = null,
    val flexId: String? = null,
    val flexStartPickupDropOffWindow: String? = null,
    val flexEndPickupDropOffWindow: String? = null
)

@Serializable
data class Leg(
    val mode: Mode,
    @SerialName("from") val fromTransitPlace: TransitPlace,
    @SerialName("to") val toTransitPlace: TransitPlace,
    val duration: Int,
    val startTime: String,
    val endTime: String,
    val scheduledStartTime: String,
    val scheduledEndTime: String,
    val realTime: Boolean,
    val scheduled: Boolean,
    val distance: Double? = null,
    val interlineWithPreviousLeg: Boolean? = null,
    val headsign: String? = null,
    val routeColor: String? = null,
    val routeTextColor: String? = null,
    val routeType: String? = null,
    val agencyName: String? = null,
    val agencyUrl: String? = null,
    val agencyId: String? = null,
    val tripId: String? = null,
    val routeShortName: String? = null,
    val cancelled: Boolean? = null,
    val source: String? = null,
    val intermediateStops: List<TransitPlace>? = null,
    val legGeometry: EncodedPolyline? = null,
    val steps: List<StepInstruction>? = null,
    val rental: Rental? = null,
    val fareTransferIndex: Int? = null,
    val effectiveFareLegIndex: Int? = null,
    val alerts: List<Alert>? = null,
    val loopedCalendarSince: String? = null
)

@Serializable
data class Itinerary(
    val duration: Int,
    val startTime: String,
    val endTime: String,
    val transfers: Int,
    val legs: List<Leg>
)

@Serializable
data class PlanResponse(
    val from: TransitPlace,
    val to: TransitPlace,
    val direct: List<Itinerary>,
    val itineraries: List<Itinerary>,
    val previousPageCursor: String,
    val nextPageCursor: String
)
