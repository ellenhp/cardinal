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

package earth.maps.cardinal.geocoding

import earth.maps.cardinal.data.GeocodeResult
import earth.maps.cardinal.data.LatLng
import kotlinx.coroutines.flow.Flow

interface GeocodingService {
    /**
     * Geocode a query string to find matching locations
     * @param query The search query (e.g., address, place name)
     * @param focusPoint Optional focus point for viewport biasing
     * @return Flow of geocoding results
     */
    suspend fun geocode(query: String, focusPoint: LatLng? = null): Flow<List<GeocodeResult>>

    /**
     * Reverse geocode coordinates to find address information
     * @param latitude The latitude coordinate
     * @param longitude The longitude coordinate
     * @return Flow of geocoding results
     */
    suspend fun reverseGeocode(latitude: Double, longitude: Double): Flow<List<GeocodeResult>>

    /**
     * Find nearby places around a given point
     * @param latitude The latitude coordinate
     * @param longitude The longitude coordinate
     * @return Flow of nearby places
     */
    suspend fun nearby(latitude: Double, longitude: Double): Flow<List<GeocodeResult>>
}
