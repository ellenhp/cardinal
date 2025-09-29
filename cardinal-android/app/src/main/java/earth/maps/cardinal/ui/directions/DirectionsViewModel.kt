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

package earth.maps.cardinal.ui.directions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import earth.maps.cardinal.data.AppPreferenceRepository
import earth.maps.cardinal.data.GeocodeResult
import earth.maps.cardinal.data.LatLng
import earth.maps.cardinal.data.LocationRepository
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.RoutingMode
import earth.maps.cardinal.data.ViewportRepository
import earth.maps.cardinal.data.room.RoutingProfile
import earth.maps.cardinal.data.room.RoutingProfileRepository
import earth.maps.cardinal.data.room.SavedPlaceDao
import earth.maps.cardinal.data.room.SavedPlaceRepository
import earth.maps.cardinal.geocoding.GeocodingService
import earth.maps.cardinal.routing.FerrostarWrapperRepository
import earth.maps.cardinal.routing.RouteRepository
import earth.maps.cardinal.transit.PlanResponse
import earth.maps.cardinal.transit.TransitousService
import earth.maps.cardinal.ui.core.NavigationUtils
import earth.maps.cardinal.ui.core.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.Route
import uniffi.ferrostar.UserLocation
import uniffi.ferrostar.Waypoint
import uniffi.ferrostar.WaypointKind
import java.time.Instant
import javax.inject.Inject
import kotlin.time.ExperimentalTime

// Consolidated route state
data class RouteState(
    val route: Route? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

// Consolidated transit plan state
data class TransitPlanState(
    val planResponse: PlanResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@OptIn(FlowPreview::class)
@HiltViewModel
class DirectionsViewModel @Inject constructor(
    private val geocodingService: GeocodingService,
    private val ferrostarWrapperRepository: FerrostarWrapperRepository,
    private val viewportRepository: ViewportRepository,
    private val placeDao: SavedPlaceDao,
    private val savedPlaceRepository: SavedPlaceRepository,
    private val locationRepository: LocationRepository,
    private val routingProfileRepository: RoutingProfileRepository,
    private val routeRepository: RouteRepository,
    private val appPreferenceRepository: AppPreferenceRepository,
    private val transitousService: TransitousService,
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

    // Consolidated transit plan state
    var planState by mutableStateOf(TransitPlanState())
        private set

    // Saved places for quick suggestions
    val savedPlaces = placeDao.getAllPlacesAsFlow().map { list ->
        list.map { savedPlaceRepository.toPlace(it) }
    }

    var isGettingLocation by mutableStateOf(false)
        private set

    private var haveManuallySetDeparture: Boolean = false

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

        // Initialize with the default profile for the current routing mode
        initializeDefaultProfileForMode(selectedRoutingMode)

        // Set initial routing mode from preferences
        selectedRoutingMode = appPreferenceRepository.lastRoutingMode.value.let { modeString ->
            RoutingMode.entries.find { it.value == modeString } ?: RoutingMode.AUTO
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        _searchQueryFlow.value = query
    }

    suspend fun initializeDeparture() {
        if (!appPreferenceRepository.continuousLocationTracking.value) {
            return
        }
        val defaultDeparture = getCurrentLocationAsPlace()
        defaultDeparture?.let {
            if (!haveManuallySetDeparture) {
                updateFromPlace(it)
            }
        }
    }

    fun updateFromPlace(place: Place?) {
        haveManuallySetDeparture = true
        fromPlace = place
        fetchDirectionsIfNeeded()
    }

    fun updateToPlace(place: Place?) {
        toPlace = place
        fetchDirectionsIfNeeded()
    }

    private fun fetchDirectionsIfNeeded() {
        val origin = fromPlace
        val destination = toPlace
        if (origin != null && destination != null) {
            fetchDirections(origin, destination)
        } else {
            routeState = RouteState()
            planState = TransitPlanState()
        }
    }

    private fun fetchDirections(origin: Place, destination: Place) {
        if (selectedRoutingMode == RoutingMode.PUBLIC_TRANSPORT) {
            fetchTransitDirections(origin, destination)
        } else {
            fetchDrivingDirections(origin, destination)
        }
    }

    private fun fetchDrivingDirections(origin: Place, destination: Place) {
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

    @OptIn(ExperimentalTime::class)
    private fun fetchTransitDirections(origin: Place, destination: Place) {
        viewModelScope.launch {
            planState = planState.copy(isLoading = true, error = null)

            try {
                transitousService.getPlan(
                    from = LatLng(origin.latLng.latitude, origin.latLng.longitude),
                    to = LatLng(destination.latLng.latitude, destination.latLng.longitude),
                    timetableView = false,
                    withFares = true,
                ).collect { planResponse ->
                    planState = planState.copy(
                        planResponse = planResponse,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error while fetching transit plan", e)
                planState = planState.copy(
                    planResponse = null,
                    isLoading = false,
                    error = e.message ?: "An error occurred while fetching transit directions"
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
        timestamp = Instant.now(),
        speed = null
    )

    fun updateRoutingMode(mode: RoutingMode) {
        selectedRoutingMode = mode
        appPreferenceRepository.setLastRoutingMode(mode.value)
        // Load the default profile for the new mode
        initializeDefaultProfileForMode(mode)
        fetchDirectionsIfNeeded()
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
                            options?.let {
                                ferrostarWrapperRepository.setOptionsForMode(
                                    selectedRoutingMode, it
                                )
                            }
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

            fetchDirectionsIfNeeded()
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
        val modes = mutableListOf(
            RoutingMode.PUBLIC_TRANSPORT,
            RoutingMode.BICYCLE,
            RoutingMode.PEDESTRIAN,
            RoutingMode.AUTO,
        )

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
                        options?.let {
                            selectedRoutingProfile = profile
                            ferrostarWrapperRepository.setOptionsForMode(mode, it)
                        }
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


    fun startNavigation(navController: NavController) {
        routeState.route?.let { route ->
            NavigationUtils.navigate(
                navController,
                Screen.TurnByTurnNavigation(
                    routeId = routeRepository.storeRoute(route),
                    routingMode = selectedRoutingMode
                )
            )
        }
    }

    fun flipDestinations() {
        val tempFrom = fromPlace
        val tempTo = toPlace
        fromPlace = tempTo
        toPlace = tempFrom
        fetchDirectionsIfNeeded()
    }

    fun recalculateDirections() {
        val origin = fromPlace
        val destination = toPlace
        if (origin != null && destination != null) {
            if (origin.isMyLocation || destination.isMyLocation) {
                refreshMyLocationPlacesAndRecalculate(origin, destination)
            } else {
                fetchDirections(origin, destination)
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

                fetchDirections(updatedOrigin, updatedDestination)
            } catch (_: Exception) {
                fetchDirections(origin, destination)
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
    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
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
