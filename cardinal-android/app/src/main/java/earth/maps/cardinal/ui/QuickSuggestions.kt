package earth.maps.cardinal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import earth.maps.cardinal.R
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.data.Place

@Composable
fun QuickSuggestions(
    onMyLocationSelected: () -> Unit,
    savedPlaces: List<Place>,
    onSavedPlaceSelected: (Place) -> Unit,
    isGettingLocation: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        // My Location item
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimensionResource(dimen.padding))
                    .clickable { onMyLocationSelected() },
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
                                imageVector = Icons.Default.LocationOn,
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
                            text = "Use current location",
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
                    place = place,
                    onPlaceSelected = onSavedPlaceSelected
                )
            }
        }
    }
}

@Composable
private fun SavedPlaceItem(
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
                    imageVector = when (place.icon) {
                        "home" -> Icons.Default.LocationOn // You might want to add specific icons
                        "work" -> Icons.Default.LocationOn
                        else -> Icons.Default.LocationOn
                    },
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
                        text = address.format()?.take(50) ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
