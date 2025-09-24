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

package earth.maps.cardinal.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Utility functions for formatting distances based on unit preferences.
 */
object GeoUtils {

    private const val METERS_TO_KILOMETERS = 1000.0
    private const val METERS_TO_MILES = 1609.34
    private const val METERS_TO_FEET = 3.28084

    /**
     * Formats a distance in meters to a human-readable string based on the unit preference.
     *
     * @param meters Distance in meters
     * @param unitPreference Either DISTANCE_UNIT_METRIC or DISTANCE_UNIT_IMPERIAL
     * @return Formatted distance string (e.g., "1.2 km" or "0.8 mi")
     */
    fun formatDistance(meters: Double, unitPreference: Int): String {
        return when (unitPreference) {
            AppPreferences.DISTANCE_UNIT_METRIC -> formatMetricDistance(meters)
            AppPreferences.DISTANCE_UNIT_IMPERIAL -> formatImperialDistance(meters)
            else -> formatMetricDistance(meters) // Default to metric
        }
    }

    /**
     * Formats a distance in meters using metric units (km/m).
     */
    private fun formatMetricDistance(meters: Double): String {
        return when {
            meters >= METERS_TO_KILOMETERS -> {
                val kilometers = meters / METERS_TO_KILOMETERS
                if (kilometers >= 10) {
                    "${kilometers.roundToInt()} km"
                } else {
                    String.format("%.1f km", kilometers)
                }
            }

            else -> {
                "${meters.roundToInt()} m"
            }
        }
    }

    /**
     * Formats a distance in meters using imperial units (mi/ft).
     */
    private fun formatImperialDistance(meters: Double): String {
        val miles = meters / METERS_TO_MILES

        return when {
            miles >= 0.1 -> {
                if (miles >= 10) {
                    "${miles.roundToInt()} mi"
                } else {
                    String.format("%.1f mi", miles)
                }
            }

            else -> {
                val feet = meters * METERS_TO_FEET
                "${feet.roundToInt()} ft"
            }
        }
    }

    /**
     * Formats a short distance (typically for route steps) in meters.
     * Always shows meters for metric, feet for imperial.
     *
     * @param meters Distance in meters
     * @param unitPreference Either DISTANCE_UNIT_METRIC or DISTANCE_UNIT_IMPERIAL
     * @return Formatted short distance string (e.g., "150 m" or "490 ft")
     */
    fun formatShortDistance(meters: Double, unitPreference: Int): String {
        return when (unitPreference) {
            AppPreferences.DISTANCE_UNIT_METRIC -> "${meters.roundToInt()} m"
            AppPreferences.DISTANCE_UNIT_IMPERIAL -> {
                val feet = meters * METERS_TO_FEET
                "${feet.roundToInt()} ft"
            }

            else -> "${meters.roundToInt()} m" // Default to metric
        }
    }

    /**
     * Calculates the great-circle distance between two points on Earth using the haversine formula.
     *
     * @param latLng1 First point with latitude and longitude
     * @param latLng2 Second point with latitude and longitude
     * @return Distance in meters
     */
    fun haversineDistance(latLng1: LatLng, latLng2: LatLng): Double {
        val lat1 = Math.toRadians(latLng1.latitude)
        val lon1 = Math.toRadians(latLng1.longitude)
        val lat2 = Math.toRadians(latLng2.latitude)
        val lon2 = Math.toRadians(latLng2.longitude)

        val deltaLat = lat2 - lat1
        val deltaLon = lon2 - lon1

        val a = sin(deltaLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(deltaLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        // Earth's radius in meters
        val earthRadius = 6371000.0

        return earthRadius * c
    }

    /**
     * Creates a bounding box with an approximate radius around a specified LatLng point.
     *
     * @param center The center point around which to create the bounding box
     * @param radiusMeters The radius in meters
     * @return A BoundingBox representing the area around the center point
     */
    fun createBoundingBoxAroundPoint(center: LatLng, radiusMeters: Double): BoundingBox {
        // Earth's radius in meters
        val earthRadius = 6371000.0

        // Calculate the approximate delta in degrees for the given radius
        val latDelta = Math.toDegrees(radiusMeters / earthRadius)
        val lonDelta = Math.toDegrees(radiusMeters / (earthRadius * cos(Math.toRadians(center.latitude))))

        // Create the bounding box with north, south, east, west boundaries
        return BoundingBox(
            north = center.latitude + latDelta,
            south = center.latitude - latDelta,
            east = center.longitude + lonDelta,
            west = center.longitude - lonDelta
        )
    }
}
