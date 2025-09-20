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

import android.content.Context
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.zIndex
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.drawable
import earth.maps.cardinal.R.string
import earth.maps.cardinal.data.AppPreferenceRepository
import earth.maps.cardinal.data.LatLng
import earth.maps.cardinal.data.OfflineArea
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.ui.map.LocationPuck
import earth.maps.cardinal.viewmodel.MapViewModel
import io.github.dellisd.spatialk.geojson.Feature
import io.github.dellisd.spatialk.geojson.FeatureCollection
import io.github.dellisd.spatialk.geojson.LineString
import io.github.dellisd.spatialk.geojson.Point
import io.github.dellisd.spatialk.geojson.Polygon
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.offset
import org.maplibre.compose.expressions.dsl.rgbColor
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.map.RenderOptions
import org.maplibre.compose.material3.DisappearingCompassButton
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.rememberStyleState
import org.maplibre.compose.util.ClickResult
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MapView(
    port: Int,
    mapViewModel: MapViewModel,
    onMapPoiClick: (Place) -> Unit,
    onMapInteraction: () -> Unit,
    onDropPin: (LatLng) -> Unit,
    onRequestLocationPermission: () -> Unit,
    hasLocationPermission: Boolean,
    mapPins: List<Position>,
    fabInsets: PaddingValues,
    cameraState: CameraState,
    appPreferences: AppPreferenceRepository,
    selectedOfflineArea: OfflineArea? = null,
    currentRoute: uniffi.ferrostar.Route? = null,
) {
    val context = LocalContext.current
    val styleState = rememberStyleState()
    val pinFeatures = mapPins.map { Feature(geometry = Point(it)) }
    rememberCoroutineScope()

    val styleVariant = if (isSystemInDarkTheme()) "dark" else "light"

    // Load saved viewport on initial composition
    LaunchedEffect(Unit) {
        val savedViewport = mapViewModel.loadViewport()
        if (savedViewport != null) {
            cameraState.animateTo(savedViewport, duration = 1.milliseconds)
        }
    }

    // Save viewport when composition is disposed
    DisposableEffect(cameraState) {
        onDispose {
            // Save current viewport
            mapViewModel.saveViewport(cameraState.position)
        }
    }

    // Update viewport center when camera position changes
    LaunchedEffect(cameraState.position) {
        mapViewModel.updateViewportCenter(cameraState.position)
    }

    // Start continuous location updates when we have permission
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            mapViewModel.startContinuousLocationUpdates(context)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Validate port before using it in URL
        if (port > 0 && port < 65536) {
            MaplibreMap(
                modifier = Modifier.fillMaxSize(),
                cameraState = cameraState,
                baseStyle = BaseStyle.Uri("http://127.0.0.1:$port/style_$styleVariant.json"),
                styleState = styleState,
                options = MapOptions(
                    ornamentOptions = OrnamentOptions.AllDisabled, renderOptions = RenderOptions()
                ),
                onMapClick = { position, dpOffset ->
                    Log.d("MapView", "Logged tap at location $position")
                    mapViewModel.handleMapTap(
                        cameraState, dpOffset, onMapPoiClick, onMapInteraction
                    )
                    ClickResult.Consume
                },
                onMapLongClick = { position, dpOffset ->
                    onDropPin(LatLng(position.latitude, position.longitude))
                    ClickResult.Consume
                }) {
                val location by mapViewModel.locationFlow.collectAsState()
                val savedPlaces by mapViewModel.savedPlacesFlow.collectAsState(FeatureCollection())
                location?.let { LocationPuck(it) }

                // Show user favorites
                val textColor = MaterialTheme.colorScheme.onSurface
                SymbolLayer(
                    id = "user_favorites",
                    source = rememberGeoJsonSource(GeoJsonData.Features(savedPlaces)),
                    iconImage = image(
                        if (isSystemInDarkTheme()) {
                            painterResource(drawable.ic_stars_dark)
                        } else {
                            painterResource(drawable.ic_stars_light)
                        }
                    ),
                    iconSize = const(0.8f),
                    textField = org.maplibre.compose.expressions.dsl.Feature["name"].cast(),
                    textSize = const(0.8.em),
                    textColor = rgbColor(
                        const((textColor.red * 255.0f).toInt()),
                        const((textColor.green * 255.0f).toInt()),
                        const((textColor.blue * 255.0f).toInt()),
                    ),
                    textAnchor = const(SymbolAnchor.Top),
                    textOffset = offset(0.em, 0.8.em),
                    textOptional = const(true),
                )

                // Show offline download bounds if an area is selected
                selectedOfflineArea?.let { area ->
                    val boundsPolygon = Polygon(
                        listOf(
                            listOf(
                                Position(area.west, area.north),  // Northwest
                                Position(area.east, area.north),  // Northeast
                                Position(area.east, area.south),  // Southeast
                                Position(area.west, area.south),  // Southwest
                                Position(area.west, area.north)   // Close the polygon
                            )
                        )
                    )
                    val boundsFeature = Feature(geometry = boundsPolygon)
                    val offlineDownloadBoundsSource = rememberGeoJsonSource(
                        GeoJsonData.Features(FeatureCollection(features = listOf(boundsFeature)))
                    )

                    val color = MaterialTheme.colorScheme.onSurface
                    LineLayer(
                        id = "offline_download_bounds",
                        source = offlineDownloadBoundsSource,
                        color = rgbColor(
                            const((color.red * 255).toInt()),
                            const((color.green * 255).toInt()),
                            const((color.blue * 255).toInt())
                        ),
                        width = const(3.dp)
                    )
                }

                // Display route if available
                currentRoute?.let { route ->
                    val routePositions = route.geometry.map { coord ->
                        Position(coord.lng, coord.lat) // [longitude, latitude]
                    }
                    val routeLineString = LineString(routePositions)
                    val routeFeature = Feature(geometry = routeLineString)
                    val routeSource = rememberGeoJsonSource(
                        GeoJsonData.Features(FeatureCollection(features = listOf(routeFeature)))
                    )

                    val polylineColor = colorResource(earth.maps.cardinal.R.color.polyline_color)
                    val polylineCasingColor =
                        colorResource(earth.maps.cardinal.R.color.polyline_casing_color)

                    LineLayer(
                        id = "route_line_casing", source = routeSource,
                        color = rgbColor(
                            const((polylineCasingColor.red * 255.0).toInt()), // Blue color
                            const((polylineCasingColor.green * 255.0).toInt()),
                            const((polylineCasingColor.blue * 255.0).toInt())
                        ),
                        width = const(8.dp),
                        opacity = const(1f),
                        cap = const(LineCap.Round),
                        join = const(LineJoin.Round),
                    )
                    LineLayer(
                        id = "route_line", source = routeSource,
                        color = rgbColor(
                            const((polylineColor.red * 255.0).toInt()), // Blue color
                            const((polylineColor.green * 255.0).toInt()),
                            const((polylineColor.blue * 255.0).toInt())
                        ),
                        width = const(6.dp),
                        opacity = const(1f),
                        cap = const(LineCap.Round),
                        join = const(LineJoin.Round),
                    )
                }

                SymbolLayer(
                    id = "map_pins",
                    source = rememberGeoJsonSource(GeoJsonData.Features(FeatureCollection(features = pinFeatures))),
                    iconImage = image(
                        if (isSystemInDarkTheme()) {
                            painterResource(drawable.map_pin_dark)
                        } else {
                            painterResource(drawable.map_pin_light)
                        }
                    ),
                )
            }
        } else {
            // Handle invalid port - could show an error message
            Box(modifier = Modifier.fillMaxSize()) {
                // Error UI could be added here
            }
        }

        // Handle permission state changes
        LaunchedEffect(hasLocationPermission) {
            mapViewModel.handlePermissionStateChange(hasLocationPermission, cameraState, context)
        }

        MapControls(
            cameraState = cameraState,
            mapViewModel = mapViewModel,
            fabInsets = fabInsets,
            hasLocationPermission = hasLocationPermission,
            onRequestLocationPermission = onRequestLocationPermission,
            appPreferences = appPreferences,
            context = context
        )
    }
}

