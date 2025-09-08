package earth.maps.cardinal.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import earth.maps.cardinal.data.ViewportPreferences
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.maplibre.compose.camera.CameraPosition
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * ViewModel responsible for handling map-related functionality including location services.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val viewportPreferences: ViewportPreferences
) : ViewModel() {

    private companion object {
        private const val LOCATION_REQUEST_INTERVAL_MS = 15000L // 15 seconds
        private const val LOCATION_REQUEST_TIMEOUT_MS = 10000L // 10 seconds
        private const val CONTINUOUS_LOCATION_UPDATE_INTERVAL_MS = 5000L // 5 seconds
        private const val CONTINUOUS_LOCATION_UPDATE_DISTANCE_M = 5f // 5 meters
    }

    // Location caching with thread safety
    private var lastRequestedLocation: Location? = null
    private val locationMutex = Mutex()
    private val locationLock = Any()

    // State flows for UI components
    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating

    private val _hasPendingLocationRequest = MutableStateFlow(false)
    val hasPendingLocationRequest: StateFlow<Boolean> = _hasPendingLocationRequest

    private val _locationFlow: MutableStateFlow<Location?> = MutableStateFlow(null)
    val locationFlow: StateFlow<Location?> = _locationFlow.asStateFlow()

    // Location listener for continuous updates
    private var locationListener: LocationListener? = null

    // Permission tracking
    private val previousPermissionState = AtomicBoolean(false)

    /**
     * Saves the current viewport to preferences.
     */
    fun saveViewport(cameraPosition: CameraPosition) {
        viewportPreferences.saveViewport(cameraPosition)
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

    /**
     * Handles permission state changes and initiates location request if needed.
     *
     * @param hasPermission Current permission state
     * @param cameraState Camera state to animate to location
     * @param context Android context for location services
     */
    suspend fun handlePermissionStateChange(
        hasPermission: Boolean,
        cameraState: org.maplibre.compose.camera.CameraState,
        context: Context
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
    @SuppressLint("MissingPermission")
    suspend fun fetchLocationAndCreateCameraPosition(context: Context): CameraPosition? {
        _isLocating.value = true
        try {
            val currentTime = System.currentTimeMillis()

            // Return cached location if it's recent
            val cachedLocation = locationMutex.withLock {
                lastRequestedLocation
            }

            cachedLocation?.let { location ->
                if (isLocationRecent(location, currentTime)) {
                    return createCameraPosition(location)
                }
            }

            return try {
                val locationManager = getLocationManager(context)

                // Try to get last known location from any provider
                val lastKnownLocation = getLastKnownLocation(locationManager)

                // If we have a recent last known location, use it
                lastKnownLocation?.let { location ->
                    if (isLocationRecent(location, currentTime)) {
                        locationMutex.withLock {
                            lastRequestedLocation = location
                        }
                        return createCameraPosition(location)
                    }
                }

                // If no recent location available, request a fresh location
                requestFreshLocation(locationManager)
            } catch (_: Exception) {
                // Handle exceptions during location fetching
                null
            }
        } finally {
            _isLocating.value = false
        }
    }

    /**
     * Creates a CameraPosition from a Location.
     */
    private fun createCameraPosition(location: Location): CameraPosition {
        return CameraPosition(
            target = Position(location.longitude, location.latitude),
            zoom = 15.0
        )
    }

    /**
     * Checks if a location is recent (less than LOCATION_REQUEST_INTERVAL_MS old).
     */
    private fun isLocationRecent(location: Location, currentTime: Long): Boolean {
        return currentTime - location.time < LOCATION_REQUEST_INTERVAL_MS
    }

    /**
     * Gets the LocationManager system service.
     */
    private fun getLocationManager(context: Context): LocationManager {
        return try {
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        } catch (e: Exception) {
            // Handle exceptions during service retrieval
            throw IllegalStateException("Failed to get LocationManager", e)
        }
    }

    /**
     * Gets the most recent last known location from all available providers.
     */
    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(locationManager: LocationManager): Location? {
        return try {
            var lastKnownLocation: Location? = null
            val providers = locationManager.getProviders(true)

            for (provider in providers) {
                val location = locationManager.getLastKnownLocation(provider)
                if (location != null && (lastKnownLocation == null || location.time > lastKnownLocation.time)) {
                    lastKnownLocation = location
                }
            }

            lastKnownLocation
        } catch (_: Exception) {
            // Handle exceptions during location retrieval
            null
        }
    }

    /**
     * Requests a fresh location from the location manager.
     */
    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(locationManager: LocationManager): CameraPosition? {
        return withContext(Dispatchers.Main) {
            val locationDeferred = CompletableDeferred<Location?>()

            val locationListener = createLocationListener(locationManager, locationDeferred)

            try {
                // Request location updates from the best provider
                @Suppress("DEPRECATION") val bestProvider = locationManager.getBestProvider(
                    android.location.Criteria().apply {
                        accuracy = android.location.Criteria.ACCURACY_FINE
                    }, true
                )

                bestProvider?.let { provider ->
                    locationManager.requestLocationUpdates(
                        provider,
                        0, // min time in ms
                        0f, // min distance in meters
                        locationListener
                    )

                    // Set a timeout for location request
                    setupLocationRequestTimeout(locationManager, locationListener, locationDeferred)
                } ?: run {
                    // If no provider is available, complete immediately
                    locationDeferred.complete(null)
                }

                // Wait for location or timeout
                val location = locationDeferred.await()
                location?.let { createCameraPosition(it) }
            } catch (_: Exception) {
                // Ensure cleanup in case of exceptions
                try {
                    locationManager.removeUpdates(locationListener)
                } catch (_: Exception) {
                    // Ignore cleanup exceptions
                }
                locationDeferred.complete(null)
                null
            }
        }
    }


    /**
     * Starts continuous location updates.
     * This should be called from the UI when the map is ready.
     */
    @SuppressLint("MissingPermission")
    fun startContinuousLocationUpdates(context: Context) {
        viewModelScope.launch {
            try {
                val locationManager = getLocationManager(context)

                @Suppress("DEPRECATION") val bestProvider = locationManager.getBestProvider(
                    android.location.Criteria().apply {
                        accuracy = android.location.Criteria.ACCURACY_FINE
                    }, true
                )

                bestProvider?.let { provider ->
                    locationListener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            // Update the location flow with the new location
                            _locationFlow.value = location
                            synchronized(locationLock) {
                                lastRequestedLocation = location
                            }
                        }

                        override fun onProviderDisabled(provider: String) {
                            // Stop location updates if provider is disabled
                            try {
                                locationManager.removeUpdates(this)
                            } catch (_: Exception) {
                                // Ignore exceptions during cleanup
                            }
                            locationListener = null
                            _locationFlow.value = null
                        }

                        override fun onProviderEnabled(provider: String) {}

                        override fun onStatusChanged(
                            provider: String?,
                            status: Int,
                            extras: Bundle?
                        ) {
                        }
                    }

                    locationManager.requestLocationUpdates(
                        provider,
                        CONTINUOUS_LOCATION_UPDATE_INTERVAL_MS,
                        CONTINUOUS_LOCATION_UPDATE_DISTANCE_M,
                        locationListener!!
                    )
                }
            } catch (_: Exception) {
                // Handle exceptions during setup
                locationListener = null
                _locationFlow.value = null
            }
        }
    }

    /**
     * Stops location updates when the ViewModel is cleared.
     */
    override fun onCleared() {
        super.onCleared()
        locationListener?.let { listener ->
            // Note: We can't access context here to remove updates
            // The UI should handle cleanup when the map is destroyed
            locationListener = null
        }
    }

    /**
     * Creates a location listener that handles location updates and provider events.
     */
    private fun createLocationListener(
        locationManager: LocationManager,
        locationDeferred: CompletableDeferred<Location?>
    ): LocationListener {
        return object : LocationListener {
            override fun onLocationChanged(location: Location) {
                // Remove updates to avoid continuous location requests
                try {
                    locationManager.removeUpdates(this)
                } catch (_: Exception) {
                    // Ignore exceptions during cleanup
                }

                // Complete the deferred with the location
                if (locationDeferred.isActive) {
                    synchronized(locationLock) {
                        lastRequestedLocation = location
                    }
                    locationDeferred.complete(location)
                }
            }

            override fun onProviderDisabled(provider: String) {
                try {
                    locationManager.removeUpdates(this)
                } catch (_: Exception) {
                    // Ignore exceptions during cleanup
                }

                if (locationDeferred.isActive) {
                    locationDeferred.complete(null)
                }
            }

            override fun onProviderEnabled(provider: String) {}

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
    }

    /**
     * Sets up a timeout for location requests to prevent hanging.
     */
    private fun setupLocationRequestTimeout(
        locationManager: LocationManager,
        locationListener: LocationListener,
        locationDeferred: CompletableDeferred<Location?>
    ) {
        viewModelScope.launch {
            kotlinx.coroutines.delay(LOCATION_REQUEST_TIMEOUT_MS)
            if (locationDeferred.isActive) {
                try {
                    locationManager.removeUpdates(locationListener)
                } catch (_: Exception) {
                    // Ignore exceptions during cleanup
                }
                locationDeferred.complete(null)
            }
        }
    }
}
