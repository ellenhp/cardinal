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
