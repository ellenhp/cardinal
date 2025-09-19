package earth.maps.cardinal.ui

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.string
import earth.maps.cardinal.data.GeocodeResult
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.deduplicateSearchResults
import earth.maps.cardinal.viewmodel.HomeViewModel
import earth.maps.cardinal.viewmodel.ManagePlacesViewModel
import kotlin.math.abs

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    managePlacesViewModel: ManagePlacesViewModel,
    onPlaceSelected: (Place) -> Unit,
    onPeekHeightChange: (dp: Dp) -> Unit,
    isSearchFocused: Boolean,
    onSearchFocusChange: (Boolean) -> Unit
) {
    val savedPlaces = viewModel.savedPlaces.value
    val geocodeResults = deduplicateSearchResults(viewModel.geocodeResults.value)
    val isSearching = viewModel.isSearching
    val searchQuery = viewModel.searchQuery

    Column {
        SearchPanelContent(
            searchQuery = searchQuery,
            onSearchQueryChange = { query ->
                viewModel.updateSearchQuery(query)
            },
            isSearchFocused = isSearchFocused,
            onSearchFocusChange = onSearchFocusChange,
            savedPlaces = savedPlaces,
            geocodeResults = geocodeResults,
            isSearching = isSearching,
            managePlacesViewModel = managePlacesViewModel,
            onPeekHeightChange = onPeekHeightChange,
            onPlaceSelected = onPlaceSelected
        )
    }
}

@Composable
private fun SearchPanelContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearchFocused: Boolean,
    onSearchFocusChange: (Boolean) -> Unit,
    savedPlaces: List<Place>,
    geocodeResults: List<GeocodeResult>,
    isSearching: Boolean,
    managePlacesViewModel: ManagePlacesViewModel,
    onPeekHeightChange: (dp: Dp) -> Unit,
    onPlaceSelected: (Place) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(dimen.padding))
    ) {
        val density = LocalDensity.current

        // Measure the height of this row for peekHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    val heightInDp = with(density) { coordinates.size.height.toDp() }
                    onPeekHeightChange(heightInDp)
                }
        ) {
            // Search box with "Where to?" placeholder
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimensionResource(dimen.padding))
                    .onFocusChanged { focusState ->
                        onSearchFocusChange(focusState.isFocused)
                    },
                placeholder = { Text(stringResource(string.where_to)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(string.content_description_search)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        FilledTonalIconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(string.content_description_clear_search)
                            )
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(dimensionResource(dimen.icon_size))
            )

            // Pinned destinations.
            var showManagePlacesDialog by remember { mutableStateOf(false) }

            if (showManagePlacesDialog) {
                ManagePlacesDialog(
                    onDismiss = { showManagePlacesDialog = false },
                    onPlaceSelected = { place ->
                        showManagePlacesDialog = false
                        onPlaceSelected(place)
                    },
                    viewModel = managePlacesViewModel
                )
            }

            // Find pinned places
            val homePlace = savedPlaces.find { it.icon == "home" }
            val workPlace = savedPlaces.find { it.icon == "work" }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimensionResource(dimen.padding))
            ) {
                NavigationIcon(
                    text = stringResource(string.home),
                    icon = Icons.Default.Home,
                    place = homePlace,
                    onPlaceSelected = onPlaceSelected
                )
                NavigationIcon(
                    text = stringResource(string.work),
                    icon = Icons.Default.AccountCircle,
                    place = workPlace,
                    onPlaceSelected = onPlaceSelected
                )

                FilledTonalIconButton(
                    modifier = Modifier.size(48.dp),
                    onClick = {
                        showManagePlacesDialog = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(
                            string.content_description_edit_saved_places
                        )
                    )
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

        // Content: either saved places or search results
        LazyColumn {
            if (isSearchFocused) {
                // Show search results
                if (isSearching) {
                    item {
                        Text(
                            text = stringResource(string.searching_in_progress),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(dimensionResource(dimen.padding))
                        )
                    }
                } else {
                    items(geocodeResults) { result ->
                        SearchResultItem(result = result, onPlaceSelected = onPlaceSelected)
                    }
                }
            } else {
                // Show saved places
                items(savedPlaces) { place ->
                    PlaceItem(place = place, onClick = {
                        onPlaceSelected(place)
                    })
                }
            }
        }

        if (isSearchFocused) {
            Spacer(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun PlaceItem(place: Place, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(dimen.padding))
            .clickable(
                true,
                onClick = onClick
            ),
        onClick = {
            Log.d("Place", "$place")
            onClick()
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(dimen.padding)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Place icon (simplified)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .padding(dimensionResource(dimen.padding) / 2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (place.icon) {
                        "home" -> Icons.Default.Home
                        "work" -> Icons.Default.AccountCircle
                        else -> Icons.Default.LocationOn
                    },
                    contentDescription = place.name,
                    modifier = Modifier.size(dimensionResource(dimen.icon_size))
                )
            }

            // Place details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = dimensionResource(dimen.padding))
            ) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = place.type,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun NavigationIcon(
    icon: ImageVector,
    text: String,
    place: Place?,
    onPlaceSelected: (Place) -> Unit,
) {
    FilledTonalButton(
        onClick = { place?.let { onPlaceSelected(it) } },
        modifier = Modifier.padding(end = dimensionResource(dimen.padding_minor)),
        enabled = place != null
    ) {
        Row(modifier = Modifier.padding(vertical = 4.dp)) {
            Icon(
                imageVector = icon,
                modifier = Modifier
                    .padding(end = 4.dp),
                contentDescription = null // This is fine because the semantic information is provided by the text.
            )
            Text(
                modifier = Modifier.align(Alignment.CenterVertically),
                text = text
            )
        }
    }
}

/**
 * Generate a unique ID for a place based on its properties.
 * This ensures that each search result gets a consistent but unique ID.
 */
fun generatePlaceId(result: GeocodeResult): Int {
    // Create a string representation of the unique properties
    val uniqueString = buildString {
        append(result.latitude)
        append(result.longitude)
        append(result.displayName)
    }

    // Generate a hash code and ensure it's positive
    return abs(uniqueString.hashCode())
}
