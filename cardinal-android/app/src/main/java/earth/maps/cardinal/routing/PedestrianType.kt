package earth.maps.cardinal.routing

import com.google.gson.annotations.SerializedName

/**
 * Enum representing pedestrian types for routing.
 * Based on Valhalla API pedestrian type parameter.
 */
enum class PedestrianType(val value: String, val displayName: String) {
    @SerializedName("foot")
    FOOT("foot", "Foot"),

    @SerializedName("wheelchair")
    WHEELCHAIR("wheelchair", "Wheelchair"),

    @SerializedName("blind")
    BLIND("blind", "Blind");

    companion object {
        /**
         * Get PedestrianType from string value, with fallback to FOOT.
         */
        fun fromValue(value: String?): PedestrianType {
            return entries.find { it.value == value } ?: FOOT
        }
    }
}
