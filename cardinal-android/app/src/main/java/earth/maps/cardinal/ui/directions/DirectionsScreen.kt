/*
 *     Cardinal Maps
 *     Copyright (C) 2025 Cardinal Maps Authors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package earth.maps.cardinal.ui.directions

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.drawable
import earth.maps.cardinal.R.string
import earth.maps.cardinal.data.AppPreferenceRepository
import earth.maps.cardinal.data.GeoUtils
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.RoutingMode
import earth.maps.cardinal.data.deduplicateSearchResults
import earth.maps.cardinal.data.room.RoutingProfile
import earth.maps.cardinal.ui.core.NavigationUtils
import earth.maps.cardinal.ui.core.Screen
import earth.maps.cardinal.ui.place.SearchResults
import earth.maps.cardinal.ui.saved.QuickSuggestions
import io.github.dellisd.spatialk.geojson.BoundingBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import uniffi.ferrostar.Route

enum class FieldFocusState {
    NONE, FROM, TO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectionsScreen(
    viewModel: DirectionsViewModel,
    onPeekHeightChange: (dp: Dp) -> Unit,
    onBack: () -> Unit,
    onFullExpansionRequired: () -> Job,
    navController: NavController,
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    appPreferences: AppPreferenceRepository
) {
    val availableProfiles by viewModel.getAvailableProfilesForCurrentMode()
        .collectAsState(initial = emptyList())

    var fieldFocusState by remember { mutableStateOf(FieldFocusState.NONE) }
    val isAnyFieldFocused = fieldFocusState != FieldFocusState.NONE

    // Track pending location request for auto-retry after permission grant
    var pendingLocationRequest by remember { mutableStateOf<FieldFocusState?>(null) }

    // Get route result from ViewModel
    val routeState = viewModel.routeState

    val savedPlaces by viewModel.savedPlaces.collectAsState(initial = emptyList())

    val coroutineScope = rememberCoroutineScope()

    // Auto-retry location request when permissions are granted
    AutoRetryMyLocation(
        hasLocationPermission = hasLocationPermission,
        pendingLocationRequest = pendingLocationRequest,
        coroutineScope = coroutineScope,
        viewModel = viewModel,
        appPreferences = appPreferences,
        onCompletion = {
            fieldFocusState = FieldFocusState.NONE
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(dimen.padding))
    ) {
        if (isAnyFieldFocused) {
            DirectionsScreenFocusedField(
                viewModel = viewModel,
                fieldFocusState = fieldFocusState,
                onFieldFocusStateChange = {
                    fieldFocusState = it
                    if (fieldFocusState != FieldFocusState.NONE) {
                        onFullExpansionRequired()
                    }
                },
                savedPlaces = savedPlaces,
                hasLocationPermission = hasLocationPermission,
                onRequestLocationPermission = {
                    pendingLocationRequest = fieldFocusState
                    onRequestLocationPermission()
                },
                coroutineScope = coroutineScope
            )
        } else {
            DirectionsScreenFullUI(
                viewModel = viewModel,
                onPeekHeightChange = onPeekHeightChange,
                onBack = onBack,
                navController = navController,
                onFieldFocusStateChange = { fieldFocusState = it },
                fieldFocusState = fieldFocusState,
                routeState = routeState,
                availableProfiles = availableProfiles,
                appPreferences = appPreferences,
                hasNotificationPermission = hasNotificationPermission,
                onRequestNotificationPermission = onRequestNotificationPermission
            )
        }
    }
}

@Composable
private fun AutoRetryMyLocation(
    hasLocationPermission: Boolean,
    pendingLocationRequest: FieldFocusState?,
    coroutineScope: CoroutineScope,
    viewModel: DirectionsViewModel,
    appPreferences: AppPreferenceRepository,
    onCompletion: () -> Unit,
) {
    LaunchedEffect(hasLocationPermission, pendingLocationRequest) {
        if (hasLocationPermission && pendingLocationRequest != null) {
            val targetField = pendingLocationRequest

            // Automatically fetch location for the target field
            coroutineScope.launch {
                val myLocationPlace = viewModel.getCurrentLocationAsPlace()
                myLocationPlace?.let { place ->
                    // Update the appropriate place based on which field was focused
                    if (targetField == FieldFocusState.FROM) {
                        viewModel.updateFromPlace(place)
                    } else {
                        viewModel.updateToPlace(place)
                    }
                    onCompletion()
                }
            }
        } else if (hasLocationPermission && appPreferences.continuousLocationTracking.value) {
            coroutineScope.launch {
                viewModel.initializeDeparture()
            }
        }
    }
}

@Composable
private fun DirectionsScreenFullUI(
    viewModel: DirectionsViewModel,
    onPeekHeightChange: (Dp) -> Unit,
    onBack: () -> Unit,
    navController: NavController,
    onFieldFocusStateChange: (FieldFocusState) -> Unit,
    fieldFocusState: FieldFocusState,
    routeState: RouteState,
    availableProfiles: List<RoutingProfile>,
    appPreferences: AppPreferenceRepository,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current

    // Show full UI when no field is focused
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val heightInDp = with(density) { coordinates.size.height.toDp() }
                onPeekHeightChange(heightInDp)
            }) {
        // Header with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(drawable.ic_arrow_back),
                    contentDescription = stringResource(string.back)
                )
            }

            Text(
                text = stringResource(string.directions),
                style = MaterialTheme.typography.headlineSmall
            )

            // Spacer to balance the row
            Box(modifier = Modifier.size(48.dp))
        }

        // From and To fields
        PlaceField(
            label = stringResource(string.from),
            place = viewModel.fromPlace,
            onCleared = { viewModel.updateFromPlace(null) },
            onTextChange = { viewModel.updateSearchQuery(it) },
            onTextFieldFocusChange = {
                onFieldFocusStateChange(if (it) FieldFocusState.FROM else FieldFocusState.NONE)
            },
            isFocused = fieldFocusState == FieldFocusState.FROM,
            showRecalculateButton = viewModel.fromPlace != null && viewModel.toPlace != null,
            onRecalculateClick = { viewModel.recalculateDirections() },
            isRouteLoading = routeState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        PlaceField(
            label = stringResource(string.to),
            place = viewModel.toPlace,
            onCleared = { viewModel.updateToPlace(null) },
            onTextChange = { viewModel.updateSearchQuery(it) },
            onTextFieldFocusChange = {
                onFieldFocusStateChange(if (it) FieldFocusState.TO else FieldFocusState.NONE)
            },
            isFocused = fieldFocusState == FieldFocusState.TO,
            showFlipButton = viewModel.fromPlace != null && viewModel.toPlace != null,
            onFlipClick = { viewModel.flipDestinations() },
            isRouteLoading = routeState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = dimensionResource(dimen.padding))
        )

        val availableRoutingModes by viewModel.getAvailableRoutingModes().collectAsState(
            initial = listOf(
                RoutingMode.PUBLIC_TRANSPORT,
                RoutingMode.BICYCLE,
                RoutingMode.PEDESTRIAN,
                RoutingMode.AUTO,
            )
        )

        RoutingModeSelector(
            availableModes = availableRoutingModes,
            selectedMode = viewModel.selectedRoutingMode,
            onModeSelected = { viewModel.updateRoutingMode(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = dimensionResource(dimen.padding_minor))
        )

        RoutingProfileSelector(
            modifier = Modifier.fillMaxWidth(),
            viewModel = viewModel,
            availableProfiles = availableProfiles
        )

        // Inset horizontal divider
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = dimensionResource(dimen.padding) / 2),
            thickness = DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }

    // Route results
    DirectionsRouteResults(
        viewModel = viewModel,
        routeState = routeState,
        navController = navController,
        appPreferences = appPreferences,
        hasNotificationPermission = hasNotificationPermission,
        onRequestNotificationPermission = onRequestNotificationPermission
    )
}

@Composable
private fun DirectionsRouteResults(
    viewModel: DirectionsViewModel,
    routeState: RouteState,
    navController: NavController,
    appPreferences: AppPreferenceRepository,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit
) {
    val planState = viewModel.planState
    if (viewModel.selectedRoutingMode == RoutingMode.PUBLIC_TRANSPORT) {
        TransitRouteResults(
            planState = planState,
            navController = navController,
            viewModel = viewModel,
            appPreferences = appPreferences
        )
    } else {
        NonTransitRouteResults(
            routeState = routeState,
            viewModel = viewModel,
            navController = navController,
            appPreferences = appPreferences,
            hasNotificationPermission = hasNotificationPermission,
            onRequestNotificationPermission = onRequestNotificationPermission
        )
    }
}

@Composable
private fun TransitRouteResults(
    planState: TransitPlanState,
    navController: NavController,
    viewModel: DirectionsViewModel,
    appPreferences: AppPreferenceRepository
) {
    when {
        planState.isLoading -> {
            Text(
                text = stringResource(string.calculating_route_in_progress),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(dimen.padding))
            )
        }

        planState.error != null -> {
            Text(
                text = stringResource(string.directions_error, planState.error),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(dimen.padding))
            )
        }

        planState.planResponse != null -> {
            TransitDirectionsScreen(
                viewModel = viewModel, onItineraryClick = { itinerary ->
                    NavigationUtils.navigate(
                        navController, Screen.TransitItineraryDetail(itinerary)
                    )
                }, appPreferences = appPreferences
            )
        }

        else -> {
            // No plan calculated yet
            Text(
                text = stringResource(string.enter_start_and_end_locations_to_get_directions),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(dimen.padding))
            )
        }
    }
}

@Composable
private fun NonTransitRouteResults(
    routeState: RouteState,
    viewModel: DirectionsViewModel,
    navController: NavController,
    appPreferences: AppPreferenceRepository,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit
) {
    when {
        routeState.isLoading -> {
            Text(
                text = stringResource(string.calculating_route_in_progress),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(dimen.padding))
            )
        }

        routeState.error != null -> {
            Text(
                text = stringResource(string.directions_error, routeState.error),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(dimen.padding))
            )
        }

        routeState.route != null -> {
            FerrostarRouteResults(
                ferrostarRoute = routeState.route,
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth(),
                navController = navController,
                distanceUnit = appPreferences.distanceUnit.collectAsState().value,
                availableProfiles = viewModel.getAvailableProfilesForCurrentMode()
                    .collectAsState(initial = emptyList()).value,
                hasNotificationPermission = hasNotificationPermission,
                onRequestNotificationPermission = onRequestNotificationPermission
            )
        }

        else -> {
            // No route calculated yet
            Text(
                text = stringResource(string.enter_start_and_end_locations_to_get_directions),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(dimen.padding))
            )
        }
    }
}

@Composable
private fun DirectionsScreenFocusedField(
    viewModel: DirectionsViewModel,
    fieldFocusState: FieldFocusState,
    onFieldFocusStateChange: (FieldFocusState) -> Unit,
    savedPlaces: List<Place>,
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    coroutineScope: CoroutineScope
) {
    // Show only the focused field and search results when a field is focused
    PlaceField(
        label = if (fieldFocusState == FieldFocusState.FROM) "From" else "To",
        place = if (fieldFocusState == FieldFocusState.FROM) viewModel.fromPlace else viewModel.toPlace,
        onCleared = {
            if (fieldFocusState == FieldFocusState.FROM) {
                viewModel.updateFromPlace(null)
            } else {
                viewModel.updateToPlace(null)
            }
        },
        onTextChange = { viewModel.updateSearchQuery(it) },
        onTextFieldFocusChange = { isFocused ->
            if (isFocused) {
                onFieldFocusStateChange(fieldFocusState)
            } else {
                onFieldFocusStateChange(FieldFocusState.NONE)
            }
        },
        isFocused = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )

    // Show search results or quick suggestions based on search query
    FocusedFieldContent(
        viewModel = viewModel,
        fieldFocusState = fieldFocusState,
        savedPlaces = savedPlaces,
        hasLocationPermission = hasLocationPermission,
        onRequestLocationPermission = onRequestLocationPermission,
        coroutineScope = coroutineScope,
        onFieldFocusStateChange = onFieldFocusStateChange,
    )
}

@Composable
private fun FocusedFieldContent(
    viewModel: DirectionsViewModel,
    fieldFocusState: FieldFocusState,
    onFieldFocusStateChange: (FieldFocusState) -> Unit,
    savedPlaces: List<Place>,
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    coroutineScope: CoroutineScope
) {
    when {
        viewModel.isSearching -> {
            SearchingIndicator()
        }

        viewModel.searchQuery.isEmpty() -> {
            QuickSuggestionsContent(
                viewModel = viewModel,
                fieldFocusState = fieldFocusState,
                savedPlaces = savedPlaces,
                hasLocationPermission = hasLocationPermission,
                onRequestLocationPermission = onRequestLocationPermission,
                coroutineScope = coroutineScope,
                onFieldFocusStateChange = onFieldFocusStateChange,
            )
        }

        else -> {
            SearchResultsContent(
                viewModel = viewModel,
                fieldFocusState = fieldFocusState,
                onFieldFocusStateChange = onFieldFocusStateChange,
            )
        }
    }
}

@Composable
private fun SearchingIndicator() {
    Text(
        text = stringResource(string.searching),
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(dimen.padding))
    )
}

@Composable
private fun QuickSuggestionsContent(
    viewModel: DirectionsViewModel,
    fieldFocusState: FieldFocusState,
    onFieldFocusStateChange: (FieldFocusState) -> Unit,
    savedPlaces: List<Place>,
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    coroutineScope: CoroutineScope
) {
    QuickSuggestions(
        onMyLocationSelected = handleMyLocationSelected(
            viewModel = viewModel,
            fieldFocusState = fieldFocusState,
            hasLocationPermission = hasLocationPermission,
            onRequestLocationPermission = onRequestLocationPermission,
            coroutineScope = coroutineScope,
            onFieldFocusStateChange = onFieldFocusStateChange,
        ),
        savedPlaces = savedPlaces,
        onSavedPlaceSelected = { place ->
            updatePlaceForField(viewModel, fieldFocusState, place)
            onFieldFocusStateChange(FieldFocusState.NONE)
        },
        isGettingLocation = viewModel.isGettingLocation,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SearchResultsContent(
    viewModel: DirectionsViewModel,
    onFieldFocusStateChange: (FieldFocusState) -> Unit,
    fieldFocusState: FieldFocusState
) {
    SearchResults(
        viewModel = hiltViewModel(),
        geocodeResults = deduplicateSearchResults(viewModel.geocodeResults.value),
        onPlaceSelected = { place ->
            updatePlaceForField(viewModel, fieldFocusState, place)
            onFieldFocusStateChange(FieldFocusState.NONE)
        },
        modifier = Modifier.fillMaxWidth()
    )
}

private fun updatePlaceForField(
    viewModel: DirectionsViewModel,
    fieldFocusState: FieldFocusState,
    place: Place
) {
    if (fieldFocusState == FieldFocusState.FROM) {
        viewModel.updateFromPlace(place)
    } else {
        viewModel.updateToPlace(place)
    }
}

private fun handleMyLocationSelected(
    viewModel: DirectionsViewModel,
    fieldFocusState: FieldFocusState,
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    onFieldFocusStateChange: (FieldFocusState) -> Unit,
    coroutineScope: CoroutineScope
): () -> Unit = {
    if (hasLocationPermission) {
        coroutineScope.launch {
            val myLocationPlace = viewModel.getCurrentLocationAsPlace()
            myLocationPlace?.let { place ->
                updatePlaceForField(viewModel, fieldFocusState, place)
                onFieldFocusStateChange(FieldFocusState.NONE)
            }
        }
    } else {
        onRequestLocationPermission()
    }
}

/**
 * Handles route display and camera animation for the directions screen.
 * This composable manages the route state and automatically animates the camera
 * to show the full route when it's calculated.
 */
