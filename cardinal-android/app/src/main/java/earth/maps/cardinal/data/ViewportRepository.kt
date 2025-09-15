package earth.maps.cardinal.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.maplibre.compose.camera.CameraPosition
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing viewport state across the application.
 * This allows sharing viewport information between different ViewModels.
 */
@Singleton
class ViewportRepository @Inject constructor() {

    // Current viewport center for geocoding focus
    private val _viewportCenter = MutableStateFlow<LatLng?>(null)
    val viewportCenter: StateFlow<LatLng?> = _viewportCenter.asStateFlow()

    /**
     * Updates the current viewport center for geocoding focus.
     */
    fun updateViewportCenter(cameraPosition: CameraPosition) {
        val center = LatLng(
            latitude = cameraPosition.target.latitude,
            longitude = cameraPosition.target.longitude
        )
        _viewportCenter.value = center
    }

    /**
     * Updates the viewport center from LatLng coordinates.
     */
    fun updateViewportCenter(latLng: LatLng) {
        _viewportCenter.value = latLng
    }
}
