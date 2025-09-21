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

import android.content.ClipData
import android.util.Log
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.string
import earth.maps.cardinal.data.room.ItemType
import earth.maps.cardinal.data.room.ListContent
import earth.maps.cardinal.data.room.ListItem
import earth.maps.cardinal.data.room.SavedList
import earth.maps.cardinal.data.room.SavedPlace
import earth.maps.cardinal.viewmodel.SavedPlacesViewModel
import kotlinx.coroutines.launch

@Composable
fun SavedPlacesList(
    viewModel: SavedPlacesViewModel = hiltViewModel(),
    onPlaceSelected: (String) -> Unit = {},
    onListSelected: (String) -> Unit = {},
    onSheetFixedChange: (Boolean) -> Unit = {}
) {
    val rootList by viewModel.observeRootList().collectAsState(null)
    val isEditMode = viewModel.isEditMode
    val selectedItems = viewModel.selectedItems

    LaunchedEffect(isEditMode) {
        onSheetFixedChange(isEditMode)
    }

    if (isEditMode) {
        BackHandler {
            viewModel.toggleEditMode()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        rootList?.let { rootList ->
            val content by viewModel.observeListChildren(rootList.id).collectAsState(emptyList())
            if (content.isEmpty()) {
                EmptySavedPlacesContent()
            } else {
                SavedPlacesContent(
                    viewModel = viewModel,
                    savedList = rootList,
                    isEditMode = isEditMode,
                    selectedItems = selectedItems,
                    onItemClicked = { item ->
                        if (isEditMode) {
                            viewModel.toggleItemSelection(item.itemId)
                        }
                    },
                    onItemLongClicked = { item ->
                        if (!isEditMode) {
                            viewModel.toggleEditMode()
                            viewModel.toggleItemSelection(item.itemId)
                        }
                    },
                    onReorder = viewModel::reorderItems,
                    onToggleCollapse = viewModel::toggleListCollapse,
                    onUpdateName = viewModel::updateItemName
                )
            }
        }
    }
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
            imageVector = Icons.Default.Place,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.padding(8.dp))
        Text(
            text = stringResource(string.no_saved_places_yet),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.padding(4.dp))
        Text(
            text = stringResource(string.save_places_to_view_them_here),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SavedPlacesContent(
    viewModel: SavedPlacesViewModel,
    savedList: SavedList,
    isEditMode: Boolean,
    selectedItems: Set<String>,
    onItemClicked: (ListItem) -> Unit,
    onItemLongClicked: (ListItem) -> Unit,
    onReorder: (List<ListItem>) -> Unit,
    onToggleCollapse: (String, Boolean) -> Unit,
    onUpdateName: (String, String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<ListContent?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(dimen.padding_minor)),
            ) {
                SavedPlacesListListItem(
                    savedList,
                    onToggleCollapse = onToggleCollapse,
                    viewModel = hiltViewModel<SavedPlacesViewModel>().also {
                        it.setListId(
                            savedList.id
                        )
                    },
                    isExpanded = true,
                    allowCollapse = false,
                    isSelected = false,
                    isEditMode = isEditMode,
                    onItemClicked = { item ->
                        coroutineScope.launch {
                            viewModel.maybeHandleListClick(item, fallback = onItemClicked)
                        }
                    },
                    onItemLongClicked = onItemLongClicked,
                    onAddNewList = { parentId ->
                        viewModel.addNewListToRoot("New List")
                    })
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(string.delete_item)) },
            text = { Text(stringResource(string.delete_confirmation_question)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Handle deletion
                        showDeleteDialog = false
                        itemToDelete = null
                    }) {
                    Text(stringResource(string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        itemToDelete = null
                    }) {
                    Text(stringResource(string.cancel))
                }
            })
    }
}

