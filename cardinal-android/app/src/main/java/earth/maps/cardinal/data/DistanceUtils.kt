package earth.maps.cardinal.data

import kotlin.math.roundToInt

/**
 * Utility functions for formatting distances based on unit preferences.
 */
object DistanceUtils {

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
}
