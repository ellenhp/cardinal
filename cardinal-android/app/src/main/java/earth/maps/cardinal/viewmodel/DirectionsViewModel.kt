package earth.maps.cardinal.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import earth.maps.cardinal.data.GeocodeResult
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.RoutingMode
import earth.maps.cardinal.geocoding.GeocodingService
import earth.maps.cardinal.routing.RouteResult
import earth.maps.cardinal.routing.RoutingService
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class DirectionsViewModel @Inject constructor(
    private val geocodingService: GeocodingService,
    private val routingService: RoutingService
) : ViewModel() {
    
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

    // Directions state
    var fromPlace by mutableStateOf<Place?>(null)
        private set

    var toPlace by mutableStateOf<Place?>(null)
        private set

    var selectedRoutingMode by mutableStateOf(RoutingMode.AUTO)
        private set

    // Route result state
    var routeResult by mutableStateOf<RouteResult?>(null)
        private set
    
    var isRouteLoading by mutableStateOf(false)
        private set
    
    var routeError by mutableStateOf<String?>(null)
        private set

    init {
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

    fun updateFromPlace(place: Place?) {
        fromPlace = place
        fetchRouteIfNeeded()
    }

    fun updateToPlace(place: Place?) {
        toPlace = place
        fetchRouteIfNeeded()
    }
    
    private fun fetchRouteIfNeeded() {
        val origin = fromPlace
        val destination = toPlace
        if (origin != null && destination != null) {
            fetchRoute(origin, destination)
        }
    }
    
    private fun fetchRoute(origin: Place, destination: Place) {
        viewModelScope.launch {
            isRouteLoading = true
            routeError = null
            try {
                // Convert RoutingMode to the string format expected by the routing service
                val profile = when (selectedRoutingMode) {
                    RoutingMode.AUTO -> "auto"
                    RoutingMode.PEDESTRIAN -> "pedestrian"
                    RoutingMode.BICYCLE -> "bicycle"
                }
                
                routingService.getRoute(
                    origin = origin,
                    destination = destination,
                    profile = profile
                ).collect { result ->
                    routeResult = result
                    isRouteLoading = false
                }
            } catch (e: Exception) {
                routeError = e.message ?: "An error occurred while fetching the route"
                routeResult = null
                isRouteLoading = false
            }
        }
    }

    fun updateRoutingMode(mode: RoutingMode) {
        selectedRoutingMode = mode
        fetchRouteIfNeeded()
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
