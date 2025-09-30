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

package earth.maps.cardinal.ui.directions

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.core.graphics.toColorInt
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.drawable
import earth.maps.cardinal.R.string
import earth.maps.cardinal.data.AppPreferenceRepository
import earth.maps.cardinal.data.GeoUtils
import earth.maps.cardinal.data.formatDuration
import earth.maps.cardinal.data.formatTime
import earth.maps.cardinal.transit.Itinerary
import earth.maps.cardinal.transit.Leg
import earth.maps.cardinal.transit.Mode

@Composable
fun TransitItineraryDetailScreen(
    itinerary: Itinerary,
    onBack: () -> Unit,
    appPreferences: AppPreferenceRepository
) {
    BackHandler {
        onBack()
    }

    val use24HourFormat by appPreferences.use24HourFormat.collectAsState()
    val distanceUnit by appPreferences.distanceUnit.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(dimensionResource(dimen.padding))
    ) {
        // Header with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = dimensionResource(dimen.padding)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(drawable.ic_arrow_back),
                    contentDescription = stringResource(string.back)
                )
            }
            Text(
                text = stringResource(string.trip_details),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
        }

        // Basic itinerary information card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(dimen.padding))
            ) {
                // Summary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = dimensionResource(dimen.padding)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(
                                string.depart,
                                itinerary.startTime.formatTime(use24HourFormat)
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(
                                string.arrive,
                                itinerary.endTime.formatTime(use24HourFormat)
                            ),
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

                // Detailed itinerary overview
                Text(
                    text = stringResource(string.journey_overview),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = dimensionResource(dimen.padding_minor))
                )

                Text(
                    text = stringResource(
                        string.total_walking_distance,
                        calculateTotalDistance(itinerary, distanceUnit)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(dimen.padding)))

        // Detailed step-by-step journey
        Text(
            text = stringResource(string.step_by_step_journey),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = dimensionResource(dimen.padding))
        )

        // Journey timeline
        itinerary.legs.forEachIndexed { index, leg ->
            DetailedLegCard(
                leg = leg,
                use24HourFormat = use24HourFormat,
                distanceUnit = distanceUnit,
                modifier = Modifier.padding(bottom = dimensionResource(dimen.padding))
            )
        }
    }
}

@Composable
private fun calculateTotalDistance(itinerary: Itinerary, distanceUnit: Int): String {
    val totalDistanceMeters = itinerary.legs.sumOf { it.distance ?: 0.0 }
    return if (totalDistanceMeters > 0) {
        GeoUtils.formatDistance(totalDistanceMeters, distanceUnit)
    } else {
        stringResource(string.distance_not_available)
    }
}

@Composable
private fun DetailedLegCard(
    leg: Leg,
    use24HourFormat: Boolean,
    distanceUnit: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(dimen.padding))
        ) {
            LegHeader(leg)

            Spacer(modifier = Modifier.height(dimensionResource(dimen.padding)))

            JourneyDetails(leg, use24HourFormat)

            LegAdditionalDetails(leg, distanceUnit)

            LegAlerts(leg)
        }
    }
}

@Composable
private fun LegHeader(leg: Leg) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mode icon with route color
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = parseRouteColor(leg.routeColor)
                        ?: MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(leg.mode.getIcon()),
                contentDescription = null,
                tint = parseRouteColor(leg.routeTextColor)
                    ?: MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(dimensionResource(dimen.padding)))

        Column(modifier = Modifier.weight(1f)) {
            // Route name and headsign
            val routeText = leg.routeShortName ?: leg.mode.name.lowercase()
                .replaceFirstChar { it.uppercase() }
            Text(
                text = if (leg.headsign != null) "$routeText to ${leg.headsign}" else routeText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
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
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun JourneyDetails(leg: Leg, use24HourFormat: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "From",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = leg.fromTransitPlace.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            leg.fromTransitPlace.departure?.let { departure ->
                Text(
                    text = "Depart: ${departure.formatTime(use24HourFormat)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            painter = painterResource(drawable.ic_arrow_forward),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(horizontal = dimensionResource(dimen.padding_minor)),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "To",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = leg.toTransitPlace.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            leg.toTransitPlace.arrival?.let { arrival ->
                Text(
                    text = "Arrive: ${arrival.formatTime(use24HourFormat)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LegAdditionalDetails(leg: Leg, distanceUnit: Int) {
    when (leg.mode) {
        Mode.WALK, Mode.BIKE -> {
            leg.distance?.let { distance ->
                Spacer(modifier = Modifier.height(dimensionResource(dimen.padding_minor)))
                HorizontalDivider(
                    thickness = DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(dimensionResource(dimen.padding_minor)))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Distance:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = GeoUtils.formatDistance(distance, distanceUnit),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        else -> {
            // Transit leg details
            leg.intermediateStops?.let { stops ->
                if (stops.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(dimensionResource(dimen.padding_minor)))
                    HorizontalDivider(
                        thickness = DividerDefaults.Thickness,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(dimen.padding_minor)))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(string.stops),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(string.stops_qty, stops.size),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegAlerts(leg: Leg) {
    leg.alerts?.let { alerts ->
        if (alerts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(dimensionResource(dimen.padding_minor)))
            HorizontalDivider(
                thickness = DividerDefaults.Thickness,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(modifier = Modifier.height(dimensionResource(dimen.padding_minor)))

            alerts.forEach { alert ->
                alert.headerText?.let { header ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(drawable.ic_close), // Using close as warning icon
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(dimensionResource(dimen.padding_minor)))
                        Text(
                            text = header,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

private fun parseRouteColor(colorString: String?): androidx.compose.ui.graphics.Color? {
    if (colorString.isNullOrBlank()) return null
    return try {
        androidx.compose.ui.graphics.Color("#$colorString".toColorInt())
    } catch (_: Exception) {
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
