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

package earth.maps.cardinal.data

import io.github.dellisd.spatialk.geojson.Position

data class Place(
    val id: String? = null,
    val name: String,
    val description: String = "",
    val icon: String = "place",
    val latLng: LatLng,
    val address: Address? = null,
    val isMyLocation: Boolean = false,
    val isTransitStop: Boolean = false,
    val transitStopId: String? = null,
) {
    fun toPosition(): Position {
        return Position(latitude = latLng.latitude, longitude = latLng.longitude)
    }
}

