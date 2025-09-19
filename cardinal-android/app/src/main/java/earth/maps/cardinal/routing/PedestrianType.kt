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
