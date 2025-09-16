package earth.maps.cardinal.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import earth.maps.cardinal.R
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.RoutingMode
import earth.maps.cardinal.data.deduplicateSearchResults
import earth.maps.cardinal.viewmodel.DirectionsViewModel
import kotlinx.coroutines.Job
import uniffi.ferrostar.Route

enum class FieldFocusState {
    NONE,
    FROM,
    TO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectionsScreen(
    viewModel: DirectionsViewModel,
    onPeekHeightChange: (dp: Dp) -> Unit,
    onBack: () -> Unit,
    onFullExpansionRequired: () -> Job,
    navigationCoordinator: NavigationCoordinator
) {
    var fieldFocusState by remember { mutableStateOf(FieldFocusState.NONE) }
    val isAnyFieldFocused = fieldFocusState != FieldFocusState.NONE

    // Get route result from ViewModel
    val ferrostarRoute = viewModel.ferrostarRoute
    val isRouteLoading = viewModel.isRouteLoading
    val routeError = viewModel.routeError

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
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
                    }
            ) {
                // Header with back button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }

                    Text(
                        text = "Directions",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    // Spacer to balance the row
                    Box(modifier = Modifier.size(48.dp))
                }

                // From and To fields
                PlaceField(
                    label = "From",
                    place = viewModel.fromPlace,
                    onCleared = {
                        viewModel.updateFromPlace(null)

                    },
                    onTextChange = {
                        viewModel.updateSearchQuery(it)
                    },
                    onTextFieldFocusChange = {
                        fieldFocusState = if (it) FieldFocusState.FROM else FieldFocusState.NONE
                        onFullExpansionRequired()
                    },
                    isFocused = fieldFocusState == FieldFocusState.FROM,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                PlaceField(
                    label = "To",
                    place = viewModel.toPlace,
                    onCleared = {
                        viewModel.updateToPlace(null)
                    },
                    onTextChange = {
                        viewModel.updateSearchQuery(it)
                    },
                    onTextFieldFocusChange = {
                        fieldFocusState = if (it) FieldFocusState.TO else FieldFocusState.NONE
                        onFullExpansionRequired()
                    },
                    isFocused = fieldFocusState == FieldFocusState.TO,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // Routing mode selection
                Text(
                    text = "Routing Mode",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                RoutingModeSelector(
                    selectedMode = viewModel.selectedRoutingMode,
                    onModeSelected = { viewModel.updateRoutingMode(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
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
                isRouteLoading -> {
                    Text(
                        text = "Calculating route...",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }

                routeError != null -> {
                    Text(
                        text = "Error: $routeError",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }

                ferrostarRoute != null -> {
                    FerrostarRouteResults(
                        ferrostarRoute = ferrostarRoute,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxWidth(),
                        navigationCoordinator = navigationCoordinator
                    )
                }

                else -> {
                    // No route calculated yet
                    Text(
                        text = "Enter start and end locations to get directions",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
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

            // Show search results or loading state
            if (viewModel.isSearching) {
                Text(
                    text = "Searching...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            } else {
                SearchResults(
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

@Composable
private fun PlaceField(
    label: String,
    place: Place?,
    onCleared: () -> Unit,
    modifier: Modifier = Modifier,
    onTextChange: (String) -> Unit = {},
    onTextFieldFocusChange: (Boolean) -> Unit = {},
    isFocused: Boolean = false
) {
    var textFieldValue by remember(place) { mutableStateOf(place?.name ?: "") }
    val focusRequester = remember { FocusRequester() }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            textFieldValue = newValue
            onTextChange(newValue)
        },
        modifier = modifier
            .onFocusChanged { focusState ->
                onTextFieldFocusChange(focusState.isFocused)
            }
            .focusRequester(focusRequester),
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.LocationOn,
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
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.content_description_clear_search)
                    )
                }
            }
        },
        placeholder = {
            Text(stringResource(R.string.enter_a_place))
        },
        readOnly = false, // Make sure the field is editable to show keyboard
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    )

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
    selectedMode: RoutingMode,
    onModeSelected: (RoutingMode) -> Unit,
    modifier: Modifier = Modifier
) {
    SegmentedButtonRow(
        selectedMode = selectedMode,
        onModeSelected = onModeSelected,
        modifier = modifier
    )
}

@Composable
private fun SegmentedButtonRow(
    selectedMode: RoutingMode,
    onModeSelected: (RoutingMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val routingModes = listOf(
        RoutingMode.AUTO,
        RoutingMode.PEDESTRIAN,
        RoutingMode.BICYCLE
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            routingModes.forEachIndexed { index, mode ->
                val isSelected = selectedMode == mode
                val isFirst = index == 0
                val isLast = index == routingModes.size - 1

                SegmentedButton(
                    selected = isSelected,
                    onClick = { onModeSelected(mode) },
                    label = mode.label,
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            start = if (isFirst) 0.dp else 2.dp,
                            end = if (isLast) 0.dp else 2.dp
                        ),
                    shape = when {
                        isFirst -> MaterialTheme.shapes.medium.copy(
                            topEnd = CornerSize(0.dp),
                            bottomEnd = CornerSize(0.dp)
                        )

                        isLast -> MaterialTheme.shapes.medium.copy(
                            topStart = CornerSize(0.dp),
                            bottomStart = CornerSize(0.dp)
                        )

                        else -> MaterialTheme.shapes.medium.copy(
                            topStart = CornerSize(0.dp),
                            topEnd = CornerSize(0.dp),
                            bottomStart = CornerSize(0.dp),
                            bottomEnd = CornerSize(0.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.medium
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        colors = if (selected) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun FerrostarRouteResults(
    ferrostarRoute: Route,
    viewModel: DirectionsViewModel,
    modifier: Modifier = Modifier,
    navigationCoordinator: NavigationCoordinator
) {
    LazyColumn(modifier = modifier) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.route_summary),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Distance:",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "${String.format("%.1f", ferrostarRoute.distance)} km",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Calculate total duration from steps
                    val totalDuration = ferrostarRoute.steps.sumOf { it.duration }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Duration:",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "${(totalDuration / 60).toInt()} min",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.startNavigation(navigationCoordinator)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.start_navigation))
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
                        .padding(16.dp),
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
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    // Step instruction
                    Text(
                        text = step.instruction,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    )

                    // Step distance
                    Text(
                        text = "${String.format("%.0f", step.distance)} m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
