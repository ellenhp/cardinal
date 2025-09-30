/*
 *     Cardinal Maps
 *     Copyright (C) 2025 Cardinal Maps Authors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
