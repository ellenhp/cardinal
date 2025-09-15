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
