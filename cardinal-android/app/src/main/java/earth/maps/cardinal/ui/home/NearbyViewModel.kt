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

package earth.maps.cardinal.ui.home

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
