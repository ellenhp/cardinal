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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import earth.maps.cardinal.R
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.drawable
import earth.maps.cardinal.data.AddressFormatter
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.format

@Composable
fun QuickSuggestions(
    onMyLocationSelected: () -> Unit,
    savedPlaces: List<Place>,
    onSavedPlaceSelected: (Place) -> Unit,
    isGettingLocation: Boolean,
    modifier: Modifier = Modifier
) {
    val addressFormatter = remember { AddressFormatter() }
    LazyColumn(modifier = modifier) {
        // My Location item
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimensionResource(dimen.padding))
                    .clickable {
                        onMyLocationSelected()
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(dimen.padding)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Location icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .padding(dimensionResource(dimen.padding) / 2),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isGettingLocation) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                painter = painterResource(drawable.my_location),
                                contentDescription = null,
                                modifier = Modifier.size(dimensionResource(dimen.icon_size)),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // My Location text
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = dimensionResource(dimen.padding))
                    ) {
                        Text(
                            text = stringResource(R.string.my_location),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.use_current_location),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Saved places
        if (savedPlaces.isNotEmpty()) {
            items(savedPlaces) { place ->
                SavedPlaceItem(
                    addressFormatter = addressFormatter,
                    place = place,
                    onPlaceSelected = onSavedPlaceSelected
                )
            }
        }
    }
}

@Composable
private fun SavedPlaceItem(
    addressFormatter: AddressFormatter,
    place: Place,
    onPlaceSelected: (Place) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(dimen.padding))
            .clickable { onPlaceSelected(place) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(dimen.padding)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Place icon based on type
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .padding(dimensionResource(dimen.padding) / 2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        when (place.icon) {
                            "home" -> drawable.ic_home
                            "work" -> drawable.ic_work
                            else -> drawable.ic_location_on
                        }
                    ),
                    contentDescription = null,
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
                place.address?.let { address ->
                    Text(
                        text = address.format(addressFormatter)?.take(50) ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