@Composable
private fun MapControls(
    cameraState: CameraState,
    mapViewModel: MapViewModel,
    fabInsets: PaddingValues,
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    appPreferences: AppPreferenceRepository,
    context: Context
) {
    val isLocating by mapViewModel.isLocating.collectAsState()
    val hasPendingLocationRequest by mapViewModel.hasPendingLocationRequest.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100000f)
            .padding(fabInsets)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = dimensionResource(dimen.padding_minor))
        ) {
            DisappearingCompassButton(
                cameraState,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = dimensionResource(dimen.padding_minor) / 2),
            )
            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = dimensionResource(dimen.padding_minor)), onClick = {
                    // Request location permissions if we don't have them
                    if (!hasLocationPermission) {
                        mapViewModel.markLocationRequestPending()
                        onRequestLocationPermission()
                    } else {
                        // Also fetch a single location and animate camera to it
                        coroutineScope.launch {
                            mapViewModel.fetchLocationAndCreateCameraPosition(context)
                                ?.let { position ->
                                    cameraState.animateTo(
                                        position,
                                        duration = appPreferences.animationSpeedDurationValue
                                    )
                                }
                        }
                    }
                }) {
                if (isLocating || hasPendingLocationRequest) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        painter = painterResource(drawable.my_location),
                        contentDescription = stringResource(string.locate_me_content_description)
                    )
                }
            }
        }
    }
}