@Composable
fun RouteDisplayHandler(
    viewModel: DirectionsViewModel,
    cameraState: org.maplibre.compose.camera.CameraState,
    appPreferences: AppPreferenceRepository,
    padding: PaddingValues,
    onRouteUpdate: (Route?) -> Unit
) {
    val routeState = viewModel.routeState
    val selectedMode = viewModel.selectedRoutingMode
    val coroutineScope = rememberCoroutineScope()

    // Update the route state and animate camera when route changes
    LaunchedEffect(routeState.route, selectedMode) {
        if (selectedMode != RoutingMode.PUBLIC_TRANSPORT) {
            onRouteUpdate(routeState.route)
            // Animate camera to show the full route when it's calculated
            routeState.route?.let { route ->
                val coordinates = route.geometry
                if (coordinates.isNotEmpty()) {
                    val lats = coordinates.map { it.lat }
                    val lngs = coordinates.map { it.lng }
                    val minLat = lats.minOrNull() ?: 0.0
                    val maxLat = lats.maxOrNull() ?: 0.0
                    val minLng = lngs.minOrNull() ?: 0.0
                    val maxLng = lngs.maxOrNull() ?: 0.0

                    coroutineScope.launch {
                        cameraState.animateTo(
                            boundingBox = BoundingBox(
                                west = minLng, south = minLat, east = maxLng, north = maxLat
                            ),
                            padding = padding,
                            duration = appPreferences.animationSpeedDurationValue
                        )
                    }
                }
            }
        } else {
            onRouteUpdate(null)
        }
    }
}

