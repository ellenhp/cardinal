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
