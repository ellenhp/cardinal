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

package earth.maps.cardinal.ui.core

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.times
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import earth.maps.cardinal.data.LocationRepository
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.ViewportPreferences
import earth.maps.cardinal.data.ViewportRepository
import earth.maps.cardinal.data.room.SavedPlace
import earth.maps.cardinal.data.room.SavedPlaceDao
import earth.maps.cardinal.geocoding.OfflineGeocodingService
import io.github.dellisd.spatialk.geojson.Feature
import io.github.dellisd.spatialk.geojson.FeatureCollection
import io.github.dellisd.spatialk.geojson.Point
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * ViewModel responsible for handling map-related functionality including location services.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val viewportPreferences: ViewportPreferences,
    private val viewportRepository: ViewportRepository,
    private val locationRepository: LocationRepository,
    private val offlineGeocodingService: OfflineGeocodingService,
    private val placeDao: SavedPlaceDao,
) : ViewModel() {

    private companion object {
        const val TAG = "MapViewModel"
        private const val LOCATION_REQUEST_TIMEOUT_MS = 10000L // 10 seconds
        private const val CONTINUOUS_LOCATION_UPDATE_INTERVAL_MS = 5000L // 5 seconds
        private const val CONTINUOUS_LOCATION_UPDATE_DISTANCE_M = 5f // 5 meters
    }

    // Location caching with thread safety
    private var lastRequestedLocation: Location? = null
    private val locationLock = Any()

    // State flows for UI components - delegate to repository
    val isLocating: StateFlow<Boolean> = locationRepository.isLocating

    private val _hasPendingLocationRequest = MutableStateFlow(false)
    val hasPendingLocationRequest: StateFlow<Boolean> = _hasPendingLocationRequest

    val locationFlow: StateFlow<Location?> = locationRepository.locationFlow

    // Location listener for continuous updates
    private var locationListener: LocationListener? = null

    // Permission tracking
    private val previousPermissionState = AtomicBoolean(false)

    var peekHeight: Dp = 0.dp
    var screenHeight: Dp = 0.dp
    var screenWidth: Dp = 0.dp

    val savedPlacesFlow: Flow<FeatureCollection> = placeDao.getAllPlacesAsFlow().map { placeList ->
        FeatureCollection(placeList.map { createFeatureFromPlace(it) })
    }

    init {
        locationRepository.startContinuousLocationUpdates(context)
    }

    /**
     * Creates a Feature from a SavedPlace with proper JSON escaping.
     */
    private fun createFeatureFromPlace(place: SavedPlace): Feature {
        val name = place.customName ?: place.name
        val description = place.customDescription ?: place.type

        val properties = mutableMapOf(
            "saved_poi_id" to escapeJsonString(place.id),
            "name" to escapeJsonString(name),
            "description" to escapeJsonString(description)
        )

        place.houseNumber?.let { properties["addr:housenumber"] = escapeJsonString(it) }
        place.road?.let { properties["addr:street"] = escapeJsonString(it) }
        place.city?.let { properties["addr:city"] = escapeJsonString(it) }
        place.postcode?.let { properties["addr:postcode"] = escapeJsonString(it) }
        place.state?.let { properties["addr:state"] = escapeJsonString(it) }
        place.country?.let { properties["addr:country"] = escapeJsonString(it) }
        place.countryCode?.let { properties["country_code"] = escapeJsonString(it) }
        place.transitStopId?.let { properties["transit_stop_id"] = escapeJsonString(it) }

        return Feature(
            geometry = Point(Position(latitude = place.latitude, longitude = place.longitude)),
            properties = properties
        )
    }

    private fun escapeJsonString(input: String): JsonElement {
        return Json.parseToJsonElement(
            Json.Default.encodeToString(
                String.Companion.serializer(),
                input
            )
        )
    }

    /**
     * Saves the current viewport to preferences.
     */
    fun saveViewport(cameraPosition: CameraPosition) {
        viewportPreferences.saveViewport(cameraPosition)
        // Update the viewport center for geocoding focus
        updateViewportCenter(cameraPosition)
    }

    /**
     * Updates the current viewport center for geocoding focus.
     */
    fun updateViewportCenter(cameraPosition: CameraPosition) {
        viewportRepository.updateViewportCenter(cameraPosition)
    }

    /**
     * Loads the saved viewport from preferences.
     * Returns null if no viewport has been saved.
     */
    fun loadViewport(): CameraPosition? {
        return viewportPreferences.loadViewport()
    }

    /**
     * Marks that a location request is pending due to missing permissions.
     */
    fun markLocationRequestPending() {
        _hasPendingLocationRequest.value = true
    }

    fun handleMapTap(
        cameraState: CameraState,
        dpOffset: DpOffset,
        onMapPoiClick: (Place) -> Unit,
        onMapInteraction: () -> Unit
    ) {
        val features = cameraState.projection?.queryRenderedFeatures(
            dpOffset,
            layerIds = setOf("user_favorites", "poi_z14", "poi_z15", "poi_z16", "poi_transit")
        )
        Log.d(TAG, "${features?.count()} features available at tap location")
        val filteredFeatures = features?.filter {
            it.geometry is Point
        }
        val savedFeatures = filteredFeatures?.filter { it.properties.contains("saved_poi_id") }
        val transitFeatures =
            filteredFeatures?.filter { it.properties["class"]?.jsonPrimitive?.content == "bus" }
        val namedFeatures = filteredFeatures?.filter { it.properties.contains("name") }
        val feature = savedFeatures?.firstOrNull() ?: transitFeatures?.firstOrNull()
        ?: namedFeatures?.firstOrNull() ?: filteredFeatures?.firstOrNull()
        if (feature != null) {
            val feature = convertFeatureToPlace(
                feature
            )
            onMapPoiClick(feature)
        } else {
            onMapInteraction()
        }
    }

    fun convertFeatureToPlace(feature: Feature): Place {
        // Convert JsonElement properties to Map<String, String>
        val tags = feature.properties.mapValues { (_, value) ->
            value.jsonPrimitive.content
        }

        // Extract coordinates from geometry (assuming Point geometry)
        val point = feature.geometry as Point
        val coordinates = point.coordinates
        val longitude = coordinates.longitude
        val latitude = coordinates.latitude

        // These two lines are not ideal. Ideally we'd have a less heavyweight way to format the address.
        val result =
            offlineGeocodingService.buildResult(tags, latitude = latitude, longitude = longitude)
        return locationRepository.createSearchResultPlace(result).copy(
            transitStopId = tags["transit_stop_id"],
            isTransitStop = tags.containsKey("transit_stop_id"),
            id = tags["saved_poi_id"]
        )
    }

    /**
     * Handles permission state changes and initiates location request if needed.
     *
     * @param hasPermission Current permission state
     * @param cameraState Camera state to animate to location
     * @param context Android context for location services
     */
    suspend fun handlePermissionStateChange(
        hasPermission: Boolean, cameraState: CameraState, context: Context
    ) {
        val previousState = previousPermissionState.getAndSet(hasPermission)
        // Check if permissions changed from denied to granted and we have a pending request
        if (!previousState && hasPermission && _hasPendingLocationRequest.value) {
            _hasPendingLocationRequest.value = false
            fetchLocationAndCreateCameraPosition(context)?.let { position ->
                cameraState.animateTo(position)
            }
        }
    }

    /**
     * Fetches the current location and returns a CameraPosition to animate to.
     * Returns null if location cannot be determined.
     */
    suspend fun fetchLocationAndCreateCameraPosition(
        context: Context,
    ): CameraPosition? {
        val location = locationRepository.getCurrentLocation(context)
        return location?.let { createCameraPosition(it) }
    }

    /**
     * Creates a CameraPosition from a Location.
     */
    private fun createCameraPosition(location: Location): CameraPosition {
        return CameraPosition(
            target = Position(location.longitude, location.latitude),
            zoom = 15.0,
            padding = PaddingValues(
                start = screenWidth / 8,
                top = screenHeight / 8,
                end = screenWidth / 8,
                bottom = min(
                    3f * screenHeight / 4, peekHeight + screenHeight / 8
                )
            )
        )
    }
}