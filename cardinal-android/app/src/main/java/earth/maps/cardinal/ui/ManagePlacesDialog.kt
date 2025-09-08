package earth.maps.cardinal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import earth.maps.cardinal.R
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.viewmodel.ManagePlacesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePlacesDialog(
    onDismiss: () -> Unit,
    onPlaceSelected: (Place) -> Unit,
    viewModel: ManagePlacesViewModel = viewModel()
) {
    val places = viewModel.places.value
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding))
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.saved_places),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = {
                    scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                        onDismiss()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close_button)
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding)))

            // Places list
            if (places.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_saved_places_yet),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn {
                    items(places) { place ->
                        PlaceItemWithActions(
                            place = place,
                            onPlaceSelected = { 
                                viewModel.selectPlace(place)
                                onPlaceSelected(place)
                            },
                            onEditPlace = { viewModel.startEditingPlace(place) },
                            onDeletePlace = { viewModel.startDeletingPlace(place) }
                        )
                        if (place != places.last()) {
                            Divider(
                                modifier = Modifier.padding(vertical = dimensionResource(R.dimen.padding_minor))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding)))
        }
    }

    // Edit Place Dialog
    viewModel.placeToEdit.value?.let { place ->
        EditPlaceDialog(
            place = place,
            onDismiss = { viewModel.cancelEditingPlace() },
            onEdit = { updatedPlace ->
                viewModel.editPlace(updatedPlace)
            }
        )
    }

    // Delete Confirmation Dialog
    viewModel.placeToDelete.value?.let { place ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeletingPlace() },
            title = { Text(stringResource(R.string.delete_place)) },
            text = { Text(stringResource(R.string.are_you_sure_you_want_to_delete, place.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlace(place)
                    }
                ) {
                    Text(stringResource(R.string.delete_button))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.cancelDeletingPlace() }
                ) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
}

@Composable
private fun PlaceItemWithActions(
    place: Place,
    onPlaceSelected: (Place) -> Unit,
    onEditPlace: () -> Unit,
    onDeletePlace: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlaceSelected(place) }
            .padding(vertical = dimensionResource(R.dimen.padding_minor)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Place icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .padding(dimensionResource(R.dimen.padding) / 2),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (place.icon) {
                    "home" -> Icons.Default.Home
                    "work" -> Icons.Default.Place
                    else -> Icons.Default.Place
                },
                contentDescription = place.name,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_size)),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // Place details
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = dimensionResource(R.dimen.padding))
        ) {
            Text(
                text = place.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = place.type,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Edit button
        IconButton(
            onClick = onEditPlace,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(R.string.edit_place),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // Delete button
        IconButton(
            onClick = onDeletePlace,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete_button),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun EditPlaceDialog(
    place: Place,
    onDismiss: () -> Unit,
    onEdit: (Place) -> Unit
) {
    var name by remember { mutableStateOf(place.name) }
    var type by remember { mutableStateOf(place.type) }
    var isHome by remember { mutableStateOf(place.icon == "home") }
    var isWork by remember { mutableStateOf(place.icon == "work") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_place)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.place_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_minor)))
                
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text(stringResource(R.string.place_description)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding)))
                
                // Home/Work toggle buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(
                        onClick = { 
                            isHome = true
                            isWork = false
                        },
                        enabled = !isHome
                    ) {
                        Text(stringResource(R.string.set_as_home))
                    }
                    
                    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.padding)))
                    
                    TextButton(
                        onClick = { 
                            isWork = true
                            isHome = false
                        },
                        enabled = !isWork
                    ) {
                        Text(stringResource(R.string.set_as_work))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updatedPlace = place.copy(
                        name = name,
                        type = type,
                        icon = when {
                            isHome -> "home"
                            isWork -> "work"
                            else -> place.icon
                        }
                    )
                    onEdit(updatedPlace)
                }
            ) {
                Text(stringResource(R.string.save_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}
