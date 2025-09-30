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
