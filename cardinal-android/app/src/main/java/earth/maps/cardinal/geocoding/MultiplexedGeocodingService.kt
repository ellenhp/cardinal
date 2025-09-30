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

import earth.maps.cardinal.data.AppPreferenceRepository
import earth.maps.cardinal.data.GeocodeResult
import earth.maps.cardinal.data.LatLng
import kotlinx.coroutines.flow.Flow

class MultiplexedGeocodingService(
    private val appPreferenceRepository: AppPreferenceRepository,
    private val onlineGeocodingService: GeocodingService,
    private val offlineGeocodingService: GeocodingService
) : GeocodingService {

    override suspend fun geocode(query: String, focusPoint: LatLng?): Flow<List<GeocodeResult>> {
        return if (appPreferenceRepository.offlineMode.value) {
            offlineGeocodingService.geocode(query, focusPoint)
        } else {
            onlineGeocodingService.geocode(query, focusPoint)
        }
    }

    override suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double
    ): Flow<List<GeocodeResult>> {
        return if (appPreferenceRepository.offlineMode.value) {
            offlineGeocodingService.reverseGeocode(latitude, longitude)
        } else {
            onlineGeocodingService.reverseGeocode(latitude, longitude)
        }
    }

    override suspend fun nearby(latitude: Double, longitude: Double): Flow<List<GeocodeResult>> {
        return if (appPreferenceRepository.offlineMode.value) {
            offlineGeocodingService.nearby(latitude, longitude)
        } else {
            onlineGeocodingService.nearby(latitude, longitude)
        }
    }
}
