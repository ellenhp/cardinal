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

package earth.maps.cardinal.ui.place

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import earth.maps.cardinal.data.AppPreferenceRepository
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.room.SavedPlaceDao
import earth.maps.cardinal.transit.StopTime
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
    private val placeDao: SavedPlaceDao,
    private val transitousService: TransitousService,
    private val appPreferenceRepository: AppPreferenceRepository,
) : ViewModel() {

    private var refreshJob: Job? = null

    val isPlaceSaved = mutableStateOf(false)
    val stop = mutableStateOf<Place?>(null)
    val departures = MutableStateFlow<List<StopTime>>(emptyList())

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

    val use24HourFormat = appPreferenceRepository.use24HourFormat

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

    private suspend fun fetchDepartures() {
        _isLoading.value = true
        _isRefreshingDepartures.value = true
        try {
            stop.value?.transitStopId?.let { stopId ->
                if (stopId.isNotEmpty()) {
                    transitousService.getStopTimes(stopId).collectLatest { response ->
                        departures.value = response.stopTimes
                    }
                }
            }
            _didLoadingFail.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch departures for $stop", e)
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
