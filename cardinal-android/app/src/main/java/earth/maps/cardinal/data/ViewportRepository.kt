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