@Composable
private fun PlaceField(
    label: String,
    place: Place?,
    onCleared: () -> Unit,
    modifier: Modifier = Modifier,
    onTextChange: (String) -> Unit = {},
    onTextFieldFocusChange: (Boolean) -> Unit = {},
    isFocused: Boolean = false,
    showRecalculateButton: Boolean = false,
    showFlipButton: Boolean = false,
    onRecalculateClick: () -> Unit = {},
    onFlipClick: () -> Unit = {},
    isRouteLoading: Boolean = false
) {
    var textFieldValue by remember(place) { mutableStateOf(place?.name ?: "") }
    val focusRequester = remember { FocusRequester() }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textFieldValue,
                singleLine = true,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    onTextChange(newValue)
                },
                modifier = Modifier
                    .weight(1.0f)
                    .onFocusChanged { focusState ->
                        onTextFieldFocusChange(focusState.isFocused)
                    }
                    .focusRequester(focusRequester),
                label = { Text(label) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(drawable.ic_location_on),
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (place != null) {
                        IconButton(onClick = {
                            textFieldValue = ""
                            onTextChange("")
                            onCleared()
                        }) {
                            Icon(
                                painter = painterResource(drawable.ic_close),
                                contentDescription = stringResource(string.content_description_clear_search)
                            )
                        }
                    }
                },
                placeholder = {
                    Text(stringResource(string.enter_a_place))
                },
                readOnly = false, // Make sure the field is editable to show keyboard
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            )
            if (showRecalculateButton) {
                IconButton(
                    onClick = onRecalculateClick,
                    enabled = !isRouteLoading,
                ) {
                    Icon(
                        painter = painterResource(drawable.ic_refresh),
                        contentDescription = stringResource(string.recalculate_route)
                    )
                }
            }
            if (showFlipButton) {
                IconButton(
                    onClick = onFlipClick, enabled = !isRouteLoading
                ) {
                    Icon(
                        painter = painterResource(drawable.swap_vertical),
                        contentDescription = stringResource(string.flip_destinations)
                    )
                }
            }
        }
    }

    if (isFocused) {
        BackHandler {
            onTextFieldFocusChange(false)
        }
    }

    // Request focus when the field should be focused
    LaunchedEffect(isFocused) {
        if (isFocused) {
            focusRequester.requestFocus()
        }
    }

    // Update textFieldValue when place changes
    LaunchedEffect(place) {
        textFieldValue = place?.name ?: ""
    }
}

