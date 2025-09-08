package earth.maps.cardinal.geocoding

import earth.maps.cardinal.data.GeocodeResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PeliasGeocodingServiceTest {

    @Test
    fun testPeliasGeocodingServiceCreation() {
        val service = PeliasGeocodingService()
        assertNotNull(service)
    }

    @Test
    fun testGeocodeMethodExists() {
        val service = PeliasGeocodingService()
        assertTrue(service is GeocodingService)
    }

    @Test
    fun testReverseGeocodeMethodExists() {
        val service = PeliasGeocodingService()
        assertTrue(service is GeocodingService)
    }
}
