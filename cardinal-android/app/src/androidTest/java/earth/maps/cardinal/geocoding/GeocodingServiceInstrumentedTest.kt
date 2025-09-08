package earth.maps.cardinal.geocoding

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import earth.maps.cardinal.data.GeocodeResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for geocoding service
 */
@RunWith(AndroidJUnit4::class)
class GeocodingServiceInstrumentedTest {

    @Test
    fun useAppContext() {
        // Context of the app under test
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("earth.maps.cardinal", appContext.packageName)
    }

    @Test
    fun testServiceCreation() {
        val onlineService = GeocodingServiceFactory.createService(GeocodingServiceFactory.ServiceType.ONLINE)
        assertTrue(onlineService is PeliasGeocodingService)
        
        val offlineService = GeocodingServiceFactory.createService(GeocodingServiceFactory.ServiceType.OFFLINE)
        assertTrue(offlineService is OfflineGeocodingService)
    }

    @Test
    fun testGeocodeResultCreation() {
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
