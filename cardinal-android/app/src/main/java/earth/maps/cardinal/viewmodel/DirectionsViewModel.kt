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
import earth.maps.cardinal.ui.NavigationCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

// Consolidated route state
data class RouteState(
    val route: Route? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

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

    // Consolidated route state
    var routeState by mutableStateOf(RouteState())
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

        // Initialize with the default profile for the current routing mode
        initializeDefaultProfileForMode(selectedRoutingMode)
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
            routeState = RouteState()
        }
    }

    private fun fetchRoute(origin: Place, destination: Place) {
        viewModelScope.launch {
            routeState = routeState.copy(isLoading = true, error = null)

            try {
                val ferrostarWrapper = getFerrostarWrapper()
                val waypoints = createWaypoints(destination)
                val userLocation = createUserLocation(origin)

                val routes = withContext(Dispatchers.IO) {
                    ferrostarWrapper.core.getRoutes(userLocation, waypoints)
                }

                routeState = routeState.copy(
                    route = routes.firstOrNull(),
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error while fetching route", e)
                routeState = routeState.copy(
                    route = null,
                    isLoading = false,
                    error = e.message ?: "An error occurred while fetching the route"
                )
            }
        }
    }

    private fun getFerrostarWrapper() = when (selectedRoutingMode) {
        RoutingMode.AUTO -> ferrostarWrapperRepository.driving
        RoutingMode.PEDESTRIAN -> ferrostarWrapperRepository.walking
        RoutingMode.BICYCLE -> ferrostarWrapperRepository.cycling
        else -> ferrostarWrapperRepository.driving
    }

    private fun createWaypoints(destination: Place) = listOf(
        Waypoint(
            coordinate = GeographicCoordinate(
                destination.latLng.latitude,
                destination.latLng.longitude
            ),
            kind = WaypointKind.BREAK
        )
    )

    private fun createUserLocation(origin: Place) = UserLocation(
        coordinates = GeographicCoordinate(origin.latLng.latitude, origin.latLng.longitude),
        horizontalAccuracy = 10.0,
        courseOverGround = null,
        timestamp = java.time.Instant.now(),
        speed = null
    )

    fun updateRoutingMode(mode: RoutingMode) {
        selectedRoutingMode = mode
        // Load the default profile for the new mode
        initializeDefaultProfileForMode(mode)
        fetchRouteIfNeeded()
    }

    /**
     * Ensures the selected routing mode is valid by checking if it's in the available modes list.
     * If not, falls back to AUTO mode. This should be called when available modes change.
     */
    private fun ensureSelectedModeIsValid(availableModes: List<RoutingMode>) {
        if (selectedRoutingMode !in availableModes) {
            // Fall back to AUTO mode if current mode is no longer available
            updateRoutingMode(RoutingMode.AUTO)
        }
    }

    fun selectRoutingProfile(profile: RoutingProfile?) {
        selectedRoutingProfile = profile

        viewModelScope.launch {
            // Apply profile options to the ferrostar wrapper
            if (profile != null) {
                // Use custom routing profile - update options on existing wrapper
                val profileWithOptions = routingProfileRepository.getProfileWithOptions(profile.id)
                profileWithOptions.fold(
                    onSuccess = { pair ->
                        pair?.let { (_, options) ->
                            // Update options on the appropriate wrapper
                            ferrostarWrapperRepository.setOptionsForMode(
                                selectedRoutingMode,
                                options
                            )
                        }
                    },
                    onFailure = {
                        // Fallback to default if profile loading fails
                        ferrostarWrapperRepository.resetOptionsToDefaultsForMode(selectedRoutingMode)
                    }
                )
            } else {
                // User explicitly selected "Default" - use built-in defaults
                selectedRoutingProfile = null
                ferrostarWrapperRepository.resetOptionsToDefaultsForMode(selectedRoutingMode)
            }

            fetchRouteIfNeeded()
        }

    }

    /**
     * Gets available routing profiles for the current routing mode.
     */
    fun getAvailableProfilesForCurrentMode() =
        routingProfileRepository.getProfilesForMode(selectedRoutingMode)

    /**
     * Gets available routing modes for display in the UI.
     * Always includes AUTO, PEDESTRIAN, and BICYCLE.
     * Conditionally includes TRUCK, MOTOR_SCOOTER, and MOTORCYCLE only if custom profiles exist for those modes.
     */
    fun getAvailableRoutingModes() = combine(
        routingProfileRepository.getProfilesForMode(RoutingMode.TRUCK),
        routingProfileRepository.getProfilesForMode(RoutingMode.MOTOR_SCOOTER),
        routingProfileRepository.getProfilesForMode(RoutingMode.MOTORCYCLE)
    ) { truckProfiles, motorScooterProfiles, motorcycleProfiles ->
        val modes = mutableListOf(RoutingMode.AUTO, RoutingMode.PEDESTRIAN, RoutingMode.BICYCLE)

        // Add conditional modes only if they have custom profiles
        if (truckProfiles.isNotEmpty()) {
            modes.add(RoutingMode.TRUCK)
        }
        if (motorScooterProfiles.isNotEmpty()) {
            modes.add(RoutingMode.MOTOR_SCOOTER)
        }
        if (motorcycleProfiles.isNotEmpty()) {
            modes.add(RoutingMode.MOTORCYCLE)
        }

        // Ensure the selected mode is still valid
        ensureSelectedModeIsValid(modes)

        modes
    }

    /**
     * Initializes the default routing profile for the given mode.
     * This loads the saved default profile from the database and applies its options.
     * If no default profile exists, sets selectedRoutingProfile to null and uses built-in defaults.
     */
    private fun initializeDefaultProfileForMode(mode: RoutingMode) {
        viewModelScope.launch {
            routingProfileRepository.getDefaultProfile(mode).fold(
                onSuccess = { profileWithOptions ->
                    if (profileWithOptions != null) {
                        val (profile, options) = profileWithOptions
                        selectedRoutingProfile = profile
                        ferrostarWrapperRepository.setOptionsForMode(mode, options)
                    } else {
                        // No default profile exists, use built-in defaults
                        selectedRoutingProfile = null
                        ferrostarWrapperRepository.resetOptionsToDefaultsForMode(mode)
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to load default profile for mode $mode", error)
                    // Fallback to built-in defaults
                    selectedRoutingProfile = null
                    ferrostarWrapperRepository.resetOptionsToDefaultsForMode(mode)
                }
            )
        }
    }


    fun startNavigation(navigationCoordinator: NavigationCoordinator) {
        routeState.route?.let { route ->
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
            if (origin.isMyLocation || destination.isMyLocation) {
                refreshMyLocationPlacesAndRecalculate(origin, destination)
            } else {
                fetchRoute(origin, destination)
            }
        }
    }

    private fun refreshMyLocationPlacesAndRecalculate(origin: Place, destination: Place) {
        viewModelScope.launch {
            isGettingLocation = true
            try {
                val updatedOrigin = if (origin.isMyLocation) {
                    locationRepository.getFreshCurrentLocationAsPlace()?.also { fromPlace = it }
                        ?: origin
                } else origin

                val updatedDestination = if (destination.isMyLocation) {
                    locationRepository.getFreshCurrentLocationAsPlace()?.also { toPlace = it }
                        ?: destination
                } else destination

                fetchRoute(updatedOrigin, updatedDestination)
            } catch (e: Exception) {
                fetchRoute(origin, destination)
            } finally {
                isGettingLocation = false
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

    /**
     * Gets current location as a Place, handling loading state.
     */
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
     */
    fun createMyLocationPlace(latLng: LatLng): Place {
        return locationRepository.createMyLocationPlace(latLng)
    }

    companion object {
        private const val TAG = "DirectionsViewModel"
    }
}
