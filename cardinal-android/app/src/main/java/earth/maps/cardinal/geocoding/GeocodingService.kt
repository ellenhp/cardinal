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
}