@Composable
private fun RoutingModeSelector(
    availableModes: List<RoutingMode>,
    selectedMode: RoutingMode,
    onModeSelected: (RoutingMode) -> Unit,
    modifier: Modifier = Modifier
) {
    SegmentedButtonRow(
        availableModes = availableModes,
        selectedMode = selectedMode,
        onModeSelected = onModeSelected,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SegmentedButtonRow(
    availableModes: List<RoutingMode>,
    selectedMode: RoutingMode,
    onModeSelected: (RoutingMode) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        availableModes.forEachIndexed { index, mode ->
            ToggleButton(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = dimensionResource(dimen.padding) / 2),
                checked = mode == selectedMode,
                onCheckedChange = { if (it) onModeSelected(mode) },
            ) {
                Icon(painter = painterResource(mode.icon), contentDescription = mode.label)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RoutingProfileSelector(
    viewModel: DirectionsViewModel,
    modifier: Modifier = Modifier,
    availableProfiles: List<RoutingProfile>
) {
    val selectedProfile = viewModel.selectedRoutingProfile

    AnimatedVisibility(
        availableProfiles.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Add "Default" option at the beginning
        val profileOptions = listOf(null) + availableProfiles

        SingleChoiceSegmentedButtonRow(
            modifier = modifier.fillMaxWidth()
        ) {
            profileOptions.forEach { profile ->
                val isSelected = selectedProfile?.id == profile?.id
                val label = profile?.name ?: stringResource(string.default_profile)

                ToggleButton(
                    modifier = Modifier
                        .padding(horizontal = dimensionResource(dimen.padding_minor) / 2)
                        .weight(1f),
                    checked = isSelected,
                    onCheckedChange = { if (it) viewModel.selectRoutingProfile(profile) },
                ) {
                    Text(text = label)
                }
            }
        }
    }
}

@Composable
private fun FerrostarRouteResults(
    ferrostarRoute: Route,
    viewModel: DirectionsViewModel,
    modifier: Modifier = Modifier,
    navController: NavController,
    distanceUnit: Int,
    availableProfiles: List<RoutingProfile>,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit
) {
    var showProfileDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var pendingNavigation by remember { mutableStateOf(false) }
    val selectedProfile = viewModel.selectedRoutingProfile

    LaunchedEffect(hasNotificationPermission, pendingNavigation) {
        if (pendingNavigation && hasNotificationPermission) {
            pendingNavigation = false
            viewModel.startNavigation(navController)
        }
    }

    LazyColumn(modifier = modifier) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimensionResource(dimen.padding)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(dimen.padding))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Distance:", style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = GeoUtils.formatDistance(
                                ferrostarRoute.distance, distanceUnit
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Calculate total duration from steps
                    val totalDuration = ferrostarRoute.steps.sumOf { it.duration }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = dimensionResource(dimen.padding)),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Duration:", style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "${(totalDuration / 60).toInt()} min",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = {
                            if (hasNotificationPermission) {
                                viewModel.startNavigation(navController)
                            } else {
                                showNotificationDialog = true
                            }
                        }, modifier = Modifier.fillMaxWidth(), enabled = true
                    ) {
                        Text(stringResource(string.start_navigation))
                    }
                }
            }
        }

        // Actual route steps
        items(ferrostarRoute.steps.size) { index ->
            val step = ferrostarRoute.steps[index]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(dimen.padding)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Step number
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}", style = MaterialTheme.typography.labelLarge
                        )
                    }

                    // Step instruction
                    Text(
                        text = step.instruction,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = dimensionResource(dimen.padding))
                    )

                    // Step distance
                    Text(
                        text = GeoUtils.formatDistance(step.distance, distanceUnit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Dialogs
    ProfileSelectionDialog(
        showDialog = showProfileDialog,
        onDismiss = { showProfileDialog = false },
        selectedProfile = selectedProfile,
        availableProfiles = availableProfiles,
        onProfileSelected = { profile ->
            viewModel.selectRoutingProfile(profile)
            showProfileDialog = false
        }
    )

    NotificationRequestDialog(
        showDialog = showNotificationDialog,
        onDismiss = { showNotificationDialog = false },
        onConfirm = {
            showNotificationDialog = false
            pendingNavigation = true
            onRequestNotificationPermission()
        }
    )
}

@Composable
private fun ProfileSelectionDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    selectedProfile: RoutingProfile?,
    availableProfiles: List<RoutingProfile>,
    onProfileSelected: (RoutingProfile?) -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(string.select_routing_profile)) },
            text = {
                Column {
                    // Default option
                    TextButton(
                        onClick = { onProfileSelected(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(string.default_profile),
                            color = if (selectedProfile == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Custom profiles
                    availableProfiles.forEach { profile ->
                        TextButton(
                            onClick = { onProfileSelected(profile) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = profile.name,
                                color = if (selectedProfile?.id == profile.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(string.cancel_change_routing_profile))
                }
            })
    }
}

@Composable
private fun NotificationRequestDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(string.notification_ask_title)) },
            text = { Text(stringResource(string.notification_ask_body)) },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(stringResource(string.got_it))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(string.cancel))
                }
            })
    }
}
