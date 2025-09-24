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

package earth.maps.cardinal.viewmodel

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import earth.maps.cardinal.data.LocationRepository
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.geocoding.GeocodingService
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NearbyViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val geocodingService: GeocodingService,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private companion object {
        private const val TAG = "NearbyViewModel"
    }

    private val _nearbyResults = MutableStateFlow<List<Place>>(emptyList())
    val nearbyResults: StateFlow<List<Place>> = _nearbyResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var lastLocation: Location? = null

    init {
        // Start observing location updates
        locationRepository.startContinuousLocationUpdates(context)
        observeLocationUpdates()
    }

    /**
     * Observes location updates from the LocationRepository and fetches nearby data
     * when the location changes significantly.
     */
    @OptIn(FlowPreview::class)
    private fun observeLocationUpdates() {
        viewModelScope.launch {
            locationRepository.locationFlow.distinctUntilChanged { old, new ->
                // Only update if location changed significantly (more than 500 meters)
                if (new != null) {
                    (old?.distanceTo(new) ?: 1000f) < 500f
                } else {
                    false
                }
            }.collectLatest { location ->
                location?.let {
                    lastLocation = it
                    Log.d(TAG, "Location updated: $lastLocation")
                    fetchNearby(it.latitude, it.longitude)
                }
            }
        }
    }

    fun fetchNearby(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                geocodingService.nearby(latitude, longitude).collect { results ->
                    _nearbyResults.value =
                        results.map { locationRepository.createSearchResultPlace(it) }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Error fetching nearby places"
                _nearbyResults.value = emptyList()
                _isLoading.value = false
            }
        }
    }

    /**
     * Manually refreshes the nearby data using current location.
     */
    fun refreshData() {
        lastLocation?.let { location ->
            fetchNearby(location.latitude, location.longitude)
        }
    }
}
