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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.string
import earth.maps.cardinal.data.LatLng
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.transit.StopTime
import earth.maps.cardinal.viewmodel.TransitStopCardViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant

fun Color.contrastColor(): Color {
    // Calculate luminance using the standard formula
    val luminance = (this.red * 0.299f + this.green * 0.587f + this.blue * 0.114f)
    // Return black for light backgrounds and white for dark backgrounds
    return if (luminance > 0.5f) Color.Black else Color.White
}

@Composable
fun TransitStopInformation(viewModel: TransitStopCardViewModel, onRouteClicked: (Place) -> Unit) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val isRefreshingDepartures = viewModel.isRefreshingDepartures.collectAsState()
    val isInitiallyLoading = viewModel.isLoading.collectAsState()
    val didLoadingFail = viewModel.didLoadingFail.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initializeDepartures()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(
                start = dimensionResource(dimen.padding),
                end = dimensionResource(dimen.padding),
            )
    ) {
        // Departures section
        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(string.next_departures),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            // Refresh button for departures
            IconButton(onClick = { coroutineScope.launch { viewModel.refreshDepartures() } }) {
                if (isRefreshingDepartures.value) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp), strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        painter = painterResource(earth.maps.cardinal.R.drawable.ic_refresh),
                        contentDescription = stringResource(string.refresh_departures)
                    )
                }
            }
        }

        if (isInitiallyLoading.value) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                text = stringResource(string.loading_departures)
            )
            // Show indeterminate progress bar while loading
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        } else if (didLoadingFail.value) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                text = stringResource(string.failed_to_load_departures)
            )
        } else if (viewModel.departures.value.isEmpty()) {
            Text(
                text = stringResource(string.no_upcoming_departures),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            val maxDeparturesPerHeadsign = 3
            // List of departures grouped by route and headsign
            RouteDepartures(
                stopTimes = viewModel.departures.value,
                maxDepartures = maxDeparturesPerHeadsign,
                onRouteClicked = onRouteClicked,
            )
        }
    }

}

@OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RouteDepartures(
    stopTimes: List<StopTime>, maxDepartures: Int, onRouteClicked: (Place) -> Unit
) {
    // Group departures by route name
    val departuresByRoute = stopTimes.groupBy { it.routeShortName }

    departuresByRoute.forEach { (routeName, departures) ->
        val routeColor = departures.firstOrNull()?.routeColor?.let {
            Color(
                "#$it".toColorInt()
            )
        } ?: MaterialTheme.colorScheme.surfaceVariant

        val onRouteColor = routeColor.contrastColor()

        // Group departures by headsign within each route
        val departuresByHeadsign = departures.groupBy { it.headsign }.map { (key, value) ->
            (key to value.take(maxDepartures))
        }.toMap()
        val headsigns = departuresByHeadsign.keys.toList().sorted()

        val transitStopString = stringResource(string.transit_stop)
        val places = remember {
            departuresByHeadsign.map { (_, value) ->
                value.firstOrNull()?.let { departure ->
                    Place(
                        name = departure.place.name,
                        type = transitStopString,
                        latLng = LatLng(departure.place.lat, departure.place.lon),
                        isTransitStop = true
                    )
                }
            }
        }

        val pagerState = rememberPagerState(pageCount = { headsigns.size })
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
                .clickable(onClick = {
                    places[pagerState.currentPage]?.let { place ->
                        onRouteClicked(
                            place
                        )
                    }
                }),
            colors = CardDefaults.cardColors(
                containerColor = routeColor,
                contentColor = onRouteColor,
            )
        ) {
            val scrollScope = rememberCoroutineScope()
            val density = LocalDensity.current
            var routeNamePadding = remember { mutableStateOf<Dp?>(null) }

            Box {
                // Route name header
                Text(
                    text = routeName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = onRouteColor,
                    modifier = Modifier
                        .padding(dimensionResource(dimen.padding))
                        .onGloballyPositioned {
                            routeNamePadding.value = with(density) { it.size.height.toDp() }
                        })

                Box(modifier = Modifier.align(Alignment.TopCenter)) {
                    PageIndicator(headsigns.size, currentPage = pagerState.currentPage)
                }
                val routeNamePaddingLocal = routeNamePadding.value
                if (routeNamePaddingLocal != null) {
                    // Swipeable pager for headsigns with fixed height
                    FixedHeightHorizontalPager(
                        state = pagerState,
                        departuresByHeadsign = departuresByHeadsign,
                        headsigns = headsigns,
                        onRouteColor = onRouteColor,
                        routeNamePaddingLocal
                    )
                }
            }
        }
    }
}

