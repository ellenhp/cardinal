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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.string
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.transit.StopTime
import earth.maps.cardinal.viewmodel.TransitStopCardViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant

fun Color.contrastColor(): Color {
    // Calculate luminance using the standard formula
    val luminance = (this.red * 0.299f + this.green * 0.587f + this.blue * 0.114f)
    // Return black for light backgrounds and white for dark backgrounds
    return if (luminance > 0.5f) Color.Black else Color.White
}

@Composable
fun TransitStopScreen(
    stop: Place,
    onBack: () -> Unit,
    onGetDirections: (Place) -> Unit,
    viewModel: TransitStopCardViewModel,
    onPeekHeightChange: (dp: Dp) -> Unit
) {
    val density = LocalDensity.current

    // Initialize the stop when screen is opened
    LaunchedEffect(stop) {
        viewModel.setStop(stop)
    }

    BackHandler {
        onBack()
    }

    // Use the loaded place from viewModel if available
    val displayedPlace = viewModel.stop.value ?: stop

    // State for save place dialog
    var showSavePlaceDialog by remember { mutableStateOf(false) }

    // State for unsave confirmation dialog
    var showUnsaveConfirmationDialog by remember { mutableStateOf(false) }

    // Temporary place to save (used in dialog)
    var stopToSave by remember { mutableStateOf(stop) }

    val isRefreshingDepartures = viewModel.isRefreshingDepartures.collectAsState()
    val isInitiallyLoading = viewModel.isLoading.collectAsState()
    val didLoadingFail = viewModel.didLoadingFail.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(dimen.padding),
                    end = dimensionResource(dimen.padding),
                )
                .onGloballyPositioned { coordinates ->
                    val heightInDp = with(density) { coordinates.size.height.toDp() }
                    onPeekHeightChange(heightInDp)
                },

            ) {
            // Place name and type
            Text(
                text = displayedPlace.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = displayedPlace.type,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Address information
            displayedPlace.address?.let { address ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(dimensionResource(dimen.icon_size))
                    )
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = dimensionResource(dimen.padding)),
                        text = displayedPlace.address.format()
                            ?: stringResource(string.address_unavailable)
                    )
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                // Get directions button
                Button(
                    onClick = { onGetDirections(displayedPlace) }, modifier = Modifier.padding(
                        start = dimensionResource(dimen.padding_minor), end = 0.dp
                    )
                ) {
                    Text(stringResource(string.get_directions))
                }

                // Save/Unsave button
                Button(
                    onClick = {
                        if (viewModel.isPlaceSaved.value) {
                            // Show confirmation dialog for unsaving
                            showUnsaveConfirmationDialog = true
                        } else {
                            // Show dialog for saving
                            stopToSave = displayedPlace
                            showSavePlaceDialog = true
                        }
                    }, modifier = Modifier.padding(start = dimensionResource(dimen.padding_minor))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (viewModel.isPlaceSaved.value) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (viewModel.isPlaceSaved.value) {
                                stringResource(string.unsave_place)
                            } else {
                                stringResource(string.save_place)
                            }
                        )
                    }
                }
            }

            // Inset horizontal divider
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dimensionResource(dimen.padding) / 2),
                thickness = DividerDefaults.Thickness,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(
                    start = dimensionResource(dimen.padding),
                    end = dimensionResource(dimen.padding),
                )
        ) {
            // Departures section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(string.next_departures),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                // Refresh button for departures
                IconButton(onClick = { coroutineScope.launch { viewModel.refreshDepartures() } }) {
                    if (isRefreshingDepartures.value) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp), strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(string.refresh_departures)
                        )
                    }
                }
            }

            if (isInitiallyLoading.value) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    text = stringResource(string.loading_departures)
                )
                // Show indeterminate progress bar while loading
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            } else if (didLoadingFail.value) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    text = stringResource(string.failed_to_load_departures)
                )
            } else if (viewModel.departures.value.isEmpty()) {
                Text(
                    text = stringResource(string.no_upcoming_departures),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                val maxDeparturesPerHeadsign = 5
                // List of departures grouped by route and headsign
                RouteDepartures(
                    stopTimes = viewModel.departures.value,
                    maxDepartures = maxDeparturesPerHeadsign
                )
            }
        }
    }

    // Save Place Dialog
    if (showSavePlaceDialog) {
        SaveTransitStopDialog(
            place = stopToSave,
            onDismiss = { showSavePlaceDialog = false },
            onSave = { updatedPlace ->
                viewModel.savePlace(updatedPlace)
                showSavePlaceDialog = false
            })
    }

    // Unsave Confirmation Dialog
    if (showUnsaveConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showUnsaveConfirmationDialog = false },
            title = { Text(stringResource(string.unsave_place)) },
            text = {
                Text(
                    stringResource(
                        string.are_you_sure_you_want_to_delete, displayedPlace.name
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.unsavePlace(displayedPlace)
                        showUnsaveConfirmationDialog = false
                    }) {
                    Text(stringResource(string.unsave_place))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUnsaveConfirmationDialog = false }) {
                    Text(stringResource(string.cancel_button))
                }
            })
    }
}

@OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RouteDepartures(stopTimes: List<StopTime>, maxDepartures: Int) {
    // Group departures by route name
    val departuresByRoute = stopTimes.groupBy { it.routeShortName }

    departuresByRoute.forEach { (routeName, departures) ->
        val routeColor = departures.firstOrNull()?.routeColor?.let {
            Color(
                "#$it".toColorInt()
            )
        } ?: MaterialTheme.colorScheme.surfaceVariant

        val onRouteColor = routeColor.contrastColor()

        // Group departures by headsign within each route
        val departuresByHeadsign = departures.groupBy { it.headsign }.map { (key, value) ->
            (key to value.take(maxDepartures))
        }.toMap()
        val headsigns = departuresByHeadsign.keys.toList().sorted()

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Route name header
                Text(
                    text = routeName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Carousel for headsigns if there are multiple headsigns
                if (headsigns.size > 1) {
                    val pagerState = rememberPagerState(pageCount = { headsigns.size })
                    val scrollScope = rememberCoroutineScope()

                    // Display headsign tabs for navigation
                    TabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        divider = {}) {
                        headsigns.forEachIndexed { index, headsign ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    scrollScope.launch {
                                        pagerState.scrollToPage(index)
                                    }
                                },
                                text = {
                                    Text(
                                        text = headsign,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1
                                    )
                                },
                                selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                unselectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                    alpha = 0.5f
                                )
                            )
                        }
                    }

                    // Swipeable pager for headsigns with fixed height
                    FixedHeightHorizontalPager(
                        state = pagerState,
                        departuresByHeadsign = departuresByHeadsign,
                        headsigns = headsigns,
                        routeColor = routeColor,
                        onRouteColor = onRouteColor,
                    )
                } else {
                    // If there's only one headsign, display departures normally
                    departures.take(maxDepartures).forEachIndexed { index, stopTime ->
                        DepartureRow(
                            stopTime = stopTime,
                            isFirst = index == 0,
                            routeColor = routeColor,
                            onRouteColor = onRouteColor
                        )
                        // Add divider between departure rows, but not after the last one
                        if (index < departures.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                thickness = DividerDefaults.Thickness / 2,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun DepartureRow(
    stopTime: StopTime, isFirst: Boolean, routeColor: Color, onRouteColor: Color,
) {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val isoFormatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault())
    val textColor = MaterialTheme.colorScheme.onSurface

    val scheduledDepartureInstant = stopTime.place.scheduledDeparture?.let {
        try {
            Instant.from(isoFormatter.parse(it))
        } catch (_: Exception) {
            null
        }
    }
    val scheduledDepartureTime: String? = scheduledDepartureInstant?.let {
        try {
            dateFormat.format(Date.from(it))
        } catch (_: Exception) {
            null
        }
    }

    val bestDepartureInstant = stopTime.place.departure?.let {
        try {
            Instant.from(isoFormatter.parse(it))
        } catch (_: Exception) {
            null
        }
    } ?: scheduledDepartureInstant

    val bestDepartureTime: String? = bestDepartureInstant?.let {
        try {
            dateFormat.format(Date.from(it))
        } catch (_: Exception) {
            scheduledDepartureTime
        }
    } ?: scheduledDepartureTime

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Headsign at the beginning.
        if (isFirst) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(0.6f),
                colors = CardDefaults.cardColors(containerColor = routeColor)
            ) {
                Text(
                    modifier = Modifier.padding(dimensionResource(dimen.padding)),
                    text = stopTime.headsign,
                    textAlign = TextAlign.Center,
                    color = onRouteColor,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        val stopTimeStyle = if (isFirst) {
            MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        } else {
            MaterialTheme.typography.bodyMedium
        }
        Column(
            modifier = Modifier
                .padding(horizontal = dimensionResource(dimen.padding))
                .fillMaxWidth()
        ) {
            // Departure time at the bottom
            val timeUntilDeparture =
                bestDepartureInstant?.toKotlinInstant()?.minus(Clock.System.now())
            Text(
                modifier = Modifier.align(Alignment.End),
                text = if (timeUntilDeparture != null && timeUntilDeparture > 30.minutes) {
                    bestDepartureTime ?: stringResource(string.unknown_departure)
                } else if (timeUntilDeparture != null) {
                    "${timeUntilDeparture.inWholeMinutes} min"
                } else {
                    bestDepartureTime ?: stringResource(string.unknown_departure)
                },
                textAlign = TextAlign.End,
                style = stopTimeStyle,
                color = textColor,
                fontWeight = if (stopTime.realTime) FontWeight.Medium else FontWeight.Normal
            )
            // Real-time or scheduled indicator
            val isLiveIndicatorString = if (stopTime.realTime) {
                stringResource(string.live_indicator)
            } else {
                stringResource(string.scheduled_indicator)
            }

            Text(
                text = isLiveIndicatorString,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FixedHeightHorizontalPager(
    state: PagerState,
    departuresByHeadsign: Map<String, List<StopTime>>,
    headsigns: List<String>,
    routeColor: Color,
    onRouteColor: Color,
) {
    val topPadding = dimensionResource(
        dimen.padding_minor
    )
    val maxChildHeight = remember(key1 = departuresByHeadsign) {
        mutableStateOf(0.dp)
    }
    val density = LocalDensity.current

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        HorizontalPager(
            state = state,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val selectedHeadsign = headsigns[page]
            val departuresForSelectedHeadsign =
                departuresByHeadsign[selectedHeadsign] ?: emptyList()

            Column(
                modifier = Modifier
                    .padding(top = topPadding)
                    .defaultMinSize(minHeight = maxChildHeight.value)
                    .onGloballyPositioned({ coordinates ->
                        val heightDp = with(density) { coordinates.size.height.toDp() }
                        if (heightDp > maxChildHeight.value) {
                            maxChildHeight.value = heightDp
                        }
                    })
            ) {
                departuresForSelectedHeadsign.forEachIndexed { index, stopTime ->
                    DepartureRow(
                        stopTime = stopTime,
                        isFirst = index == 0,
                        routeColor = routeColor,
                        onRouteColor = onRouteColor
                    )
                    // Add divider between departure rows, but not after the last one
                    if (index < departuresForSelectedHeadsign.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            thickness = DividerDefaults.Thickness / 2,
                            color = onRouteColor.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun SaveTransitStopDialog(
    place: Place, onDismiss: () -> Unit, onSave: (Place) -> Unit
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(string.save_place)) },
        text = {
            OutlinedButton(
                onClick = { onSave(place) }, modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(string.save_button))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(string.cancel_button))
            }
        })
}
