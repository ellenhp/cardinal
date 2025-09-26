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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.drawable
import earth.maps.cardinal.R.string
import earth.maps.cardinal.data.AddressFormatter
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.format
import earth.maps.cardinal.viewmodel.NearbyViewModel

@Composable
fun NearbyScreenContent(viewModel: NearbyViewModel, onPlaceSelected: (Place) -> Unit) {
    val nearbyResults by viewModel.nearbyResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val addressFormatter = remember { AddressFormatter() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(dimen.padding))
            .onGloballyPositioned { coordinates ->
                // This would normally update peek height, but for nearby content
                // we don't need special peek height management like other screens
            }
    ) {
        Row {
            // Header with nearby title
            Text(
                text = stringResource(string.nearby_places),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(bottom = dimensionResource(dimen.padding))
                    .weight(1f)
            )
            IconButton(onClick = { viewModel.refreshData() }) {
                Icon(
                    painter = painterResource(drawable.ic_refresh),
                    contentDescription = stringResource(string.refresh_nearby_places)
                )
            }

        }

        when {
            isLoading -> {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                // Error state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(string.error_loading_nearby_places),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(dimen.padding)))
                    Button(onClick = { viewModel.refreshData() }) {
                        Text(stringResource(string.retry))
                    }
                }
            }

            nearbyResults.isEmpty() -> {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(string.no_nearby_places_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                // Results list
                LazyColumn {
                    items(nearbyResults) { place ->
                        NearbyPlaceCard(
                            addressFormatter = addressFormatter,
                            place = place,
                            onPlaceSelected = onPlaceSelected
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NearbyPlaceCard(
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(dimen.padding))
        ) {
            // Place name and icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(drawable.ic_location_on),
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(dimen.icon_size)),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(dimensionResource(dimen.padding)))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = place.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Address information
            place.address?.let { address ->
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimensionResource(dimen.padding) / 2)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        painter = painterResource(drawable.ic_location_on),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(dimensionResource(dimen.padding)))
                    Text(
                        text = address.format(addressFormatter)
                            ?: stringResource(string.address_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
