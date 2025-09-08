package earth.maps.cardinal.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.PlaceDao
import earth.maps.cardinal.data.PlaceEntity
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
