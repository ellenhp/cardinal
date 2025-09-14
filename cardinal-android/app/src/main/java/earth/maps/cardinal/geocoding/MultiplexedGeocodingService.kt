package earth.maps.cardinal.geocoding

import earth.maps.cardinal.data.AppPreferenceRepository
import earth.maps.cardinal.data.GeocodeResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MultiplexedGeocodingService(
    private val appPreferenceRepository: AppPreferenceRepository,
    private val onlineGeocodingService: GeocodingService,
    private val offlineGeocodingService: GeocodingService
) : GeocodingService {
    
    override suspend fun geocode(query: String): Flow<List<GeocodeResult>> {
        return if (appPreferenceRepository.offlineMode.first()) {
            offlineGeocodingService.geocode(query)
        } else {
            onlineGeocodingService.geocode(query)
        }
    }

    override suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double
    ): Flow<List<GeocodeResult>> {
        return if (appPreferenceRepository.offlineMode.first()) {
            offlineGeocodingService.reverseGeocode(latitude, longitude)
        } else {
            onlineGeocodingService.reverseGeocode(latitude, longitude)
        }
    }
}
