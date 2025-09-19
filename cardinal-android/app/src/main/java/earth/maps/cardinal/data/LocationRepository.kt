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

package earth.maps.cardinal.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import dagger.hilt.android.qualifiers.ApplicationContext
import earth.maps.cardinal.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling location services.
 * Provides a centralized way to access current location across the app.
 */
@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

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

    private val _locationFlow: MutableStateFlow<Location?> = MutableStateFlow(null)
    val locationFlow: StateFlow<Location?> = _locationFlow.asStateFlow()

    // Location listener for continuous updates
    private var locationListener: LocationListener? = null

    /**
     * Gets the current location, either from cache or by requesting a fresh one.
     * Returns null if location cannot be determined.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? {
        _isLocating.value = true
        try {
            val currentTime = System.currentTimeMillis()

            // Return cached location if it's recent
            val cachedLocation = locationMutex.withLock {
                lastRequestedLocation
            }

            cachedLocation?.let { location ->
                if (isLocationRecent(location, currentTime)) {
                    return location
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
                        return location
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
     * Starts continuous location updates.
     * This should be called from the UI when the map is ready.
     */
    @SuppressLint("MissingPermission")
    fun startContinuousLocationUpdates(context: Context) {
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

    /**
     * Stops location updates.
     */
    fun stopLocationUpdates(context: Context) {
        locationListener?.let { listener ->
            try {
                val locationManager = getLocationManager(context)
                locationManager.removeUpdates(listener)
            } catch (_: Exception) {
                // Ignore exceptions during cleanup
            }
            locationListener = null
        }
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
    private suspend fun requestFreshLocation(locationManager: LocationManager): Location? {
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
                location?.let {
                    synchronized(locationLock) {
                        lastRequestedLocation = it
                    }
                }
                location
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
        kotlinx.coroutines.MainScope().launch {
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

    /**
     * Creates a standardized "My Location" Place object.
     * This centralizes the creation of "My Location" places to ensure consistency.
     */
    fun createMyLocationPlace(latLng: LatLng): Place {
        return Place(
            id = Int.MIN_VALUE, // Special ID for "My Location"
            name = context.getString(R.string.my_location),
            type = "Current Location",
            icon = "location",
            latLng = latLng,
            isMyLocation = true
        )
    }

    /**
     * Creates a "My Location" Place object using the current location.
     * Returns null if current location is not available.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocationAsPlace(): Place? {
        return getCurrentLocation(context)?.let { location ->
            createMyLocationPlace(LatLng(location.latitude, location.longitude))
        }
    }

    /**
     * Forces a fresh location fetch and creates a "My Location" Place object.
     * This bypasses the cache and always gets a new location.
     * Returns null if fresh location cannot be obtained.
     */
    @SuppressLint("MissingPermission")
    suspend fun getFreshCurrentLocationAsPlace(): Place? {
        return requestFreshLocation(getLocationManager(context))?.let { location ->
            createMyLocationPlace(LatLng(location.latitude, location.longitude))
        }
    }
}
