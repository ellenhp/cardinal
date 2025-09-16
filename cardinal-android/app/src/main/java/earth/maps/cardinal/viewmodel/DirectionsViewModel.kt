package earth.maps.cardinal.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import earth.maps.cardinal.data.GeocodeResult
import earth.maps.cardinal.data.LatLng
import earth.maps.cardinal.data.LocationRepository
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.PlaceDao
import earth.maps.cardinal.data.RoutingMode
import earth.maps.cardinal.data.ViewportRepository
import earth.maps.cardinal.geocoding.GeocodingService
import earth.maps.cardinal.routing.FerrostarWrapperRepository
import earth.maps.cardinal.ui.NavigationCoordinator
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.Route
import uniffi.ferrostar.UserLocation
import uniffi.ferrostar.Waypoint
import uniffi.ferrostar.WaypointKind
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class DirectionsViewModel @Inject constructor(
    private val geocodingService: GeocodingService,
    private val ferrostarWrapperRepository: FerrostarWrapperRepository,
    private val viewportRepository: ViewportRepository,
    private val placeDao: PlaceDao,
    private val locationRepository: LocationRepository
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

    // Route result state - using Ferrostar's native Route format
    var ferrostarRoute by mutableStateOf<Route?>(null)
        private set

    var isRouteLoading by mutableStateOf(false)
        private set

    var routeError by mutableStateOf<String?>(null)
        private set

    // Saved places for quick suggestions
    val savedPlaces = mutableStateOf<List<Place>>(emptyList())

    var isGettingLocation by mutableStateOf(false)
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

        // Load saved places
        viewModelScope.launch {
            placeDao.getAllPlaces().collect { placeEntities ->
                savedPlaces.value = placeEntities.map { it.toPlace() }
            }
        }
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
        } else {
            isRouteLoading = false
            routeError = null
            ferrostarRoute = null
        }
    }

    private fun fetchRoute(origin: Place, destination: Place) {
        viewModelScope.launch {
            isRouteLoading = true
            routeError = null
            try {
                // Get the appropriate FerrostarWrapper based on routing mode
                val ferrostarWrapper = when (selectedRoutingMode) {
                    RoutingMode.AUTO -> ferrostarWrapperRepository.driving
                    RoutingMode.PEDESTRIAN -> ferrostarWrapperRepository.walking
                    RoutingMode.BICYCLE -> ferrostarWrapperRepository.cycling
                }

                // Create waypoints for Ferrostar
                val waypoints = listOf(
                    Waypoint(
                        coordinate = GeographicCoordinate(
                            origin.latLng.latitude,
                            origin.latLng.longitude
                        ),
                        kind = WaypointKind.BREAK
                    ),
                    Waypoint(
                        coordinate = GeographicCoordinate(
                            destination.latLng.latitude,
                            destination.latLng.longitude
                        ),
                        kind = WaypointKind.BREAK
                    )
                )
                val userLocation = UserLocation(
                    coordinates = GeographicCoordinate(
                        origin.latLng.latitude,
                        origin.latLng.longitude
                    ),
                    horizontalAccuracy = 10.0,
                    courseOverGround = null,
                    timestamp = java.time.Instant.now(),
                    speed = null
                )

                // Get routes from Ferrostar
                val routes = ferrostarWrapper.core.getRoutes(userLocation, waypoints)
                ferrostarRoute = routes.firstOrNull()
                isRouteLoading = false
            } catch (e: Exception) {
                routeError = e.message ?: "An error occurred while fetching the route"
                ferrostarRoute = null
                isRouteLoading = false
            }
        }
    }

    fun updateRoutingMode(mode: RoutingMode) {
        selectedRoutingMode = mode
        fetchRouteIfNeeded()
    }

    fun startNavigation(navigationCoordinator: NavigationCoordinator) {
        ferrostarRoute?.let { route ->
            navigationCoordinator.navigateToTurnByTurnWithFerrostarRoute(route, selectedRoutingMode)
        }
    }

    fun flipDestinations() {
        val tempFrom = fromPlace
        val tempTo = toPlace
        fromPlace = tempTo
        toPlace = tempFrom
        fetchRouteIfNeeded()
    }

    fun recalculateRoute() {
        val origin = fromPlace
        val destination = toPlace
        if (origin != null && destination != null) {
            fetchRoute(origin, destination)
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            isSearching = true
            searchError = null
            try {
                // Use fromPlace as focus point for viewport biasing if available,
                // otherwise fall back to current viewport center
                val focusPoint = fromPlace?.latLng ?: viewportRepository.viewportCenter.value
                geocodingService.geocode(query, focusPoint).collect { results ->
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

    suspend fun getCurrentLocationAsPlace(context: android.content.Context): Place? {
        isGettingLocation = true
        return try {
            val location = locationRepository.getCurrentLocation(context)
            location?.let {
                Place(
                    id = Int.MIN_VALUE, // Special ID for "My Location"
                    name = "My Location",
                    type = "Current Location",
                    icon = "location",
                    latLng = LatLng(it.latitude, it.longitude),
                    isMyLocation = true
                )
            } ?: run {
                // Fallback to viewport center if location is not available
                val currentLatLng = viewportRepository.viewportCenter.value ?: LatLng(37.7749, -122.4194)
                Place(
                    id = Int.MIN_VALUE,
                    name = "My Location",
                    type = "Current Location",
                    icon = "location",
                    latLng = currentLatLng,
                    isMyLocation = true
                )
            }
        } finally {
            isGettingLocation = false
        }
    }
}
