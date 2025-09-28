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

package earth.maps.cardinal.ui.saved

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.drawable
import earth.maps.cardinal.R.string
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.room.SavedPlace
import earth.maps.cardinal.ui.core.TOOLBAR_HEIGHT_DP

@Composable
fun SavedPlacesList(
    viewModel: SavedPlacesViewModel, onPlaceSelected: (Place) -> Unit
) {
    val savedPlaces by viewModel.observeAllPlaces().collectAsState(emptyList())

    Box(modifier = Modifier.fillMaxWidth()) {
        if (savedPlaces.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(
                    Alignment.Center
                )
            ) {
                Text(
                    stringResource(string.no_saved_places_yet),
                    style = MaterialTheme.typography.bodyLarge
                )
                Icon(
                    modifier = Modifier
                        .size(64.dp)
                        .padding(dimensionResource(dimen.padding)),
                    painter = painterResource(drawable.ic_add_location),
                    contentDescription = stringResource(string.add_save_places_and_they_ll_show_up_here)
                )
            }
        } else {
            LazyColumn {
                items(savedPlaces) { place ->
                    PlaceItem(place = place, onClick = {
                        onPlaceSelected(viewModel.convertToPlace(place))
                    })
                }
                item {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(TOOLBAR_HEIGHT_DP + dimensionResource(dimen.padding_minor))
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceItem(place: SavedPlace, onClick: () -> Unit) {
    val name = place.customName ?: place.name
    val description = place.customDescription ?: place.type
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = dimensionResource(dimen.padding) / 2,
            )
            .clickable(
                true, onClick = onClick
            ), onClick = onClick, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(dimen.padding)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Place icon (simplified)
            // Place icon and pinned indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .padding(dimensionResource(dimen.padding) / 2),
                contentAlignment = Alignment.Center
            ) {
                Row {
                    Icon(
                        painter = painterResource(
                            when (place.icon) {
                                "home" -> drawable.ic_home
                                "work" -> drawable.ic_work
                                else -> drawable.ic_location_on
                            }
                        ),
                        contentDescription = place.name,
                        modifier = Modifier.size(dimensionResource(dimen.icon_size)),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    if (place.isPinned) {
                        Icon(
                            painter = painterResource(drawable.ic_bookmark_star),
                            contentDescription = stringResource(string.pin_place),
                            modifier = Modifier.size(dimensionResource(dimen.icon_size)),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Place details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = dimensionResource(dimen.padding))
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
