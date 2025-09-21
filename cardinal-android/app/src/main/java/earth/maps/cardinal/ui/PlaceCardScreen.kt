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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import earth.maps.cardinal.viewmodel.PlaceCardViewModel

@Composable
fun PlaceCardScreen(
    viewModel: PlaceCardViewModel,
    place: Place,
    onBack: () -> Unit,
    onGetDirections: (Place) -> Unit,
    onPeekHeightChange: (dp: Dp) -> Unit
) {
    val density = LocalDensity.current

    // Check if place is saved when screen is opened
    LaunchedEffect(place) {
        viewModel.checkIfPlaceIsSaved(place)
    }

    BackHandler {
        onBack()
    }

    // Use the loaded place from viewModel if available
    val displayedPlace = viewModel.place.value ?: place

    // State for save place dialog
    var showSavePlaceDialog by remember { mutableStateOf(false) }

    // State for unsave confirmation dialog
    var showUnsaveConfirmationDialog by remember { mutableStateOf(false) }

    // Temporary place to save (used in dialog)
    var placeToSave by remember { mutableStateOf(place) }

    // Place details content
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
        // Place name and type
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
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Get directions button
            Button(
                onClick = { onGetDirections(displayedPlace) },
                modifier = Modifier
                    .padding(start = dimensionResource(dimen.padding_minor), end = 0.dp)
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
                        placeToSave = displayedPlace
                        showSavePlaceDialog = true
                    }
                },
                modifier = Modifier
                    .padding(start = dimensionResource(dimen.padding_minor))
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

    // Save Place Dialog
    if (showSavePlaceDialog) {
        SavePlaceDialog(
            place = placeToSave,
            onDismiss = { showSavePlaceDialog = false },
            onSaveAsHome = { updatedPlace ->
                viewModel.savePlaceAsHome(updatedPlace)
                showSavePlaceDialog = false
            },
            onSaveAsWork = { updatedPlace ->
                viewModel.savePlaceAsWork(updatedPlace)
                showSavePlaceDialog = false
            },
            onSaveAsOther = { updatedPlace ->
                viewModel.savePlace(updatedPlace)
                showSavePlaceDialog = false
            }
        )
    }

    // Unsave Confirmation Dialog
    if (showUnsaveConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showUnsaveConfirmationDialog = false },
            title = { Text(stringResource(string.unsave_place)) },
            text = {
                Text(
                    stringResource(
                        string.are_you_sure_you_want_to_delete,
                        displayedPlace.name
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.unsavePlace(displayedPlace)
                        showUnsaveConfirmationDialog = false
                    }
                ) {
                    Text(stringResource(string.unsave_place))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUnsaveConfirmationDialog = false }
                ) {
                    Text(stringResource(string.cancel_button))
                }
            }
        )
    }
}

@Composable
fun SavePlaceDialog(
    place: Place,
    onDismiss: () -> Unit,
    onSaveAsHome: (Place) -> Unit,
    onSaveAsWork: (Place) -> Unit,
    onSaveAsOther: (Place) -> Unit
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(string.save_place)) },
        text = {
            Column {
                OutlinedButton(
                    onClick = { onSaveAsHome(place.copy(icon = "home")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(string.set_as_home))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { onSaveAsWork(place.copy(icon = "work")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(string.set_as_work))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { onSaveAsOther(place) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(string.save_button))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(string.cancel_button))
            }
        }
    )
}
