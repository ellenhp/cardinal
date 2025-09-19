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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import earth.maps.cardinal.R
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.data.DownloadStatus
import earth.maps.cardinal.data.OfflineArea
import earth.maps.cardinal.viewmodel.OfflineAreasViewModel
import kotlinx.coroutines.launch
import org.maplibre.compose.util.VisibleRegion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineAreasScreen(
    currentViewport: VisibleRegion,
    currentZoom: Double,
    viewModel: OfflineAreasViewModel = hiltViewModel(),
    snackBarHostState: SnackbarHostState,
    onDismiss: () -> Unit,
    onAreaSelected: (OfflineArea) -> Unit = {}
) {
    val offlineAreas by viewModel.offlineAreas
    val isDownloading by viewModel.isDownloading

    var showDownloadDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var areaToDelete by remember { mutableStateOf<OfflineArea?>(null) }
    var selectedArea by remember { mutableStateOf<OfflineArea?>(null) }

    // Format for displaying dates
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val zoomInMessage = stringResource(R.string.zoom_in_to_download_an_area)

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.padding_minor))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = dimensionResource(dimen.padding)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.offline_areas_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close)
                )
            }
        }

        // Download button
        Button(
            onClick = {
                if (currentZoom < 8) {
                    coroutineScope.launch {
                        snackBarHostState.showSnackbar(zoomInMessage)
                    }
                } else {
                    showDownloadDialog = true
                }
            }, modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = dimensionResource(dimen.padding)), enabled = !isDownloading
        ) {
            Icon(
                painter = painterResource(R.drawable.cloud_download_24dp),
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(text = stringResource(R.string.download_new_area))
        }

        // List of offline areas
        Text(
            text = stringResource(R.string.saved_offline_areas),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (offlineAreas.isEmpty()) {
            Text(
                text = stringResource(R.string.no_offline_areas),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(dimen.padding))
                    .align(Alignment.CenterHorizontally)
            )
        } else {
            LazyColumn {
                items(offlineAreas) { area ->
                    OfflineAreaItem(
                        area = area, dateFormat = dateFormat, onDeleteClick = {
                            areaToDelete = area
                            showDeleteDialog = true
                        }, onSelected = {
                            selectedArea = area
                            onAreaSelected(area)
                        }, isSelected = selectedArea?.id == area.id
                    )
                }
            }
        }
    }

    // Download dialog
    if (showDownloadDialog) {
        DownloadAreaDialog(
            currentViewport = currentViewport,
            onDismiss = { showDownloadDialog = false },
            onDownload = { name, boundingBox ->
                viewModel.startDownload(boundingBox, name)
                showDownloadDialog = false
            })
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = {
                Text(
                    stringResource(
                        R.string.delete_area_confirmation, areaToDelete?.name ?: ""
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        areaToDelete?.let { viewModel.deleteOfflineArea(it) }
                        showDeleteDialog = false
                        areaToDelete = null
                    }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        areaToDelete = null
                    }) {
                    Text(stringResource(R.string.cancel))
                }
            })
    }
}

@Composable
fun OfflineAreaItem(
    area: OfflineArea,
    dateFormat: SimpleDateFormat,
    onDeleteClick: () -> Unit,
    onSelected: () -> Unit,
    isSelected: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable { onSelected() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(dimen.padding))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = area.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_area)
                    )
                }
            }

            // Status
            val statusText = when (area.status) {
                DownloadStatus.PENDING -> stringResource(R.string.status_pending)
                DownloadStatus.DOWNLOADING_BASEMAP -> stringResource(R.string.status_downloading_basemap)
                DownloadStatus.DOWNLOADING_VALHALLA -> stringResource(R.string.status_downloading_valhalla)
                DownloadStatus.PROCESSING_GEOCODER -> stringResource(R.string.status_processing)
                DownloadStatus.COMPLETED -> stringResource(R.string.status_completed)
                DownloadStatus.FAILED -> stringResource(R.string.status_failed)
            }

            Text(
                text = stringResource(R.string.status_label, statusText),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Date
            val dateString = dateFormat.format(Date(area.downloadDate))
            Text(
                text = stringResource(R.string.downloaded_on, dateString),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Zoom levels
            Text(
                text = stringResource(R.string.zoom_levels, area.minZoom, area.maxZoom),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )

            // File size
            val fileSizeText = formatFileSize(area.fileSize)
            Text(
                text = stringResource(R.string.file_size, fileSizeText),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun DownloadAreaDialog(
    currentViewport: VisibleRegion,
    onDismiss: () -> Unit,
    onDownload: (name: String, boundingBox: BoundingBox) -> Unit
) {
    val viewModel: OfflineAreasViewModel = hiltViewModel()

    // Calculate default bounding box from current viewport
    val (north, south, east, west) = calculateBoundingBoxFromViewport(
        currentViewport
    )

    // Pre-fill area name with current date/time
    val currentTime = System.currentTimeMillis()
    val date = Date(currentTime)
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    val initialName = "Offline Map ${dateFormat.format(date)}"

    var nameState by remember { mutableStateOf(initialName) }

    // Create a FocusRequester to request focus on the text field
    val focusRequester = remember { FocusRequester() }

    val minZoom = 5
    val maxZoom = 14

    var nameError by remember { mutableStateOf(false) }

    // Calculate estimated tile count (fixed zoom levels 7-14) - recalculate when bounds change
    val estimatedTileCount by remember(north, south, east, west) {
        mutableIntStateOf(
            if (north >= south && east >= west) {
                viewModel.estimateTileCount(BoundingBox(north, south, east, west), minZoom, maxZoom)
            } else {
                0
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.download_new_area)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = nameState,
                    onValueChange = { newValue ->
                        nameState = newValue
                        nameError = false
                    },
                    label = { Text(stringResource(R.string.area_name)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    isError = nameError,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
                if (nameError) {
                    Text(
                        text = stringResource(R.string.name_required),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.estimated_tiles, estimatedTileCount),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = dimensionResource(dimen.padding))
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Validate inputs
                    var valid = true
                    val name = nameState

                    if (name.isBlank()) {
                        nameError = true
                        valid = false
                    }

                    if (valid) {
                        onDownload(
                            name, BoundingBox(north, south, east, west)
                        )
                    }
                }) {
                Text(stringResource(R.string.download))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        })
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }
}

fun calculateBoundingBoxFromViewport(
    viewport: VisibleRegion
): BoundingBox {
    return BoundingBox(
        north = viewport.farLeft.latitude,
        south = viewport.nearRight.latitude,
        east = viewport.nearRight.longitude,
        west = viewport.farLeft.longitude,
    )
}

data class BoundingBox(
    val north: Double, val south: Double, val east: Double, val west: Double
)
