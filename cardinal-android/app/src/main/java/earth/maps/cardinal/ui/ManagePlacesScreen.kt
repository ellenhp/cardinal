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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.drawable
import earth.maps.cardinal.R.string
import earth.maps.cardinal.data.room.ListContent
import earth.maps.cardinal.data.room.ListContentItem
import earth.maps.cardinal.data.room.PlaceContent
import earth.maps.cardinal.viewmodel.ManagePlacesViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ManagePlacesScreen(
    navController: NavController,
    listId: String?,
    parents: List<String>,
) {
    val viewModel: ManagePlacesViewModel = hiltViewModel(key = listId ?: "root")

    val currentListName by viewModel.currentListName.collectAsState(initial = null)
    // This is not always equal to listId, notably in the case of a root list referred to implicitly by a `null` listId
    val currentListId by viewModel.currentListId.collectAsState(initial = null)
    val currentListContent by viewModel.currentListContent.collectAsState(initial = null)
    val clipboard by viewModel.clipboard.collectAsState(emptySet())
    val selectedItems by viewModel.selectedItems.collectAsState()
    val isAllSelected by viewModel.isAllSelected.collectAsState(initial = false)
    val showDeleteConfirmation = remember { mutableStateOf(false) }
    val showCreateListDialog = remember { mutableStateOf(false) }
    val showEditDialog = remember { mutableStateOf(false) }
    val editingItem = remember { mutableStateOf<ListContent?>(null) }
    var newListName by remember { mutableStateOf("") }
    var editName by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var editPinned by remember { mutableStateOf(false) }
    var fabMenuExpanded by remember { mutableStateOf(false) }

    // Initialize the view model with the listId if provided
    LaunchedEffect(listId) {
        viewModel.setInitialList(listId)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            ManagePlacesTopBar(
                navController = navController,
                title = currentListName ?: stringResource(string.saved_places_title_case),
                breadcrumbNames = parents.plus(currentListName ?: ""),
            )
        }) { paddingValues ->
        if (currentListContent?.isEmpty() == true) {
            Column(
                modifier = Modifier.padding(paddingValues)
            ) {
                EmptyListContent()
            }
        }

        val coroutineScope = rememberCoroutineScope()
        val currentListContent = currentListContent
        Box(modifier = Modifier.padding(paddingValues)) {
            AnimatedVisibility(
                visible = currentListContent?.isNotEmpty() == true,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ListContentGrid(
                    viewModel = viewModel,
                    content = currentListContent ?: emptyList(),
                    selectedItems = selectedItems,
                    bottomSpacer = TOOLBAR_HEIGHT_DP + dimensionResource(
                        dimen.padding
                    ) + dimensionResource(dimen.fab_height), // FAB height.
                    clipboard = clipboard,
                    onItemClick = { item ->
                        when (item) {
                            is PlaceContent -> {
                                coroutineScope.launch {
                                    val place = viewModel.getSavedPlace(item.id) ?: return@launch
                                    NavigationUtils.navigate(
                                        navController,
                                        Screen.PlaceCard(place)
                                    )
                                }
                            }

                            is ListContentItem -> {
                                currentListId?.let { currentListId ->
                                    NavigationUtils.navigate(
                                        navController,
                                        Screen.ManagePlaces(
                                            item.id,
                                            parents = parents.plus(currentListName ?: "")
                                        ),
                                        avoidCycles = false
                                    )
                                }
                            }
                        }
                    },
                    onEditClick = { editingItem.value = it; showEditDialog.value = true }
                )
            }
        }
        val modifier = if (fabMenuExpanded) {
            Modifier
                .clickable(
                    indication = null,
                    interactionSource = null,
                    onClick = { fabMenuExpanded = false })
        } else {
            Modifier
        }
        Box(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(bottom = TOOLBAR_HEIGHT_DP)
        ) {
            FloatingActionButtonMenu(
                modifier = Modifier.align(Alignment.BottomEnd),
                expanded = fabMenuExpanded,
                button = {
                    ToggleFloatingActionButton(
                        checked = fabMenuExpanded,
                        onCheckedChange = {
                            fabMenuExpanded = it
                        },
                        content = {
                            val close = painterResource(drawable.ic_close)
                            val open = painterResource(drawable.ic_menu)
                            val painter by remember {
                                derivedStateOf {
                                    if (checkedProgress > 0.5f) close else open
                                }
                            }
                            Icon(
                                painter = painter,
                                contentDescription = null,
                                modifier = Modifier.animateIcon({ checkedProgress }),
                            )
                        }
                    )
                }
            ) {
                FloatingActionButtonMenuItem(
                    onClick = {
                        showCreateListDialog.value = true
                        fabMenuExpanded = false
                    },
                    text = {
                        Text(
                            text = stringResource(
                                string.new_list
                            )
                        )
                    },
                    icon = {
                        Icon(
                            painter = painterResource(drawable.ic_new_list),
                            contentDescription = null
                        )
                    })

                FloatingActionButtonMenuItem(
                    onClick = {
                        viewModel.cutSelected()
                        fabMenuExpanded = false
                    },
                    text = {
                        Text(text = stringResource(string.cut))
                    },
                    icon = {
                        Icon(
                            painter = painterResource(drawable.ic_content_cut),
                            contentDescription = null
                        )
                    }
                )
                FloatingActionButtonMenuItem(
                    onClick = {
                        viewModel.pasteSelected()
                        fabMenuExpanded = false
                    },
                    text = {
                        Text(text = stringResource(string.paste))
                    },
                    icon = {
                        Icon(
                            painter = painterResource(drawable.ic_content_paste),
                            contentDescription = null
                        )
                    }
                )
                FloatingActionButtonMenuItem(
                    onClick = {
                        showDeleteConfirmation.value = true
                        fabMenuExpanded = false
                    },
                    text = {
                        Text(text = stringResource(string.delete))
                    },
                    icon = {
                        Icon(
                            painter = painterResource(drawable.ic_delete),
                            contentDescription = null
                        )
                    }
                )
                FloatingActionButtonMenuItem(
                    onClick = {
                        if (isAllSelected) {
                            viewModel.clearSelection()
                        } else {
                            viewModel.selectAll()
                        }
                    },
                    text = {
                        Text(text = stringResource(if (isAllSelected) string.deselect_all else string.select_all))
                    },
                    icon = {
                        Icon(
                            painter = painterResource(if (isAllSelected) drawable.ic_clear_selection else drawable.ic_select_all),
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }

    if (showDeleteConfirmation.value) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation.value = false },
            title = { Text(stringResource(string.confirm_delete)) },
            text = { Text(stringResource(string.delete_confirmation_message, selectedItems.size)) },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteSelected()
                    showDeleteConfirmation.value = false
                }) {
                    Text(stringResource(string.delete))
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirmation.value = false }) {
                    Text(stringResource(string.cancel))
                }
            }
        )
    }

    if (showCreateListDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showCreateListDialog.value = false
                newListName = ""
            },
            title = { Text(stringResource(string.add_new_list)) },
            text = {
                OutlinedTextField(
                    value = newListName,
                    onValueChange = { newListName = it },
                    label = { Text(stringResource(string.list_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newListName.isNotBlank()) {
                        viewModel.createNewListWithSelected(newListName)
                        showCreateListDialog.value = false
                        newListName = ""
                    }
                }) {
                    Text(stringResource(string.add_new_list))
                }
            },
            dismissButton = {
                Button(onClick = {
                    showCreateListDialog.value = false
                    newListName = ""
                }) {
                    Text(stringResource(string.cancel))
                }
            }
        )
    }

    if (showEditDialog.value) {
        editingItem.value?.let { item ->
            LaunchedEffect(item) {
                when (item) {
                    is PlaceContent -> {
                        editName = item.name
                        editDescription = item.customDescription ?: ""
                        editPinned = item.isPinned
                    }

                    is ListContentItem -> {
                        editName = item.name
                        editDescription = item.description ?: ""
                        editPinned = false
                    }
                }
            }

            val title = when (item) {
                is PlaceContent -> stringResource(string.edit_place)
                is ListContentItem -> stringResource(string.edit_list)
            }

            AlertDialog(
                onDismissRequest = { showEditDialog.value = false },
                title = { Text(title) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text(stringResource(string.name)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editDescription,
                            onValueChange = { editDescription = it },
                            label = { Text(stringResource(string.description)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (item is PlaceContent) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = editPinned,
                                    onCheckedChange = { editPinned = it }
                                )
                                Text(stringResource(string.pin_place))
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val name = if (editName.isBlank()) null else editName
                        val desc = if (editDescription.isBlank()) null else editDescription
                        when (item) {
                            is PlaceContent -> viewModel.updatePlace(
                                item.id,
                                name,
                                desc,
                                editPinned
                            )

                            is ListContentItem -> viewModel.updateList(item.id, name, desc)
                        }
                        showEditDialog.value = false
                    }) {
                        Text(stringResource(string.save))
                    }
                },
                dismissButton = {
                    Button(onClick = { showEditDialog.value = false }) {
                        Text(stringResource(string.cancel))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ManagePlacesTopBar(
    navController: NavController,
    title: String,
    breadcrumbNames: List<String>,
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                )
                // Breadcrumbs
                if (breadcrumbNames.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensionResource(dimen.padding)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        breadcrumbNames.forEachIndexed { index, name ->
                            if (index > 0) {
                                Text(
                                    text = " - ", // Don't replace this with a carat or arrow without dealing with RTL layouts.
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (index == breadcrumbNames.size - 1) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = if (index < breadcrumbNames.size - 1) {
                                    Modifier.clickable {
                                        // Navigate back to this level
                                        repeat(breadcrumbNames.size - 1 - index) {
                                            navController.popBackStack()
                                        }
                                    }
                                } else {
                                    Modifier
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun EmptyListContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
    ) {
        Text(
            text = stringResource(string.no_saved_places_yet),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.padding(16.dp))
        Icon(
            modifier = Modifier.size(64.dp),
            painter = painterResource(drawable.ic_add_location),
            contentDescription = stringResource(string.add_save_places_and_they_ll_show_up_here),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ListContentGrid(
    viewModel: ManagePlacesViewModel,
    content: List<Flow<ListContent?>>,
    clipboard: Set<String>,
    selectedItems: Set<String>,
    onItemClick: (ListContent) -> Unit,
    onEditClick: (ListContent) -> Unit,
    bottomSpacer: Dp
) {
    val states = content.map {
        it.collectAsState(null).value
    }
    val lists = states.filterIsInstance<ListContentItem>().sortedBy { it.position }
    val places = states.filterIsInstance<PlaceContent>().sortedBy { it.position }
    val pinnedPlaces = places.filter { it.isPinned }
    val unpinnedPlaces = places.filter { !it.isPinned }

    LazyColumn {
        // Lists section
        val shouldShowListsHeader = lists.isNotEmpty()
        if (shouldShowListsHeader) {
            item {
                SectionHeader(
                    title = stringResource(string.saved_lists),
                    modifier = Modifier.padding(
                        vertical = dimensionResource(dimen.padding_minor),
                        horizontal = dimensionResource(dimen.padding)
                    )
                )
            }
        }
        items(lists) { item ->
            ListItem(
                item = item,
                isInClipboard = clipboard.contains(item.id),
                isSelected = selectedItems.contains(item.id),
                onSelectionChange = { viewModel.toggleSelection(item.id) },
                onClick = { onItemClick(item) },
                onEditClick = onEditClick
            )
        }

        // Pinned places section
        if (pinnedPlaces.isNotEmpty() || (places.isNotEmpty() && unpinnedPlaces.isNotEmpty())) {
            item {
                SectionHeader(
                    title = stringResource(string.pinned_places_title_case),
                    modifier = Modifier.padding(
                        vertical = dimensionResource(dimen.padding_minor),
                        horizontal = dimensionResource(dimen.padding)
                    )
                )
            }
            items(pinnedPlaces) { item ->
                PlaceItem(
                    item = item,
                    isInClipboard = clipboard.contains(item.id),
                    isSelected = selectedItems.contains(item.id),
                    onSelectionChange = { viewModel.toggleSelection(item.id) },
                    onClick = { onItemClick(item) },
                    onEditClick = onEditClick
                )
            }
        }

        // Saved places section
        val shouldShowSavedPlacesHeader = places.size <= 1 || pinnedPlaces.isNotEmpty()
        if (unpinnedPlaces.isNotEmpty() && shouldShowSavedPlacesHeader) {
            item {
                SectionHeader(
                    title = stringResource(string.saved_places_title_case),
                    modifier = Modifier.padding(
                        vertical = dimensionResource(dimen.padding_minor),
                        horizontal = dimensionResource(dimen.padding)
                    )
                )
            }
        }
        items(unpinnedPlaces) { item ->
            PlaceItem(
                item = item,
                isInClipboard = clipboard.contains(item.id),
                isSelected = selectedItems.contains(item.id),
                onSelectionChange = { viewModel.toggleSelection(item.id) },
                onClick = { onItemClick(item) },
                onEditClick = onEditClick
            )
        }
        item {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bottomSpacer)
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

@Composable
private fun PlaceItem(
    item: PlaceContent,
    isInClipboard: Boolean,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onEditClick: (ListContent) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = dimensionResource(dimen.padding) / 2,
                horizontal = dimensionResource(dimen.padding)
            )
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(dimen.padding)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = isSelected, onCheckedChange = onSelectionChange)

            // Place details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = dimensionResource(dimen.padding))
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.customDescription ?: item.type,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { onEditClick(item) }) {
                Icon(
                    painter = painterResource(drawable.ic_edit),
                    contentDescription = stringResource(string.edit)
                )
            }

            if (isInClipboard) {
                Icon(
                    painter = painterResource(drawable.ic_content_cut),
                    contentDescription = stringResource(string.in_clipboard)
                )
            }
        }
    }
}

@Composable
private fun ListItem(
    item: ListContentItem,
    isInClipboard: Boolean,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onEditClick: (ListContent) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = dimensionResource(dimen.padding) / 2,
                horizontal = dimensionResource(dimen.padding)
            )
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(dimen.padding)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = isSelected, onCheckedChange = onSelectionChange)

            // List details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = dimensionResource(dimen.padding))
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                item.description?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // TODO: Show item count when available
            }

            IconButton(onClick = { onEditClick(item) }) {
                Icon(
                    painter = painterResource(drawable.ic_edit),
                    contentDescription = stringResource(string.edit)
                )
            }

            if (isInClipboard) {
                Icon(
                    painter = painterResource(drawable.ic_content_cut),
                    contentDescription = stringResource(string.in_clipboard)
                )
            }
            // Arrow to indicate this is a folder/list
            Icon(
                painter = painterResource(drawable.ic_arrow_forward),
                contentDescription = stringResource(string.open_list),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
