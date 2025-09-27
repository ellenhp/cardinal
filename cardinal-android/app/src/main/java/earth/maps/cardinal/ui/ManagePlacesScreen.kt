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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ManagePlacesScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    listId: String?,
    parents: List<String>,
) {
    val viewModel: ManagePlacesViewModel = hiltViewModel(key = listId ?: "root")

    val currentListName by viewModel.currentListName.collectAsState(initial = null)
    // This is not always equal to listId, notably in the case of a root list referred to implicitly by a `null` listId
    val currentListId by viewModel.currentListId.collectAsState(initial = null)
    val currentListContent by viewModel.currentListContent.collectAsState(initial = null)
    val navigationStack by viewModel.navigationStack.collectAsState()
    val selectedItems by viewModel.selectedItems.collectAsState()
    val isAllSelected by viewModel.isAllSelected.collectAsState(initial = false)
    val showDeleteConfirmation = remember { mutableStateOf(false) }
    val showCreateListDialog = remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }

    // Initialize the view model with the listId if provided
    LaunchedEffect(listId) {
        viewModel.setInitialList(listId)
    }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            ManagePlacesTopBar(
                navController = navController,
                title = currentListName ?: stringResource(string.saved_places_title_case),
                breadcrumbNames = parents.plus(currentListName ?: ""),
                onBackClick = { navController.popBackStack() },
                onCloseClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        if (currentListContent?.isEmpty() == true) {
            Column(
                modifier = Modifier.padding(paddingValues)
            ) {
                EmptyListContent()
            }
        }

        val currentListContent = currentListContent
        AnimatedVisibility(
            visible = currentListContent?.isNotEmpty() == true,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ListContentGrid(
                viewModel = viewModel,
                content = currentListContent ?: emptyList(),
                selectedItems = selectedItems,
                paddingValues = paddingValues,
                onItemClick = { item ->
                    when (item) {
                        is PlaceContent -> {
                            // TODO: Should we navigate to the POI? Or edit it?
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
                }
            )
        }
        Box(modifier = Modifier.fillMaxSize()) {
            VerticalFloatingToolbar(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                expanded = true,
                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
            ) {
                IconButton(onClick = { showCreateListDialog.value = true }) {
                    Icon(
                        painter = painterResource(drawable.ic_new_list),
                        contentDescription = stringResource(
                            string.new_list
                        )
                    )
                }
                IconButton(onClick = { showDeleteConfirmation.value = true }) {
                    Icon(
                        painter = painterResource(drawable.ic_delete),
                        contentDescription = stringResource(
                            string.delete
                        )
                    )
                }
                IconButton(onClick = {
                    if (isAllSelected) {
                        viewModel.clearSelection()
                    } else {
                        viewModel.selectAll()
                    }
                }) {
                    Icon(
                        painter = painterResource(if (isAllSelected) drawable.ic_clear_selection else drawable.ic_select_all),
                        contentDescription = stringResource(if (isAllSelected) string.deselect_all else string.select_all),
                    )
                }
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
}

@Composable
private fun ManagePlacesTopBar(
    navController: NavController,
    title: String,
    breadcrumbNames: List<String>,
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(dimen.padding)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button (only if can navigate back)
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(drawable.ic_arrow_back),
                    contentDescription = stringResource(string.back)
                )
            }

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )

            // Close button
            IconButton(onClick = onCloseClick) {
                Icon(
                    painter = painterResource(drawable.ic_close),
                    contentDescription = stringResource(string.close)
                )
            }
        }

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

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = dimensionResource(dimen.padding)),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
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
    selectedItems: Set<String>,
    onItemClick: (ListContent) -> Unit,
    paddingValues: PaddingValues
) {
    val states = content.map {
        it.collectAsState(null).value
    }
    val lists = states.filterIsInstance<ListContentItem>().sortedBy { it.position }
    val places = states.filterIsInstance<PlaceContent>().sortedBy { it.position }
    val pinnedPlaces = places.filter { it.isPinned }
    val unpinnedPlaces = places.filter { !it.isPinned }

    LazyColumn(modifier = Modifier.padding(paddingValues)) {
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
                isSelected = selectedItems.contains(item.id),
                onSelectionChange = { viewModel.toggleSelection(item.id) },
                onClick = { onItemClick(item) }
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
                    isSelected = selectedItems.contains(item.id),
                    onSelectionChange = { viewModel.toggleSelection(item.id) },
                    onClick = { onItemClick(item) }
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
                isSelected = selectedItems.contains(item.id),
                onSelectionChange = { viewModel.toggleSelection(item.id) },
                onClick = { onItemClick(item) }
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
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onClick: () -> Unit
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
        }
    }
}

@Composable
private fun ListItem(
    item: ListContentItem,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onClick: () -> Unit
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

            // Arrow to indicate this is a folder/list
            Icon(
                painter = painterResource(drawable.ic_arrow_forward),
                contentDescription = stringResource(string.open_list),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
