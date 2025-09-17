package earth.maps.cardinal.routing

import com.google.gson.annotations.SerializedName

/**
 * Enum representing bicycle types for routing.
 * Based on Valhalla API bicycle_type parameter.
 */
enum class BicycleType(val value: String, val displayName: String) {
    @SerializedName("road")
    ROAD("road", "Road"),

    @SerializedName("hybrid")
    HYBRID("hybrid", "Hybrid"),

    @SerializedName("cross")
    CROSS("cross", "Cross"),

    @SerializedName("mountain")
    MOUNTAIN("mountain", "Mountain");

    companion object {
        /**
         * Get BicycleType from string value, with fallback to ROAD.
         */
        fun fromValue(value: String?): BicycleType {
            return entries.find { it.value == value } ?: ROAD
        }
    }
}
