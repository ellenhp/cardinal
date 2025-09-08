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
            val existingPlace = placeDao.getPlaceById(place.id)
            isPlaceSaved.value = existingPlace != null
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
