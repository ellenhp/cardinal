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
import earth.maps.cardinal.data.isYellow
import earth.maps.cardinal.transit.StopTime
import earth.maps.cardinal.viewmodel.TransitScreenViewModel
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

    LaunchedEffect(secondsSinceStart) {
        if (secondsSinceStart % 30 == 0) {
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
                        painter = painterResource(earth.maps.cardinal.R.drawable.ic_refresh),
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
        } else if (isLoading.value && viewModel.departures.value.isEmpty()) {
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
        } else if (viewModel.departures.value.isEmpty()) {
            Text(
                text = stringResource(string.no_upcoming_departures),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            // List of departures grouped by route and headsign
            TransitScreenRouteDepartures(
                stopTimes = viewModel.departures.value,
                onRouteClicked = onRouteClicked,
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun TransitScreenRouteDepartures(
    stopTimes: List<StopTime>, onRouteClicked: (Place) -> Unit
) {
    // Group departures by route name
    val departuresByRoute = stopTimes.groupBy { it.routeShortName }

    departuresByRoute.forEach { (routeName, departures) ->
        val routeColor = departures.firstOrNull()?.routeColor?.let {
            Color("#$it".toColorInt())
        } ?: MaterialTheme.colorScheme.surfaceVariant

        val cardContainerColor = if (routeColor.isYellow()) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            routeColor
        }

        val onRouteColor = cardContainerColor.contrastColor()

        // Group departures by headsign within each route
        val departuresByHeadsign = departures.groupBy { it.headsign }.map { (key, value) ->
            (key to value.take(1))
        }.toMap()
        val headsigns = departuresByHeadsign.keys.toList().sorted()

        val transitStopString = stringResource(string.transit_stop)
        val places = remember {
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
                                val bestDepartureTime = formatDepartureTime(soonestDeparture)
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

                                if (routeColor.isYellow()) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterVertically)
                                            .padding(end = dimensionResource(dimen.padding))
                                            .defaultMinSize(minWidth = 50.dp, minHeight = 50.dp),
                                    ) {
                                        containerContent()
                                    }
                                } else {
                                    Card(
                                        modifier = Modifier
                                            .align(Alignment.CenterVertically)
                                            .padding(end = dimensionResource(dimen.padding))
                                            .defaultMinSize(minWidth = 50.dp, minHeight = 50.dp),
                                        colors = CardDefaults.cardColors(containerColor = cardContainerColor)
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
}
