package earth.maps.cardinal.geocoding

import earth.maps.cardinal.data.GeocodeResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class OfflineGeocodingService : GeocodingService {
    override suspend fun geocode(query: String): Flow<List<GeocodeResult>> = flow {
        // TODO: Implement offline geocoding when ready
        // For now, return empty list
        emit(emptyList())
    }

    override suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double
    ): Flow<List<GeocodeResult>> = flow {
        // TODO: Implement offline reverse geocoding when ready
        // For now, return empty list
        emit(emptyList())
    }
}
