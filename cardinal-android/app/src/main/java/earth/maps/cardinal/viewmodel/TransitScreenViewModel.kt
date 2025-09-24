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
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import earth.maps.cardinal.data.LatLng
import earth.maps.cardinal.data.LocationRepository
import earth.maps.cardinal.transit.StopTime
import earth.maps.cardinal.transit.TransitStop
import earth.maps.cardinal.transit.TransitousService
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class TransitScreenViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val locationRepository: LocationRepository,
    private val transitousService: TransitousService
) : ViewModel() {

    private companion object {
        private const val TAG = "TransitMapViewModel"
        private const val DEFAULT_RADIUS_METERS = 1000
        private const val NUM_EVENTS = 200
    }

    val stop = mutableStateOf<String?>(null)
    val reverseGeocodedStop = mutableStateOf<TransitStop?>(null)
    val departures = mutableStateOf<List<StopTime>>(emptyList())

    private val _didLoadingFail = MutableStateFlow(false)

    /**
     * Whether the viewmodel is initially loading transit departures for the provided stop.
     */
    val didLoadingFail: StateFlow<Boolean> = _didLoadingFail

    private val _isLoading = MutableStateFlow(false)

    /**
     * Whether the viewmodel is initially loading transit departures for the provided stop.
     */
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isRefreshingDepartures = MutableStateFlow(false)

    /**
     * Whether the viewmodel is refreshing departures for the provided stop.
     */
    val isRefreshingDepartures: StateFlow<Boolean> = _isRefreshingDepartures

    private var lastLocation: Location? = null

    init {
        // Start observing location updates
        locationRepository.startContinuousLocationUpdates(context)
        observeLocationUpdates()

        viewModelScope.launch {
            while (true) {
                refreshData()
                delay(60.seconds)
            }
        }
    }

    /**
     * Observes location updates from the LocationRepository and fetches transit data
     * when the location changes significantly.
     */
    @OptIn(FlowPreview::class)
    private fun observeLocationUpdates() {
        viewModelScope.launch {
            locationRepository.locationFlow.distinctUntilChanged { old, new ->
                Log.d("TAG", "$old $new")
                // Only update if location changed significantly (more than 100 meters)
                if (new != null) {
                    (old?.distanceTo(new) ?: 1000f) < 100f
                } else {
                    false
                }
            }.collectLatest { location ->
                location?.let {
                    lastLocation = it
                    Log.d("TAG", "$lastLocation")
                    refreshData()
                }
            }
        }
    }

    private suspend fun reverseGeocodeStop(latLng: LatLng) {
        _isLoading.value = true
        try {
            transitousService.reverseGeocode(
                name = null, latitude = latLng.latitude, longitude = latLng.longitude, type = "STOP"
            ).collectLatest { stops ->
                reverseGeocodedStop.value = stops.firstOrNull()
                Log.d(TAG, "$reverseGeocodedStop")
                // Update the place with the reverse-geocoded name if available
                reverseGeocodedStop.value?.let { stop ->
                    this@TransitScreenViewModel.stop.value = stop.id
                }
            }
        } catch (e: Exception) {
            Log.e(
                TAG, "Failed to reverse geocode stop $latLng", e
            )
        }
    }

    private suspend fun fetchDepartures() {
        _isRefreshingDepartures.value = true
        try {
            // We need to get the stop ID from the reverse geocoded result
            reverseGeocodedStop.value?.id?.let { stopId ->
                if (stopId.isNotEmpty()) {
                    transitousService.getStopTimes(
                        stopId, n = NUM_EVENTS, radius = DEFAULT_RADIUS_METERS
                    ).collectLatest { response ->
                        departures.value = aggregateStopTimes(response.stopTimes)
                    }
                }
            }
            _didLoadingFail.value = false
        } catch (e: Exception) {
            Log.e(
                TAG, "Failed to fetch departures for $reverseGeocodedStop", e
            )
            _didLoadingFail.value = true
        } finally {
            _isRefreshingDepartures.value = false
            // Initial loading is guaranteed to be over now, as fetching departures is the last step.
            _isLoading.value = false
        }
    }

    private fun aggregateStopTimes(stopTimes: List<StopTime>): List<StopTime> {
        val lastLocation = lastLocation ?: return emptyList()
        val stopTimesByProximity = stopTimes.sortedBy {
            LatLng(it.place.lat, it.place.lon).distanceTo(
                LatLng(lastLocation.latitude, lastLocation.longitude)
            )
        }
        val bestStopForRouteHeadsign = mutableMapOf<Pair<String, String>, String>()
        val relevantStopTimes = mutableListOf<StopTime>()
        for (stopTime in stopTimesByProximity) {
            val pair = Pair(stopTime.routeShortName, stopTime.headsign)
            if (!bestStopForRouteHeadsign.containsKey(pair)) {
                bestStopForRouteHeadsign[pair] = stopTime.place.stopId
            } else if (bestStopForRouteHeadsign[pair] != stopTime.place.stopId) {
                continue
            }
            relevantStopTimes.add(stopTime)
        }
        return relevantStopTimes
    }

    /**
     * Manually refreshes the transit data.
     */
    fun refreshData() {
        lastLocation?.let { location ->
            viewModelScope.launch {
                reverseGeocodeStop(LatLng(location.latitude, location.longitude))
                fetchDepartures()
            }
        }
    }
}

