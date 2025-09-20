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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaceCardViewModel @Inject constructor(
    private val placeDao: PlaceDao
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
                val existingPlace = placeDao.getPlaceById(place.id)
                isPlaceSaved.value = existingPlace != null
            }
        }
    }

    fun toggleSavePlace(place: Place) {
        viewModelScope.launch {
            if (isPlaceSaved.value) {
                // Remove place from favorites
                placeDao.deletePlace(PlaceEntity.fromPlace(place))
                isPlaceSaved.value = false
            } else {
                // Add place to favorites
                placeDao.insertPlace(PlaceEntity.fromPlace(place))
                isPlaceSaved.value = true
            }
        }
    }

    fun savePlace(place: Place) {
        viewModelScope.launch {
            placeDao.insertPlace(PlaceEntity.fromPlace(place))
            isPlaceSaved.value = true
        }
    }

    fun savePlaceAsHome(place: Place) {
        viewModelScope.launch {
            placeDao.insertPlace(PlaceEntity.fromPlace(place.copy(icon = "home")))
            isPlaceSaved.value = true
        }
    }

    fun savePlaceAsWork(place: Place) {
        viewModelScope.launch {
            placeDao.insertPlace(PlaceEntity.fromPlace(place.copy(icon = "work")))
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
