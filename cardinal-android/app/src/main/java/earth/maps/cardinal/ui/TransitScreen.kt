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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.string
import earth.maps.cardinal.data.LatLng
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.transit.StopTime
import earth.maps.cardinal.viewmodel.TransitScreenViewModel
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

@Composable
fun TransitScreenContent(
    viewModel: TransitScreenViewModel,
    onRouteClicked: (Place) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val isRefreshingDepartures = viewModel.isRefreshingDepartures.collectAsState()
    val isInitiallyLoading = viewModel.isLoading.collectAsState()
    val didLoadingFail = viewModel.didLoadingFail.collectAsState()
    val scrollState = rememberScrollState()

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
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            // Refresh button for departures
            IconButton(onClick = { coroutineScope.launch { viewModel.refreshData() } }) {
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
            TransitScreenRouteDepartures(
                stopTimes = viewModel.departures.value,
                maxDepartures = maxDeparturesPerHeadsign,
                onRouteClicked = onRouteClicked,
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun TransitScreenRouteDepartures(
    stopTimes: List<StopTime>, maxDepartures: Int, onRouteClicked: (Place) -> Unit
) {
    // Group departures by route name
    val departuresByRoute = stopTimes.groupBy { it.routeShortName }

    departuresByRoute.forEach { (routeName, departures) ->
        val routeColor = departures.firstOrNull()?.routeColor?.let {
            Color("#$it".toColorInt())
        } ?: MaterialTheme.colorScheme.surfaceVariant

        fun isYellow(color: Color): Boolean {
            val r = color.red
            val g = color.green
            val b = color.blue
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            if (max == min) return false // gray
            val delta = max - min
            val h = when (max) {
                r -> 60 * (g - b) / delta
                g -> 60 * (2 + (b - r) / delta)
                b -> 60 * (4 + (r - g) / delta)
                else -> 0f
            }
            val hue = if (h < 0) h + 360f else h
            return hue in 30f..75f
        }

        val cardContainerColor = if (isYellow(routeColor)) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            routeColor
        }

        val onRouteColor = cardContainerColor.contrastColor()

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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = {
                    places[pagerState.currentPage]?.let { place ->
                        onRouteClicked(place)
                    }
                }),
        ) {
            Box {
                // Route name at top
                Text(
                    text = routeName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(dimensionResource(dimen.padding_minor))
                )

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
                            val bestDepartureTime = formatDepartureTime(soonestDeparture)
                            Card(
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .defaultMinSize(minWidth = 50.dp, minHeight = 50.dp),
                                colors = CardDefaults.cardColors(containerColor = cardContainerColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        dimensionResource(
                                            dimen.padding
                                        )
                                    ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (soonestDeparture.realTime) {
                                        val infiniteTransition =
                                            rememberInfiniteTransition(label = "alpha animation")
                                        val animatedAlpha by infiniteTransition.animateFloat(
                                            initialValue = 0.5f,
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
                                            color = onRouteColor.copy(alpha = animatedAlpha),
                                            style = MaterialTheme.typography.headlineSmall
                                        )
                                    }
                                    Text(
                                        text = bestDepartureTime,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (soonestDeparture.realTime) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = onRouteColor.copy(alpha = if (soonestDeparture.realTime) 1f else 0.5f),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
