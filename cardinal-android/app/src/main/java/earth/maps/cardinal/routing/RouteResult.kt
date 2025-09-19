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
