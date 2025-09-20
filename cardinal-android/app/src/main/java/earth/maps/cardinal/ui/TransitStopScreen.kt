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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.string
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.transit.StopTime
import earth.maps.cardinal.viewmodel.TransitStopCardViewModel
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

    Column {

        Column(
            modifier = Modifier.onGloballyPositioned { coordinates ->
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

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(dimen.padding),
                    end = dimensionResource(dimen.padding),
                )
        ) {

            item {
                // Departures section
                Text(
                    text = "Departures",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            item {
                // Refresh button for departures
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(string.departures_heading),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.refreshDepartures() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh departures"
                        )
                    }
                }
            }

            item {
                if (viewModel.departures.value.isEmpty()) {
                    Text(
                        text = "No upcoming departures",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            // List of departures
            items(viewModel.departures.value) { stopTime ->
                DepartureItem(stopTime = stopTime)
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

        // Departures section
        Text(
            text = "Departures",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )

        // Refresh button for departures
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Next departures:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.refreshDepartures() }) {
                Icon(
                    imageVector = Icons.Default.Refresh, contentDescription = "Refresh departures"
                )
            }
        }

        // List of departures
        if (viewModel.departures.value.isEmpty()) {
            Text(
                text = "No upcoming departures",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                items(viewModel.departures.value) { stopTime ->
                    DepartureItem(stopTime = stopTime)
                }
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

@OptIn(ExperimentalTime::class)
@Composable
fun DepartureItem(stopTime: StopTime) {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val isoFormatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault())


    val scheduledDepartureInstant = stopTime.place.scheduledDeparture?.let {
        try {
            Instant.from(isoFormatter.parse(it))
        } catch (_: Exception) {
            null
        }
    }
    val scheduledDepartureTime = scheduledDepartureInstant?.let {
        try {
            dateFormat.format(Date.from(it))
        } catch (_: Exception) {
            it
        }
    } ?: stringResource(string.unknown_departure)

    val bestDepartureInstant = stopTime.place.departure?.let {
        try {
            Instant.from(isoFormatter.parse(it))
        } catch (_: Exception) {
            null
        }
    } ?: scheduledDepartureInstant

    val bestDepartureTime = bestDepartureInstant?.let {
        try {
            dateFormat.format(Date.from(it))
        } catch (_: Exception) {
            it
        }
    } ?: scheduledDepartureTime

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stopTime.routeShortName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = if (stopTime.realTime) "● Live" else "Scheduled",
                style = MaterialTheme.typography.bodySmall,
                color = if (stopTime.realTime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = stopTime.headsign,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 2.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val timeUntilDeparture =
                bestDepartureInstant?.toKotlinInstant()?.minus(Clock.System.now())
            if (timeUntilDeparture != null && timeUntilDeparture > 30.minutes) {
                Text(
                    text = "Departure: $bestDepartureTime",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
            } else if (timeUntilDeparture != null) {
                Text(
                    text = "${timeUntilDeparture.inWholeMinutes} minutes",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
            } else if (stopTime.cancelled) {
                Text(
                    text = stringResource(string.transit_cancelled),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = stringResource(string.transit_unknown_status),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        thickness = DividerDefaults.Thickness / 2,
        color = MaterialTheme.colorScheme.outlineVariant
    )
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
