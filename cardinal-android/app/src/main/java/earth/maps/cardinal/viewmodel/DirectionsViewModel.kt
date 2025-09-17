package earth.maps.cardinal.viewmodel

import android.util.Log
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
import earth.maps.cardinal.data.RoutingProfile
import earth.maps.cardinal.data.RoutingProfileRepository
import earth.maps.cardinal.data.ViewportRepository
import earth.maps.cardinal.geocoding.GeocodingService
import earth.maps.cardinal.routing.FerrostarWrapperRepository
import earth.maps.cardinal.routing.RoutingOptions
import earth.maps.cardinal.ui.NavigationCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val locationRepository: LocationRepository,
    private val routingProfileRepository: RoutingProfileRepository
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

    var selectedRoutingProfile by mutableStateOf<RoutingProfile?>(null)
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
                // Get the appropriate FerrostarWrapper based on routing mode and profile
                val ferrostarWrapper = if (selectedRoutingProfile != null) {
                    // Use custom routing profile - update options on existing wrapper
                    val profileWithOptions = routingProfileRepository.getProfileWithOptions(selectedRoutingProfile!!.id)
                    profileWithOptions.fold(
                        onSuccess = { pair ->
                            pair?.let { (_, options) ->
                                // Update options on the appropriate wrapper
                                ferrostarWrapperRepository.setOptionsForMode(selectedRoutingMode, options)
                                // Return the updated wrapper
                                when (selectedRoutingMode) {
                                    RoutingMode.AUTO -> ferrostarWrapperRepository.driving
                                    RoutingMode.PEDESTRIAN -> ferrostarWrapperRepository.walking
                                    RoutingMode.BICYCLE -> ferrostarWrapperRepository.cycling
                                    else -> ferrostarWrapperRepository.driving
                                }
                            } ?: ferrostarWrapperRepository.driving // Fallback if profile not found
                        },
                        onFailure = {
                            // Fallback to default if profile loading fails
                            when (selectedRoutingMode) {
                                RoutingMode.AUTO -> ferrostarWrapperRepository.driving
                                RoutingMode.PEDESTRIAN -> ferrostarWrapperRepository.walking
                                RoutingMode.BICYCLE -> ferrostarWrapperRepository.cycling
                                else -> ferrostarWrapperRepository.driving
                            }
                        }
                    )
                } else {
                    // Use default wrapper based on routing mode
                    when (selectedRoutingMode) {
                        RoutingMode.AUTO -> ferrostarWrapperRepository.driving
                        RoutingMode.PEDESTRIAN -> ferrostarWrapperRepository.walking
                        RoutingMode.BICYCLE -> ferrostarWrapperRepository.cycling
                        else -> ferrostarWrapperRepository.driving
                    }
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
                val routes = withContext(Dispatchers.IO) {
                    ferrostarWrapper.core.getRoutes(userLocation, waypoints)
                }
                ferrostarRoute = routes.firstOrNull()
                isRouteLoading = false
            } catch (e: Exception) {
                Log.e(TAG, "Error while fetching route", e)
                routeError = e.message ?: "An error occurred while fetching the route"
                ferrostarRoute = null
                isRouteLoading = false
            }
        }
    }

    fun updateRoutingMode(mode: RoutingMode) {
        selectedRoutingMode = mode
        // Clear selected profile when mode changes, as profiles are mode-specific
        selectedRoutingProfile = null
        // Reset wrapper options to defaults when mode changes
        ferrostarWrapperRepository.resetOptionsToDefaultsForMode(mode)
        fetchRouteIfNeeded()
    }

    fun selectRoutingProfile(profile: RoutingProfile?) {
        selectedRoutingProfile = profile
        // Reset wrapper options to defaults when switching to "Default" profile
        if (profile == null) {
            ferrostarWrapperRepository.resetOptionsToDefaultsForMode(selectedRoutingMode)
        }
        fetchRouteIfNeeded()
    }

    /**
     * Gets available routing profiles for the current routing mode.
     */
    fun getAvailableProfilesForCurrentMode() = routingProfileRepository.getProfilesForMode(selectedRoutingMode)

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
            // Check if we need to refresh location for "my location" places
            val needsLocationRefresh = origin.isMyLocation || destination.isMyLocation

            if (needsLocationRefresh) {
                // Launch coroutine to refresh location for "my location" places
                viewModelScope.launch {
                    isGettingLocation = true
                    try {
                        var updatedOrigin = origin
                        var updatedDestination = destination

                        // Refresh origin location if it's "my location"
                        if (origin.isMyLocation) {
                            locationRepository.getFreshCurrentLocationAsPlace()
                                ?.let { freshLocation ->
                                    updatedOrigin = freshLocation
                                    fromPlace = freshLocation
                                }
                        }

                        // Refresh destination location if it's "my location"
                        if (destination.isMyLocation) {
                            locationRepository.getFreshCurrentLocationAsPlace()
                                ?.let { freshLocation ->
                                    updatedDestination = freshLocation
                                    toPlace = freshLocation
                                }
                        }

                        // Fetch route with updated locations (ensure they're not null)
                        if (updatedOrigin != null && updatedDestination != null) {
                            fetchRoute(updatedOrigin, updatedDestination)
                        } else {
                            // Fallback to original locations if refresh failed
                            fetchRoute(origin, destination)
                        }
                    } catch (e: Exception) {
                        // If location refresh fails, use original locations
                        fetchRoute(origin, destination)
                    } finally {
                        isGettingLocation = false
                    }
                }
            } else {
                // No location refresh needed, just recalculate with existing places
                fetchRoute(origin, destination)
            }
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

    suspend fun getCurrentLocationAsPlace(): Place? {
        isGettingLocation = true
        return try {
            locationRepository.getCurrentLocationAsPlace()
        } finally {
            isGettingLocation = false
        }
    }

    /**
     * Checks if location permissions are granted before attempting to get location.
     * This method should be called before any location-related operations.
     */
    fun hasLocationPermission(context: android.content.Context): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /**
     * Safely gets current location as a Place, handling permission checks.
     * Returns null if permissions are not granted or location cannot be obtained.
     */
    suspend fun getCurrentLocationAsPlaceSafe(context: android.content.Context): Place? {
        return if (hasLocationPermission(context)) {
            getCurrentLocationAsPlace()
        } else {
            null
        }
    }

    /**
     * Creates a "My Location" Place object with the given coordinates.
     * This is a public method to allow other components to create consistent "My Location" places.
     */
    fun createMyLocationPlace(latLng: LatLng): Place {
        return locationRepository.createMyLocationPlace(latLng)
    }

    companion object {
        private const val TAG = "DirectionsViewModel"
    }
}
