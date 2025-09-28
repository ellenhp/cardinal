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

package earth.maps.cardinal.ui

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
import earth.maps.cardinal.viewmodel.DirectionsViewModel
import io.github.dellisd.spatialk.geojson.BoundingBox
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
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission && pendingLocationRequest != null) {
            val targetField = pendingLocationRequest!!
            pendingLocationRequest = null // Clear the pending request

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
                    // Clear focus state after selection
                    fieldFocusState = FieldFocusState.NONE
                }
            }
        }
    }

    stringResource(string.change_start_location_to_my_location)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(dimen.padding))
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current

        // Conditionally show UI based on field focus
        if (!isAnyFieldFocused) {
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
                    onCleared = {
                        viewModel.updateFromPlace(null)

                    },
                    onTextChange = {
                        viewModel.updateSearchQuery(it)
                    },
                    onTextFieldFocusChange = {
                        fieldFocusState = if (it) {
                            onFullExpansionRequired()
                            FieldFocusState.FROM
                        } else {
                            FieldFocusState.NONE
                        }
                    },
                    isFocused = fieldFocusState == FieldFocusState.FROM,
                    showRecalculateButton = viewModel.fromPlace != null && viewModel.toPlace != null,
                    onRecalculateClick = { viewModel.recalculateRoute() },
                    isRouteLoading = routeState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                PlaceField(
                    label = stringResource(string.to),
                    place = viewModel.toPlace,
                    onCleared = {
                        viewModel.updateToPlace(null)
                    },
                    onTextChange = {
                        viewModel.updateSearchQuery(it)
                    },
                    onTextFieldFocusChange = {
                        fieldFocusState = if (it) {
                            onFullExpansionRequired()
                            FieldFocusState.TO
                        } else {
                            FieldFocusState.NONE
                        }
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
                        RoutingMode.AUTO, RoutingMode.PEDESTRIAN, RoutingMode.BICYCLE
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
        } else {
            // Show only the focused field and search results when a field is focused
            val currentFocusState = fieldFocusState
            PlaceField(
                label = if (currentFocusState == FieldFocusState.FROM) "From" else "To",
                place = if (currentFocusState == FieldFocusState.FROM) viewModel.fromPlace else viewModel.toPlace,
                onCleared = {
                    if (currentFocusState == FieldFocusState.FROM) {
                        viewModel.updateFromPlace(null)
                    } else {
                        viewModel.updateToPlace(null)
                    }
                },
                onTextChange = {
                    viewModel.updateSearchQuery(it)
                },
                onTextFieldFocusChange = { isFocused ->
                    fieldFocusState = if (isFocused) {
                        currentFocusState
                    } else {
                        FieldFocusState.NONE
                    }
                },
                isFocused = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // Show search results or quick suggestions based on search query
            if (viewModel.isSearching) {
                Text(
                    text = "Searching...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(dimen.padding))
                )
            } else if (viewModel.searchQuery.isEmpty()) {
                // Show quick suggestions when no search query
                QuickSuggestions(
                    onMyLocationSelected = {
                        // Check permissions before attempting to get location
                        if (hasLocationPermission) {
                            // Launch coroutine to get current location
                            coroutineScope.launch {
                                val myLocationPlace = viewModel.getCurrentLocationAsPlace()
                                myLocationPlace?.let { place ->
                                    // Update the appropriate place based on which field is focused
                                    if (fieldFocusState == FieldFocusState.FROM) {
                                        viewModel.updateFromPlace(place)
                                    } else {
                                        viewModel.updateToPlace(place)
                                    }
                                    // Clear focus state after selection
                                    fieldFocusState = FieldFocusState.NONE
                                }
                            }
                        } else {
                            // Set pending request for auto-retry after permission grant
                            pendingLocationRequest = fieldFocusState
                            // Request location permission
                            onRequestLocationPermission()
                        }
                    },
                    savedPlaces = savedPlaces,
                    onSavedPlaceSelected = { place ->
                        // Update the appropriate place based on which field is focused
                        if (fieldFocusState == FieldFocusState.FROM) {
                            viewModel.updateFromPlace(place)
                        } else {
                            viewModel.updateToPlace(place)
                        }
                        // Clear focus state after selection
                        fieldFocusState = FieldFocusState.NONE
                    },
                    isGettingLocation = viewModel.isGettingLocation,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Show search results when there's a query
                SearchResults(
                    viewModel = hiltViewModel(),
                    geocodeResults = deduplicateSearchResults(viewModel.geocodeResults.value),
                    onPlaceSelected = { place ->
                        // Update the appropriate place based on which field is focused
                        if (fieldFocusState == FieldFocusState.FROM) {
                            viewModel.updateFromPlace(place)
                        } else {
                            viewModel.updateToPlace(place)
                        }
                        // Clear focus state after selection
                        fieldFocusState = FieldFocusState.NONE
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
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
    val coroutineScope = rememberCoroutineScope()

    // Update the route state and animate camera when route changes
    LaunchedEffect(routeState.route) {
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
                        ), padding = padding, duration = appPreferences.animationSpeedDurationValue
                    )
                }
            }
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
            modifier = modifier
                .fillMaxWidth()
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
                        text = GeoUtils.formatShortDistance(step.distance, distanceUnit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Profile selection dialog
    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text(stringResource(string.select_routing_profile)) },
            text = {
                Column {
                    // Default option
                    TextButton(
                        onClick = {
                            viewModel.selectRoutingProfile(null)
                            showProfileDialog = false
                        }, modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(string.default_profile),
                            color = if (selectedProfile == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Custom profiles
                    availableProfiles.forEach { profile ->
                        TextButton(
                            onClick = {
                                viewModel.selectRoutingProfile(profile)
                                showProfileDialog = false
                            }, modifier = Modifier.fillMaxWidth()
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
                TextButton(onClick = { showProfileDialog = false }) {
                    Text(stringResource(string.cancel_change_routing_profile))
                }
            })
    }

    // Notification permission dialog
    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            title = { Text("Notification Permission Needed") },
            text = { Text("We need notification permissions to keep the screen on during navigation.") },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationDialog = false
                    pendingNavigation = true
                    onRequestNotificationPermission()
                }) {
                    Text("Got it")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationDialog = false }) {
                    Text("Cancel")
                }
            })
    }
}
