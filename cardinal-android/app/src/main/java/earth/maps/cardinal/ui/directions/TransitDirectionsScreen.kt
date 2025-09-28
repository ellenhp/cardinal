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

package earth.maps.cardinal.ui.directions

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.drawable
import earth.maps.cardinal.R.string
import earth.maps.cardinal.transit.Mode
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransitDirectionsScreen(viewModel: DirectionsViewModel) {
    val planState = viewModel.planState
    when {
        planState.isLoading -> {
            Text(
                text = stringResource(string.calculating_route_in_progress),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(dimen.padding))
            )
        }

        planState.error != null -> {
            Text(
                text = stringResource(string.directions_error, planState.error),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(dimen.padding))
            )
        }

        planState.planResponse != null -> {
            TransitTimelineResults(
                planResponse = planState.planResponse,
                modifier = Modifier.fillMaxWidth()
            )
        }

        else -> {
            // No plan calculated yet
            Text(
                text = stringResource(string.enter_start_and_end_locations_to_get_directions),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(dimen.padding))
            )
        }
    }
}

@Composable
private fun TransitTimelineResults(
    planResponse: earth.maps.cardinal.transit.PlanResponse,
    modifier: Modifier = Modifier
) {
    Log.d("TAG", "${planResponse.itineraries.size}")
    LazyColumn(modifier = modifier) {
        items(planResponse.itineraries.size) { index ->
            val itinerary = planResponse.itineraries[index]

            TransitItineraryCard(
                itinerary = itinerary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimensionResource(dimen.padding))
            )
        }
    }
}

@Composable
private fun TransitItineraryCard(
    itinerary: earth.maps.cardinal.transit.Itinerary,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Card(
        modifier = modifier,
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(dimen.padding))
        ) {
            // Itinerary summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimensionResource(dimen.padding)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Depart: ${itinerary.startTime.formatTime()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Arrive: ${itinerary.endTime.formatTime()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatDuration(itinerary.duration),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${itinerary.transfers} ${if (itinerary.transfers == 1) "transfer" else "transfers"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Timeline of legs
            Column(modifier = Modifier.fillMaxWidth()) {
                itinerary.legs.forEachIndexed { index, leg ->
                    TransitLegTimelineItem(
                        leg = leg,
                        isLast = index == itinerary.legs.lastIndex,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun TransitLegTimelineItem(
    leg: earth.maps.cardinal.transit.Leg,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top
    ) {
        // Timeline indicator
        Column(
            modifier = Modifier
                .padding(end = dimensionResource(dimen.padding))
                .width(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = leg.routeColor?.let {
                            parseRouteColor(
                                it
                            )
                        }
                            ?: MaterialTheme.colorScheme.primary,
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(leg.mode.getIcon()),
                    contentDescription = null,
                    tint = leg.routeTextColor?.let {
                        parseRouteColor(
                            it
                        )
                    }
                        ?: MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Connection line (except for last item)
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(24.dp)
                        .background(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(1.dp)
                        )
                )
            }
        }

        // Leg details
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Route name and headsign
                    val routeText =
                        leg.routeShortName ?: leg.mode.name.lowercase().capitalize()
                            .replaceFirstChar { it.uppercase() }
                    Text(
                        text = "$routeText ${leg.headsign ?: ""}".trim(),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2
                    )

                    // Agency
                    leg.agencyName?.let { agency ->
                        Text(
                            text = agency,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Duration
                Text(
                    text = formatDuration(leg.duration),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Walking/Transit details
            when (leg.mode) {
                Mode.WALK, Mode.BIKE -> {
                    leg.distance?.let { distance ->
                        Text(
                            text = "${String.format("%.1f", distance / 1000)} km",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                else -> {
                    // Show transfer info or other transit details
                    leg.intermediateStops?.size?.let { stops ->
                        Text(
                            text = "${stops} stops",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Add spacing before next leg
            if (!isLast) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Add spacing between legs
    if (!isLast) {
        Spacer(modifier = Modifier.height(dimensionResource(dimen.padding)))
    }
}

// Extension functions for formatting
@OptIn(ExperimentalTime::class)
private fun String.formatTime(): String {
    // Parse ISO 8601 time and format to readable time
    return try {
        val instant = Instant.parse(this)
        val localDateTime =
            instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        "${localDateTime.hour.toString().padStart(2, '0')}:${
            localDateTime.minute.toString().padStart(2, '0')
        }"
    } catch (e: Exception) {
        this // fallback to original string
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    return if (minutes < 60) {
        "$minutes min"
    } else {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        "${hours}h ${remainingMinutes}min"
    }
}

private fun parseRouteColor(colorString: String?): androidx.compose.ui.graphics.Color? {
    if (colorString.isNullOrBlank()) return null
    return try {
        androidx.compose.ui.graphics.Color("#$colorString".toColorInt())
    } catch (e: Exception) {
        null
    }
}

private fun Mode.getIcon(): Int {
    return when (this) {
        Mode.WALK -> drawable.mode_walk
        Mode.BIKE -> drawable.mode_bike
        Mode.CAR, Mode.CAR_PARKING, Mode.CAR_DROPOFF -> drawable.mode_car
        Mode.BUS -> drawable.ic_bus_railway
        Mode.TRAM -> drawable.ic_bus_railway
        Mode.SUBWAY -> drawable.ic_subway_walk
        Mode.RAIL, Mode.HIGHSPEED_RAIL, Mode.REGIONAL_RAIL, Mode.REGIONAL_FAST_RAIL -> drawable.ic_bus_railway
        Mode.FERRY -> drawable.ic_bus_railway
        Mode.AIRPLANE -> drawable.ic_bus_railway
        else -> drawable.mode_walk
    }
}

