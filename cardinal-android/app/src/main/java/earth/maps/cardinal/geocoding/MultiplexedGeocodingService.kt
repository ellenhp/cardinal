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
}
