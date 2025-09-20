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

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.room.PlaceDao
import earth.maps.cardinal.data.room.PlaceEntity
import earth.maps.cardinal.transit.StopTime
import earth.maps.cardinal.transit.TransitStop
import earth.maps.cardinal.transit.TransitousService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransitStopCardViewModel @Inject constructor(
    private val placeDao: PlaceDao,
    private val transitousService: TransitousService
) : ViewModel() {

    val isPlaceSaved = mutableStateOf(false)
    val stop = mutableStateOf<Place?>(null)
    val reverseGeocodedStop = mutableStateOf<TransitStop?>(null)
    val departures = mutableStateOf<List<StopTime>>(emptyList())
    val isLoading = mutableStateOf(false)

    suspend fun setStop(place: Place) {
        this.stop.value = place
        checkIfPlaceIsSaved(place)
        reverseGeocodeStop(place)
        fetchDepartures()
    }

    fun checkIfPlaceIsSaved(place: Place) {
        viewModelScope.launch {
            if (place.id != null) {
                val existingPlace = placeDao.getPlaceById(place.id)
                isPlaceSaved.value = existingPlace != null
            }
        }
    }

    private suspend fun reverseGeocodeStop(place: Place) {
        isLoading.value = true
        try {
            transitousService.reverseGeocode(
                latitude = place.latLng.latitude,
                longitude = place.latLng.longitude,
                type = "STOP"
            ).collectLatest { stops ->
                reverseGeocodedStop.value = stops.firstOrNull()
                // Update the place with the reverse-geocoded name if available
                reverseGeocodedStop.value?.let { stop ->
                    this@TransitStopCardViewModel.stop.value = place.copy(
                        name = stop.name
                    )
                }
            }
        } catch (e: Exception) {
            // Handle error silently or log it
            e.printStackTrace()
        } finally {
            isLoading.value = false
        }
    }

    private suspend fun fetchDepartures() {
        try {
            // We need to get the stop ID from the reverse geocoded result
            reverseGeocodedStop.value?.id?.let { stopId ->
                if (stopId.isNotEmpty()) {
                    transitousService.getStopTimes(stopId, 10).collectLatest { response ->
                        departures.value = response.stopTimes
                    }
                }
            }
        } catch (e: Exception) {
            // Handle error silently or log it
            e.printStackTrace()
        }
    }

    suspend fun refreshDepartures() {
        stop.value?.let { place ->
            fetchDepartures()
        }
    }

    fun savePlace(place: Place) {
        viewModelScope.launch {
            placeDao.insertPlace(PlaceEntity.fromPlace(place))
            isPlaceSaved.value = true
        }
    }

    fun unsavePlace(place: Place) {
        viewModelScope.launch {
            placeDao.deletePlace(PlaceEntity.fromPlace(place))
            isPlaceSaved.value = false
        }
    }
}
