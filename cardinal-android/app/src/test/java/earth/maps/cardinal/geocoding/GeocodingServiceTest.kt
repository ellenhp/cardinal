package earth.maps.cardinal.geocoding

import earth.maps.cardinal.data.GeocodeResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GeocodingServiceTest {

    @Test
    fun testOnlineGeocodingServiceCreation() {
        val service = GeocodingServiceFactory.createService(GeocodingServiceFactory.ServiceType.ONLINE)
        assertTrue(service is PeliasGeocodingService)
    }

    @Test
    fun testOfflineGeocodingServiceCreation() {
        val service = GeocodingServiceFactory.createService(GeocodingServiceFactory.ServiceType.OFFLINE)
        assertTrue(service is OfflineGeocodingService)
    }

    @Test
    fun testDefaultServiceCreation() {
        val service = GeocodingServiceFactory.createDefaultService()
        assertTrue(service is PeliasGeocodingService)
    }

    @Test
    fun testGeocodeResultDataClass() {
        val result = GeocodeResult(
            latitude = 40.7128,
            longitude = -74.0060,
            displayName = "New York, NY, USA"
        )
        
        assertEquals(40.7128, result.latitude, 0.0001)
        assertEquals(-74.0060, result.longitude, 0.0001)
        assertEquals("New York, NY, USA", result.displayName)
    }
}
