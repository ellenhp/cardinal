package earth.maps.cardinal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.data.GeocodeResult
import earth.maps.cardinal.data.LatLng
import earth.maps.cardinal.data.Place

@Composable
fun SearchResults(
    geocodeResults: List<GeocodeResult>,
    onPlaceSelected: (Place) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(geocodeResults) { result ->
            SearchResultItem(result = result, onPlaceSelected = onPlaceSelected)
        }
    }
}

@Composable
private fun SearchResultItem(result: GeocodeResult, onPlaceSelected: (Place) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(dimen.padding))
            .clickable {
                // Convert GeocodeResult to Place with a unique ID based on properties
                val place = Place(
                    id = generatePlaceId(result),
                    name = result.displayName,
                    type = "Search Result",
                    icon = "search",
                    latLng = LatLng(
                        latitude = result.latitude,
                        longitude = result.longitude,
                    ),
                    address = result.address
                )
                onPlaceSelected(place)
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(dimen.padding)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search result icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .padding(dimensionResource(dimen.padding) / 2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(dimen.icon_size))
                )
            }

            // Search result details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = dimensionResource(dimen.padding))
            ) {
                Text(
                    text = result.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                result.address?.let { address ->
                    Text(
                        text = buildString {
                            address.houseNumber?.let { append("$it ") }
                            address.road?.let { append("$it, ") }
                            address.city?.let { append("$it, ") }
                            address.state?.let { append("$it ") }
                            address.postcode?.let { append("$it, ") }
                            address.country?.let { append(it) }
                        }.trim().trimEnd(','),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Generate a unique ID for a place based on its properties.
 * This ensures that each search result gets a consistent but unique ID.
 */
private fun generatePlaceId(result: GeocodeResult): Int {
    // Create a string representation of the unique properties
    val uniqueString = buildString {
        append(result.latitude)
        append(result.longitude)
        append(result.displayName)
        result.address?.let { address ->
            append(address.houseNumber ?: "")
            append(address.road ?: "")
            append(address.city ?: "")
            append(address.state ?: "")
            append(address.postcode ?: "")
            append(address.country ?: "")
        }
    }

    // Generate a hash code and ensure it's positive
    return kotlin.math.abs(uniqueString.hashCode())
}
