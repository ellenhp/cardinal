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

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.room.SavedPlaceDao
import earth.maps.cardinal.transit.StopTime
import earth.maps.cardinal.transit.TransitStop
import earth.maps.cardinal.transit.TransitousService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class TransitStopCardViewModel @Inject constructor(
    private val placeDao: SavedPlaceDao, private val transitousService: TransitousService
) : ViewModel() {

    private var refreshJob: Job? = null

    val isPlaceSaved = mutableStateOf(false)
    val stop = mutableStateOf<Place?>(null)
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

    init {
        refreshJob = viewModelScope.launch {
            while (true) {
                refreshDepartures()
                delay(30.seconds)
            }
        }
    }

    fun setStop(place: Place) {
        this.stop.value = place
    }

    suspend fun initializeDepartures() {
        val place = stop.value
        if (place != null) {
            checkIfPlaceIsSaved(place)
            reverseGeocodeStop(place)
            fetchDepartures()
        } else {
            Log.e(TAG, "Can't find departures for a `null` stop.")
        }

    }

    fun checkIfPlaceIsSaved(place: Place) {
        viewModelScope.launch {
            if (place.id != null) {
                val existingPlace = placeDao.getPlace(place.id)
                isPlaceSaved.value = existingPlace != null
            }
        }
    }

    private suspend fun reverseGeocodeStop(place: Place) {
        _isLoading.value = true
        try {
            transitousService.reverseGeocode(
                name = place.name,
                latitude = place.latLng.latitude,
                longitude = place.latLng.longitude,
                type = "STOP"
            ).collectLatest { stops ->
                reverseGeocodedStop.value = stops.firstOrNull()
                Log.d(TAG, "$reverseGeocodedStop")
                // Update the place with the reverse-geocoded name if available
                reverseGeocodedStop.value?.let { stop ->
                    this@TransitStopCardViewModel.stop.value = place.copy(
                        name = stop.name
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reverse geocode stop $place", e)
        }
    }

    private suspend fun fetchDepartures() {
        _isRefreshingDepartures.value = true
        try {
            // We need to get the stop ID from the reverse geocoded result
            reverseGeocodedStop.value?.id?.let { stopId ->
                if (stopId.isNotEmpty()) {
                    transitousService.getStopTimes(stopId).collectLatest { response ->
                        departures.value = response.stopTimes
                    }
                }
            }
            _didLoadingFail.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch departures for $reverseGeocodedStop", e)
            _didLoadingFail.value = true
        } finally {
            _isRefreshingDepartures.value = false
            // Initial loading is guaranteed to be over now, as fetching departures is the last step.
            _isLoading.value = false
        }
    }

    suspend fun refreshDepartures() {
        _isRefreshingDepartures.value = true
        try {
            stop.value?.let { place ->
                fetchDepartures()
            }
        } finally {
            _isRefreshingDepartures.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }

    companion object {
        private const val TAG = "TransitStopViewModel"
    }
}
