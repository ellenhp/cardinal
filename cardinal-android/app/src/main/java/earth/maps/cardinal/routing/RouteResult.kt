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

data class RouteResult(
    val distance: Double, // in meters or as specified by units
    val duration: Double, // in seconds
    val legs: List<RouteLeg>,
    val geometry: RouteGeometry,
    val units: String = "kilometers" // or "miles"
)

data class RouteLeg(
    val summary: String,
    val distance: Double,
    val duration: Double,
    val steps: List<RouteStep>
)

data class RouteStep(
    val distance: Double,
    val duration: Double,
    val instruction: String,
    val name: String,
    val geometry: RouteGeometry,
)

data class RouteGeometry(
    val type: String = "LineString",
    val coordinates: List<List<Double>> // [longitude, latitude] pairs
)

data class Maneuver(
    val location: List<Double>, // [longitude, latitude]
    val bearingBefore: Double,
    val bearingAfter: Double,
    val type: String,
    val modifier: String? = null,
    val instruction: String
)
