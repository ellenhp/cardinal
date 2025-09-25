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
import earth.maps.cardinal.data.room.SavedPlace
import earth.maps.cardinal.data.room.SavedPlaceDao
import earth.maps.cardinal.data.room.SavedPlaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManagePlacesViewModel @Inject constructor(
    private val placeDao: SavedPlaceDao,
    private val savedPlaceRepository: SavedPlaceRepository,
) : ViewModel() {

    val selectedPlace = mutableStateOf<SavedPlace?>(null)
    val isEditingPlace = mutableStateOf(false)
    val placeToEdit = mutableStateOf<SavedPlace?>(null)
    val placeToDelete = mutableStateOf<SavedPlace?>(null)
    private val listId = mutableStateOf<String?>(null)

    fun selectPlace(place: SavedPlace) {
        selectedPlace.value = place
    }

    fun convertToPlace(savedPlace: SavedPlace): Place {
        return savedPlaceRepository.toPlace(savedPlace)
    }

    fun startEditingPlace(place: SavedPlace) {
        placeToEdit.value = place
        isEditingPlace.value = true
    }

    fun cancelEditingPlace() {
        isEditingPlace.value = false
        placeToEdit.value = null
    }

    fun editPlace(updatedPlace: SavedPlace) {
        viewModelScope.launch {
            placeDao.updatePlace(updatedPlace)
            isEditingPlace.value = false
            placeToEdit.value = null
        }
    }

    fun startDeletingPlace(place: SavedPlace) {
        placeToDelete.value = place
    }

    fun cancelDeletingPlace() {
        placeToDelete.value = null
    }

    fun deletePlace(place: SavedPlace) {
        viewModelScope.launch {
            placeDao.deletePlace(place)
            placeToDelete.value = null
        }
    }

    fun pinnedPlaces(): Flow<List<SavedPlace>> {
        return placeDao.getAllPlacesAsFlow().map { list -> list.filter { it.isPinned } }
    }

    fun unpinnedPlaces(): Flow<List<SavedPlace>> {
        return placeDao.getAllPlacesAsFlow().map { list -> list.filter { !it.isPinned } }
    }

    fun clearSelection() {
        selectedPlace.value = null
    }
}
