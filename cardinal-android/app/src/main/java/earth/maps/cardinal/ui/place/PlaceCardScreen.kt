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

package earth.maps.cardinal.ui.place

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.drawable
import earth.maps.cardinal.R.string
import earth.maps.cardinal.data.AddressFormatter
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.format

@Composable
fun PlaceCardScreen(
    place: Place,
    onBack: () -> Unit,
    onGetDirections: (Place) -> Unit,
    viewModel: PlaceCardViewModel,
    onPeekHeightChange: (dp: Dp) -> Unit
) {
    val density = LocalDensity.current
    val addressFormatter = remember { AddressFormatter() }

    // Check if place is saved when screen is opened
    LaunchedEffect(place) {
        viewModel.checkIfPlaceIsSaved(place)
    }

    BackHandler {
        onBack()
    }

    // Use the loaded place from viewModel if available
    val displayedPlace = viewModel.place.value ?: place

    // State for unsave confirmation dialog
    var showUnsaveConfirmationDialog by remember { mutableStateOf(false) }

    // Place details content
    Column {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(dimen.padding),
                    end = dimensionResource(dimen.padding),
                )
                .verticalScroll(rememberScrollState())
                .onGloballyPositioned { coordinates ->
                    val heightInDp = with(density) { coordinates.size.height.toDp() }
                    onPeekHeightChange(heightInDp)
                },
        ) {
            PlaceHeader(displayedPlace)
            PlaceAddress(displayedPlace, addressFormatter)
            PlaceActions(displayedPlace, viewModel, place, onGetDirections) { showUnsaveConfirmationDialog = true }
            // Inset horizontal divider
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dimensionResource(dimen.padding) / 2),
                thickness = DividerDefaults.Thickness,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        if (place.isTransitStop) {
            TransitStopInformation(viewModel = hiltViewModel<TransitStopCardViewModel>().also {
                it.setStop(place)
            }, onRouteClicked = {})
        }

        UnsaveConfirmationDialog(displayedPlace, viewModel, showUnsaveConfirmationDialog) { showUnsaveConfirmationDialog = false }
    }
}

@Composable
private fun PlaceHeader(displayedPlace: Place) {
    Text(
        text = displayedPlace.name,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )

    Text(
        text = displayedPlace.description,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun PlaceAddress(displayedPlace: Place, addressFormatter: AddressFormatter) {
    displayedPlace.address?.let { address ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(drawable.ic_location_on),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(dimen.icon_size))
            )
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = dimensionResource(dimen.padding)),
                text = displayedPlace.address.format(addressFormatter)
                    ?: stringResource(string.address_unavailable)
            )
        }
    }
}

@Composable
private fun PlaceActions(
    displayedPlace: Place,
    viewModel: PlaceCardViewModel,
    place: Place,
    onGetDirections: (Place) -> Unit,
    onShowUnsaveDialog: () -> Unit
) {
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
                    onShowUnsaveDialog()
                } else {
                    viewModel.savePlace(place)
                }
            }, modifier = Modifier.padding(start = dimensionResource(dimen.padding_minor))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = if (viewModel.isPlaceSaved.value)
                        painterResource(drawable.ic_heart_minus)
                    else
                        painterResource(drawable.ic_heart),
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
}

@Composable
private fun UnsaveConfirmationDialog(
    displayedPlace: Place,
    viewModel: PlaceCardViewModel,
    show: Boolean,
    onDismiss: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
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
                        onDismiss()
                    }) {
                    Text(stringResource(string.unsave_place))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(string.cancel_button))
                }
            }
        )
    }
}
