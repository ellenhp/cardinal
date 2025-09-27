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
import earth.maps.cardinal.data.room.SavedPlaceRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaceCardViewModel @Inject constructor(
    private val savedPlaceRepository: SavedPlaceRepository,
) : ViewModel() {

    val isPlaceSaved = mutableStateOf(false)
    val place = mutableStateOf<Place?>(null)

    fun setPlace(place: Place) {
        this.place.value = place
        checkIfPlaceIsSaved(place)
    }

    fun checkIfPlaceIsSaved(place: Place) {
        viewModelScope.launch {
            if (place.id != null) {
                val existingPlace = savedPlaceRepository.getPlaceById(place.id).getOrNull()
                isPlaceSaved.value = existingPlace != null
            }
        }
    }

    fun savePlace(place: Place) {
        viewModelScope.launch {
            savedPlaceRepository.savePlace(place)
            isPlaceSaved.value = true
        }
    }

    fun unsavePlace(place: Place) {
        viewModelScope.launch {
            place.id?.let { id ->
                savedPlaceRepository.deletePlace(placeId = id)
            }
            isPlaceSaved.value = false
        }
    }
}
