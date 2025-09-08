package earth.maps.cardinal.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import earth.maps.cardinal.data.GeocodeResult
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.PlaceEntity
import earth.maps.cardinal.data.PlaceDao
import earth.maps.cardinal.geocoding.GeocodingService
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.all
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val placeDao: PlaceDao,
    private val geocodingService: GeocodingService
) : ViewModel() {
    
    // Saved places from database
    val savedPlaces = mutableStateOf<List<Place>>(emptyList())
    
    // Search query flow for debouncing
    private val _searchQueryFlow = MutableStateFlow("")
    private val searchQueryFlow: StateFlow<String> = _searchQueryFlow.asStateFlow()

    var searchQuery by mutableStateOf("")
        private set

    val geocodeResults = mutableStateOf<List<GeocodeResult>>(emptyList())
    
    var isSearching by mutableStateOf(false)
        private set
        
    var searchError by mutableStateOf<String?>(null)
        private set

    init {
        // Add sample data to database if it's empty
        viewModelScope.launch {
            if (placeDao.getAllPlaces().all { it.isEmpty() }) {
                val samplePlaces = listOf(
                    PlaceEntity(
                        id = 1,
                        name = "Home",
                        type = "Residence",
                        icon = "home",
                        latitude = 37.7749,
                        longitude = -122.4194,
                        road = "Main Street",
                        city = "San Francisco",
                        state = "CA",
                        postcode = "94105",
                        country = "USA"
                    ),
                    PlaceEntity(
                        id = 2,
                        name = "Work",
                        type = "Office",
                        icon = "work",
                        latitude = 37.7849,
                        longitude = -122.4094,
                        road = "Market Street",
                        city = "San Francisco",
                        state = "CA",
                        postcode = "94103",
                        country = "USA"
                    ),
                    PlaceEntity(
                        id = 3,
                        name = "Coffee Shop",
                        type = "Cafe",
                        icon = "coffee",
                        latitude = 37.7793,
                        longitude = -122.4035,
                        road = "Howard Street",
                        city = "San Francisco",
                        state = "CA",
                        postcode = "94103",
                        country = "USA"
                    )
                )
                placeDao.insertPlaces(samplePlaces)
            }
        }
        
        // Load saved places from database
        viewModelScope.launch {
            placeDao.getAllPlaces().collect { placeEntities ->
                savedPlaces.value = placeEntities.map { it.toPlace() }
            }
        }
        
        // Set up debounced search
        searchQueryFlow
            .debounce(300) // 300ms delay
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isNotEmpty()) {
                    performSearch(query)
                } else {
                    // Clear results when query is empty
                    geocodeResults.value = emptyList()
                    searchError = null
                }
            }
            .launchIn(viewModelScope)
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        _searchQueryFlow.value = query
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            isSearching = true
            searchError = null
            try {
                geocodingService.geocode(query).collect { results ->
                    geocodeResults.value = results
                    isSearching = false
                }
            } catch (e: Exception) {
                // Handle error
                searchError = e.message ?: "An error occurred during search"
                geocodeResults.value = emptyList()
                isSearching = false
            }
        }
    }
}
