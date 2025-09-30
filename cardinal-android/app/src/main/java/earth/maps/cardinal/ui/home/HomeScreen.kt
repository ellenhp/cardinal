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

package earth.maps.cardinal.ui.home

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.drawable
import earth.maps.cardinal.R.string
import earth.maps.cardinal.data.AddressFormatter
import earth.maps.cardinal.data.GeocodeResult
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.ui.place.SearchResultItem
import earth.maps.cardinal.ui.saved.SavedPlacesList
import earth.maps.cardinal.ui.saved.SavedPlacesViewModel
import kotlin.math.abs

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPlaceSelected: (Place) -> Unit,
    onPeekHeightChange: (dp: Dp) -> Unit,
    onSearchFocusChange: (Boolean) -> Unit,
    onResultPinsChange: (List<Place>) -> Unit,
    onSearchEvent: () -> Unit,
) {
    val searchQuery = viewModel.searchQuery

    Column {
        SearchPanelContent(
            viewModel = viewModel,
            searchQuery = searchQuery,
            onSearchQueryChange = { query ->
                viewModel.updateSearchQuery(query)
            },
            onSearchFocusChange = onSearchFocusChange,
            onResultPinsChange = onResultPinsChange,
            onPeekHeightChange = onPeekHeightChange,
            onSearchEvent = onSearchEvent,
            onPlaceSelected = onPlaceSelected,
            homeInSearchScreen = viewModel.searchQuery.text.isNotEmpty(),
        )
    }
}

@Composable
private fun SearchPanelContent(
    viewModel: HomeViewModel,
    searchQuery: TextFieldValue,
    onSearchQueryChange: (TextFieldValue) -> Unit,
    onSearchFocusChange: (Boolean) -> Unit,
    onResultPinsChange: (List<Place>) -> Unit,
    onSearchEvent: () -> Unit,
    onPeekHeightChange: (dp: Dp) -> Unit,
    onPlaceSelected: (Place) -> Unit,
    homeInSearchScreen: Boolean,
) {
    val addressFormatter = remember { AddressFormatter() }
    val pinnedPlaces by viewModel.pinnedPlaces().collectAsState(emptyList())
    val geocodePlaces by viewModel.geocodePlaces.collectAsState(emptyList())

    LaunchedEffect(geocodePlaces) {
        onResultPinsChange(geocodePlaces)
    }

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
                }) {
            val textField = remember { FocusRequester() }
            // Search box with "Where to?" placeholder
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchEvent() }),
                singleLine = true,
                modifier = Modifier
                    .focusRequester(textField)
                    .fillMaxWidth()
                    .padding(bottom = dimensionResource(dimen.padding))
                    .onFocusChanged { focusState ->
                        onSearchFocusChange(focusState.isFocused)
                    },
                placeholder = { Text(stringResource(string.where_to)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(drawable.ic_search),
                        contentDescription = stringResource(string.content_description_search)
                    )
                },
                trailingIcon = {
                    if (searchQuery.text.isNotEmpty()) {
                        FilledTonalIconButton(
                            onClick = { onSearchQueryChange(TextFieldValue()) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(drawable.ic_close),
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

            // This block is responsible for returning focus to the search bar after we pop back to the search panel from a place card.
            LaunchedEffect(homeInSearchScreen) {
                if (homeInSearchScreen) {
                    textField.requestFocus()
                }
            }

            AnimatedVisibility(
                visible = pinnedPlaces.isNotEmpty()
            ) {
                // The homeInSearchScreen isn't part of the animated visibility condition because it looks weird.
                if (!homeInSearchScreen) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = dimensionResource(dimen.padding))
                    ) {
                        for (place in pinnedPlaces)
                            NavigationIcon(
                                place = place,
                                onPlaceSelected = onPlaceSelected
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

        if (homeInSearchScreen) {
            LazyColumn {
                items(geocodePlaces) {
                    SearchResultItem(
                        addressFormatter = addressFormatter,
                        it,
                        onPlaceSelected
                    )
                }
            }
            Spacer(modifier = Modifier.fillMaxSize())
        } else {
            val savedPlacesViewModel = hiltViewModel<SavedPlacesViewModel>()
            SavedPlacesList(savedPlacesViewModel, onPlaceSelected = onPlaceSelected)
        }
    }
}

@Composable
fun NavigationIcon(
    place: Place,
    onPlaceSelected: (Place) -> Unit,
) {
    FilledTonalButton(
        onClick = { onPlaceSelected(place) },
        modifier = Modifier.padding(end = dimensionResource(dimen.padding_minor)),
    ) {
        Row(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                modifier = Modifier.align(Alignment.CenterVertically), text = place.name
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