@Composable
fun PageIndicator(pageCount: Int, currentPage: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        repeat(pageCount) { index ->
            val color = if (index == currentPage) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(8.dp)
                    .background(color, CircleShape)
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun DepartureRow(
    stopTime: StopTime, isFirst: Boolean, onRouteColor: Color, showStop: Boolean = false
) {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val isoFormatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault())
    val textColor = onRouteColor

    val scheduledDepartureInstant = stopTime.place.scheduledDeparture?.let {
        try {
            Instant.from(isoFormatter.parse(it))
        } catch (_: Exception) {
            null
        }
    }
    val scheduledDepartureTime: String? = scheduledDepartureInstant?.let {
        try {
            dateFormat.format(Date.from(it))
        } catch (_: Exception) {
            null
        }
    }

    val bestDepartureInstant = stopTime.place.departure?.let {
        try {
            Instant.from(isoFormatter.parse(it))
        } catch (_: Exception) {
            null
        }
    } ?: scheduledDepartureInstant

    val bestDepartureTime: String? = bestDepartureInstant?.let {
        try {
            dateFormat.format(Date.from(it))
        } catch (_: Exception) {
            scheduledDepartureTime
        }
    } ?: scheduledDepartureTime

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val stopTimeStyle = if (isFirst) {
            MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        } else {
            MaterialTheme.typography.bodyMedium
        }
        Column(
            modifier = Modifier
                .padding(horizontal = dimensionResource(dimen.padding))
                .fillMaxWidth()
        ) {
            // Departure time at the bottom
            val timeUntilDeparture =
                bestDepartureInstant?.toKotlinInstant()?.minus(Clock.System.now())
            Text(
                modifier = Modifier.align(Alignment.End),
                text = if (timeUntilDeparture != null && timeUntilDeparture > 30.minutes) {
                    bestDepartureTime ?: stringResource(string.unknown_departure)
                } else if (timeUntilDeparture != null) {
                    "${timeUntilDeparture.inWholeMinutes} min"
                } else {
                    bestDepartureTime ?: stringResource(string.unknown_departure)
                },
                textAlign = TextAlign.End,
                style = stopTimeStyle,
                color = textColor,
                fontWeight = if (stopTime.realTime) FontWeight.Medium else FontWeight.Normal
            )
            // Real-time or scheduled indicator
            val isLiveIndicatorString = if (stopTime.realTime) {
                stringResource(string.live_indicator)
            } else {
                stringResource(string.scheduled_indicator)
            }

            Text(
                text = isLiveIndicatorString,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FixedHeightHorizontalPager(
    state: PagerState,
    departuresByHeadsign: Map<String, List<StopTime>>,
    headsigns: List<String>,
    onRouteColor: Color,
    headsignTopPadding: Dp,
) {
    val topPadding = dimensionResource(
        dimen.padding_minor
    )
    val maxChildHeight = remember(key1 = departuresByHeadsign) {
        mutableStateOf(0.dp)
    }
    val density = LocalDensity.current

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        HorizontalPager(
            state = state,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val selectedHeadsign = headsigns[page]
            val departuresForSelectedHeadsign =
                departuresByHeadsign[selectedHeadsign] ?: emptyList()
            Row {
                Column {
                    Text(
                        modifier = Modifier
                            .padding(
                                start = dimensionResource(dimen.padding),
                                top = dimensionResource(dimen.padding) + headsignTopPadding,
                            )
                            .fillMaxWidth(0.6f),
                        text = selectedHeadsign,
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.bodyLarge,
                        color = onRouteColor
                    )
                    Text(
                        modifier = Modifier
                            .padding(
                                start = dimensionResource(dimen.padding),
                                top = dimensionResource(dimen.padding_minor),
                            )
                            .fillMaxWidth(0.6f),
                        text = departuresForSelectedHeadsign.first().place.name,
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.bodySmall,
                        color = onRouteColor
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(top = topPadding)
                        .defaultMinSize(minHeight = maxChildHeight.value)
                        .onGloballyPositioned({ coordinates ->
                            val heightDp = with(density) { coordinates.size.height.toDp() }
                            if (heightDp > maxChildHeight.value) {
                                maxChildHeight.value = heightDp
                            }
                        })
                ) {
                    departuresForSelectedHeadsign.forEachIndexed { index, stopTime ->
                        DepartureRow(
                            stopTime = stopTime,
                            isFirst = index == 0,
                            onRouteColor = onRouteColor,
                        )
                        // Add divider between departure rows, but not after the last one
                        if (index < departuresForSelectedHeadsign.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                thickness = DividerDefaults.Thickness / 2,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(dimensionResource(dimen.padding_minor)))
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class)
fun formatDepartureTime(stopTime: StopTime): String {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val isoFormatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault())

    val scheduledDepartureInstant = stopTime.place.scheduledDeparture?.let {
        try {
            Instant.from(isoFormatter.parse(it))
        } catch (_: Exception) {
            null
        }
    }
    val scheduledDepartureTime: String? = scheduledDepartureInstant?.let {
        try {
            dateFormat.format(Date.from(it))
        } catch (_: Exception) {
            null
        }
    }

    val bestDepartureInstant = stopTime.place.departure?.let {
        try {
            Instant.from(isoFormatter.parse(it))
        } catch (_: Exception) {
            null
        }
    } ?: scheduledDepartureInstant

    val timeUntilDeparture = bestDepartureInstant?.toKotlinInstant()?.minus(Clock.System.now())

    return if (timeUntilDeparture != null && timeUntilDeparture > 30.minutes) {
        scheduledDepartureTime ?: stringResource(string.unknown_departure)
    } else if (timeUntilDeparture != null) {
        "${timeUntilDeparture.inWholeMinutes} min"
    } else {
        scheduledDepartureTime ?: stringResource(string.unknown_departure)
    }
}