@Composable
private fun SavedPlacesListItem(
    item: ListItem,
    isSelected: Boolean,
    isEditMode: Boolean,
    onItemClicked: (ListItem) -> Unit,
    onItemLongClicked: (ListItem) -> Unit,
    viewModel: SavedPlacesViewModel,
    onToggleCollapse: (String, Boolean) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .combinedClickable(
                onClick = { onItemClicked(item) },
                onLongClick = { onItemLongClicked(item) }),
        elevation = if (isSelected) CardDefaults.cardElevation(
            8.dp
        ) else CardDefaults.cardElevation(
            2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 80.dp)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection checkbox in edit mode
            AnimatedVisibility(isEditMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        modifier = Modifier.padding(end = 8.dp),
                        checked = isSelected,
                        onCheckedChange = { onItemClicked(item) })
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Drag to reorder",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }


            // Item icon
            when (item.itemType) {
                ItemType.PLACE -> {
                    val item by viewModel.observePlace(item.itemId).collectAsState(null)
                    SavedPlacesPlaceListItem(item, isEditMode, viewModel)
                }

                ItemType.LIST -> {
                    val item by viewModel.observeList(item.itemId).collectAsState(null)
                    val isExpanded by viewModel.observeIsExpanded().collectAsState(false)
                    Log.d("Expanded", "$isExpanded")
                    item?.let { item ->
                        SavedPlacesListListItem(
                            viewModel = hiltViewModel<SavedPlacesViewModel>().also {
                                it.setListId(
                                    item.id
                                )
                            },
                            item = item,
                            isExpanded = isExpanded,
                            allowCollapse = true,
                            isSelected = viewModel.isItemSelected(item.id),
                            isEditMode = isEditMode,
                            onItemClicked = onItemClicked,
                            onItemLongClicked = onItemLongClicked,
                            onToggleCollapse = onToggleCollapse,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedPlacesListListItem(
    item: SavedList,
    isExpanded: Boolean,
    allowCollapse: Boolean,
    isSelected: Boolean,
    isEditMode: Boolean,
    onItemClicked: (ListItem) -> Unit,
    onItemLongClicked: (ListItem) -> Unit,
    viewModel: SavedPlacesViewModel,
    onToggleCollapse: (String, Boolean) -> Unit,
    onAddNewList: ((String) -> Unit)? = null,
) {
    val expandCollapseDurationMillis = 250
    Column(
        modifier = Modifier.dragAndDropTarget(shouldStartDragAndDrop = {
            true
        }, object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val androidDragEvent = event.toAndroidDragEvent()
                val clipData = androidDragEvent.clipData
                if (clipData != null && clipData.itemCount > 0 && isEditMode) {
                    // Extract the place ID from the clip data
                    val placeId = clipData.getItemAt(0).text.toString()
                    // Call the stub method to reparent the place to this list
                    viewModel.reparentPlaceToList(placeId, item.id)
                    return true
                }
                return false
            }
        })
    ) {
        Row {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                item.description?.let {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Add button to create a new list
            if (onAddNewList != null) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add new list",
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clickable { onAddNewList(item.id) })
            }

            if (allowCollapse) {
                Icon(
                    imageVector = if (item.isCollapsed) Icons.AutoMirrored.Filled.List else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (item.isCollapsed) "Expand" else "Collapse"
                )
            }
        }
        val enterTransition = remember {
            expandVertically(
                expandFrom = Alignment.Top, animationSpec = tween(expandCollapseDurationMillis)
            ) + fadeIn(
                initialAlpha = 0.3f, animationSpec = tween(expandCollapseDurationMillis)
            )
        }
        val exitTransition = remember {
            shrinkVertically(
                shrinkTowards = Alignment.Top, animationSpec = tween(expandCollapseDurationMillis)
            ) + fadeOut(
                animationSpec = tween(expandCollapseDurationMillis)
            )
        }

        val children by viewModel.observeListChildren(id = item.id).collectAsState(emptyList())

        for (child in children) {
            AnimatedVisibility(
                visible = isExpanded, enter = enterTransition, exit = exitTransition
            ) {
                SavedPlacesListItem(
                    viewModel = hiltViewModel<SavedPlacesViewModel>().also { it.setListId(child.itemId) },
                    isSelected = viewModel.isItemSelected(child.itemId),
                    item = child,
                    isEditMode = isEditMode,
                    onItemClicked = onItemClicked,
                    onItemLongClicked = onItemLongClicked,
                    onToggleCollapse = onToggleCollapse,
                )
            }
        }
    }
}

@Composable
private fun SavedPlacesPlaceListItem(
    item: SavedPlace?, isEditMode: Boolean, viewModel: SavedPlacesViewModel? = null
) {
    val placeItemModifier = if (isEditMode && item != null && viewModel != null) {
        Modifier.dragAndDropSource(
            transferData = {
                DragAndDropTransferData(
                    clipData = ClipData.newPlainText("SavedPlace", item.id),
                    flags = View.DRAG_FLAG_GLOBAL
                )

            })
    } else {
        Modifier
    }

    Row(modifier = placeItemModifier) {
        if (!isEditMode) {
            Icon(
                imageVector = Icons.Default.Place, contentDescription = null, tint = Color.Red
            )
        }
        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            val name = item?.customName ?: item?.name
            if (name != null) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            val description = item?.customDescription
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
