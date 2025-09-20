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
class ManagePlacesViewModel @Inject constructor(
    private val placeDao: PlaceDao
) : ViewModel() {

    val places = mutableStateOf<List<Place>>(emptyList())
    val selectedPlace = mutableStateOf<Place?>(null)
    val isEditingPlace = mutableStateOf(false)
    val placeToEdit = mutableStateOf<Place?>(null)
    val placeToDelete = mutableStateOf<Place?>(null)

    init {
        loadPlaces()
    }

    private fun loadPlaces() {
        viewModelScope.launch {
            placeDao.getAllPlaces().collect { placeEntities ->
                places.value = placeEntities.map { it.toPlace() }
            }
        }
    }

    fun selectPlace(place: Place) {
        selectedPlace.value = place
    }

    fun startEditingPlace(place: Place) {
        placeToEdit.value = place
        isEditingPlace.value = true
    }

    fun cancelEditingPlace() {
        isEditingPlace.value = false
        placeToEdit.value = null
    }

    fun editPlace(updatedPlace: Place) {
        viewModelScope.launch {
            placeDao.updatePlace(PlaceEntity.fromPlace(updatedPlace))
            isEditingPlace.value = false
            placeToEdit.value = null
            // Reload places to reflect changes
            loadPlaces()
        }
    }

    fun startDeletingPlace(place: Place) {
        placeToDelete.value = place
    }

    fun cancelDeletingPlace() {
        placeToDelete.value = null
    }

    fun deletePlace(place: Place) {
        viewModelScope.launch {
            placeDao.deletePlace(PlaceEntity.fromPlace(place))
            placeToDelete.value = null
            // Reload places to reflect changes
            loadPlaces()
        }
    }

    fun clearSelection() {
        selectedPlace.value = null
    }
}
