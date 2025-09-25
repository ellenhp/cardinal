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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import earth.maps.cardinal.R
import earth.maps.cardinal.data.room.ListContent
import earth.maps.cardinal.data.room.ListContentItem
import earth.maps.cardinal.data.room.PlaceContent
import earth.maps.cardinal.viewmodel.SavedPlacesUiState
import earth.maps.cardinal.viewmodel.SavedPlacesViewModel

@Composable
fun SavedPlacesList(
    viewModel: SavedPlacesViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onPlaceSelected: (String) -> Unit = {},
    onListSelected: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isEditMode = viewModel.isEditMode
    val selectedItems = viewModel.selectedItems

    Scaffold(
        topBar = {
            SavedPlacesTopBar(
                isEditMode = isEditMode,
                selectedCount = selectedItems.size,
                onBack = onBack,
                onEditToggle = viewModel::toggleEditMode,
                onSelectAll = viewModel::selectAllItems,
                onClearSelection = viewModel::clearSelection,
                onDeleteSelected = viewModel::deleteSelectedItems
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val uiState = uiState
            when (uiState) {
                is SavedPlacesUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is SavedPlacesUiState.Success -> {
                    if (uiState.content.isEmpty()) {
                        EmptySavedPlacesContent()
                    } else {
                        SavedPlacesContent(
                            content = uiState.content,
                            isEditMode = isEditMode,
                            selectedItems = selectedItems,
                            onItemClicked = { item ->
                                if (isEditMode) {
                                    viewModel.toggleItemSelection(item.id)
                                } else {
                                    when (item) {
                                        is PlaceContent -> onPlaceSelected(item.id)
                                        is ListContentItem -> onListSelected(item.id)
                                    }
                                }
                            },
                            onItemLongClicked = { item ->
                                if (!isEditMode) {
                                    viewModel.toggleEditMode()
                                    viewModel.toggleItemSelection(item.id)
                                }
                            },
                            onReorder = viewModel::reorderItems,
                            onToggleCollapse = viewModel::toggleListCollapse,
                            onUpdateName = viewModel::updateItemName
                        )
                    }
                }

                is SavedPlacesUiState.Error -> {
                    Text(
                        text = uiState.message,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedPlacesTopBar(
    isEditMode: Boolean,
    selectedCount: Int,
    onBack: () -> Unit,
    onEditToggle: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    TopAppBar(
        title = {
            if (isEditMode && selectedCount > 0) {
                Text("$selectedCount selected")
            } else {
                Text("Saved Places")
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.content_description_back)
                )
            }
        },
        actions = {
            if (isEditMode) {
                if (selectedCount > 0) {
                    IconButton(onClick = onDeleteSelected) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.content_description_delete_routing_profile)
                        )
                    }
                    IconButton(onClick = onSelectAll) {
                        Icon(
                            painter = painterResource(R.drawable.ic_star),
                            contentDescription = stringResource(R.string.content_description_select_all)
                        )
                    }
                }
                IconButton(onClick = {
                    if (selectedCount > 0) {
                        onClearSelection()
                    } else {
                        onEditToggle()
                    }
                }) {
                    Text(
                        if (selectedCount > 0) stringResource(R.string.cancel) else stringResource(
                            R.string.done
                        )
                    )
                }
            } else {
                IconButton(onClick = onEditToggle) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = stringResource(R.string.content_description_edit_routing_profile)
                    )
                }
            }
        }
    )
}

@Composable
private fun EmptySavedPlacesContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_location_on),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.padding(8.dp))
        Text(
            text = "No saved places yet",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.padding(4.dp))
        Text(
            text = "Save places to view them here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SavedPlacesContent(
    content: List<ListContent>,
    isEditMode: Boolean,
    selectedItems: Set<String>,
    onItemClicked: (ListContent) -> Unit,
    onItemLongClicked: (ListContent) -> Unit,
    onReorder: (List<ListContent>) -> Unit,
    onToggleCollapse: (String, Boolean) -> Unit,
    onUpdateName: (String, String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<ListContent?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(content, key = { it.id }) { item ->
            val isSelected = item.id in selectedItems

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .combinedClickable(
                        onClick = { onItemClicked(item) },
                        onLongClick = { onItemLongClicked(item) }
                    ),
                elevation = if (isSelected) androidx.compose.material3.CardDefaults.cardElevation(8.dp) else androidx.compose.material3.CardDefaults.cardElevation(
                    2.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Selection checkbox in edit mode
                    if (isEditMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onItemClicked(item) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Drag handle in edit mode
                    if (isEditMode) {
                        Icon(
                            painter = painterResource(R.drawable.ic_drag_handle),
                            contentDescription = stringResource(R.string.content_description_drag_to_reorder),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Item icon
                    when (item) {
                        is PlaceContent -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_location_on),
                                contentDescription = null,
                                tint = Color.Red
                            )
                        }

                        is ListContentItem -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_star),
                                contentDescription = null,
                                tint = Color.Blue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Item content
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )

                        when (item) {
                            is PlaceContent -> {
                                if (item.customDescription != null) {
                                    Text(
                                        text = item.customDescription,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            is ListContentItem -> {
                                if (item.description != null) {
                                    Text(
                                        text = item.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Collapse/Expand button for lists
                    if (item is ListContentItem) {
                        IconButton(
                            onClick = { onToggleCollapse(item.id, item.isCollapsed) }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_forward),
                                contentDescription = if (item.isCollapsed) stringResource(R.string.content_description_expand) else stringResource(
                                    R.string.content_description_collapse
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Item") },
            text = { Text("Are you sure you want to delete this item?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Handle deletion
                        showDeleteDialog = false
                        itemToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        itemToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
