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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import earth.maps.cardinal.R
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.string
import earth.maps.cardinal.data.LatLng
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.transit.StopTime
import earth.maps.cardinal.ui.place.PageIndicator
import earth.maps.cardinal.ui.place.formatDepartureTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@Composable
fun TransitScreenContent(
    viewModel: TransitScreenViewModel,
    onRouteClicked: (Place) -> Unit,
) {
    var secondsSinceStart by rememberSaveable(viewModel) { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1.seconds)
            secondsSinceStart++
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val isRefreshingDepartures = viewModel.isRefreshingDepartures.collectAsState()
    val isLoading = viewModel.isLoading.collectAsState()
    val didLoadingFail = viewModel.didLoadingFail.collectAsState()
    val scrollState = rememberScrollState()
    val departures by viewModel.departures.collectAsState(emptyList())
    val use24HourFormat by viewModel.use24HourFormat.collectAsState()

    LaunchedEffect(secondsSinceStart) {
        if (secondsSinceStart % 30 == 0 && secondsSinceStart > 0) {
            viewModel.refreshData()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                start = dimensionResource(dimen.padding),
                end = dimensionResource(dimen.padding),
                bottom = 36.dp
            )
    ) {
        // Departures section
        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(string.next_departures),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(bottom = dimensionResource(dimen.padding))
                    .weight(1f)
            )
            // Refresh button for departures
            IconButton(onClick = { coroutineScope.launch { viewModel.refreshData() } }) {
                if (isRefreshingDepartures.value) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp), strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_refresh),
                        contentDescription = stringResource(string.refresh_departures)
                    )
                }
            }
        }

        if (didLoadingFail.value) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                text = stringResource(string.failed_to_load_departures)
            )
        } else if (isLoading.value && departures.isEmpty()) {
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
        } else if (departures.isEmpty()) {
            Text(
                text = stringResource(string.no_upcoming_departures),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            // List of departures grouped by route and headsign
            TransitScreenRouteDepartures(
                stopTimes = departures,
                onRouteClicked = onRouteClicked,
                use24HourFormat = use24HourFormat,
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun TransitScreenRouteDepartures(
    stopTimes: List<StopTime>, onRouteClicked: (Place) -> Unit, use24HourFormat: Boolean,
) {
    // Group departures by route name
    val departuresByRoute = stopTimes.groupBy { it.routeShortName }

    departuresByRoute.forEach { (routeName, departures) ->
        // Group departures by headsign within each route
        val departuresByHeadsign = departures.groupBy { it.headsign }.map { (key, value) ->
            (key to value.take(1))
        }.toMap()
        val headsigns = departuresByHeadsign.keys.toList().sorted()

        val transitStopString = stringResource(string.transit_stop)
        val places = remember(departures) {
            departuresByHeadsign.map { (key, value) ->
                key to value.firstOrNull()?.let { departure ->
                    Place(
                        name = departure.place.name,
                        description = transitStopString,
                        latLng = LatLng(departure.place.lat, departure.place.lon),
                        isTransitStop = true,
                        transitStopId = departure.place.stopId,
                    )
                }
            }.toMap()
        }

        val pagerState = rememberPagerState(pageCount = { headsigns.size })
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = {
                    places[headsigns[pagerState.currentPage]]?.let { place ->
                        onRouteClicked(place)
                    }
                }),
        ) {
            Card(modifier = Modifier.padding(bottom = dimensionResource(dimen.padding_minor))) {
                Box {
                    // Route name at top
                    Row(
                        modifier = Modifier.padding(dimensionResource(dimen.padding_minor))
                    ) {
                        val routeColor = departures.firstOrNull()?.parseRouteColor()
                            ?: MaterialTheme.colorScheme.surfaceVariant
                        Text(
                            text = stringResource(string.square_char),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = routeColor
                        )
                        Spacer(modifier = Modifier.width(dimensionResource(dimen.padding_minor)))
                        Text(
                            text = routeName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Page indicator
                        PageIndicator(headsigns.size, currentPage = pagerState.currentPage)

                        // Horizontal pager for headsigns
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                        ) { page ->
                            val selectedHeadsign = headsigns[page]
                            val departuresForHeadsign =
                                departuresByHeadsign[selectedHeadsign] ?: emptyList()
                            val soonestDeparture = departuresForHeadsign.minByOrNull {
                                val dep = it.place.departure ?: it.place.scheduledDeparture
                                dep ?: ""
                            } ?: return@HorizontalPager

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = dimensionResource(dimen.padding),
                                        bottom = dimensionResource(dimen.padding)
                                    ),
                            ) {
                                // Left side: headsign and stop
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(top = 24.dp)
                                        .align(Alignment.Bottom)
                                ) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        modifier = Modifier.basicMarquee(),
                                        text = selectedHeadsign,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = soonestDeparture.place.name,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                // Right side: departure time
                                val bestDepartureTime = formatDepartureTime(
                                    soonestDeparture,
                                    use24HourFormat = use24HourFormat
                                )
                                val containerContent = @Composable {
                                    Row(
                                        modifier = Modifier.padding(
                                            dimensionResource(dimen.padding),
                                        ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (soonestDeparture.realTime) {
                                            val infiniteTransition =
                                                rememberInfiniteTransition(label = "alpha animation")
                                            val animatedAlpha by infiniteTransition.animateFloat(
                                                initialValue = 0.3f,
                                                targetValue = 1f,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween(
                                                        durationMillis = 750, easing = LinearEasing
                                                    ), repeatMode = RepeatMode.Reverse
                                                ),
                                                label = "alpha"
                                            )
                                            Text(
                                                modifier = Modifier.padding(end = 4.dp),
                                                text = stringResource(string.live_indicator_short),
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = animatedAlpha
                                                ),
                                                style = MaterialTheme.typography.headlineSmall
                                            )
                                        }
                                        Text(
                                            text = bestDepartureTime,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = if (soonestDeparture.realTime) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (soonestDeparture.realTime) 1f else 0.5f),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                        .padding(end = dimensionResource(dimen.padding))
                                        .defaultMinSize(minWidth = 50.dp, minHeight = 50.dp),
                                ) {
                                    containerContent()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
