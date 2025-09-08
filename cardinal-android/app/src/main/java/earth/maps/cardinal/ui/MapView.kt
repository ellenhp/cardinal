package earth.maps.cardinal.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.string
import earth.maps.cardinal.ui.map.LocationPuck
import earth.maps.cardinal.viewmodel.MapViewModel
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.material3.DisappearingCompassButton
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.rememberStyleState

@Composable
fun MapView(
    port: Int,
    mapViewModel: MapViewModel,
    onMapInteraction: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    hasLocationPermission: Boolean,
    targetViewport: CameraPosition? = null,
    fabInsets: PaddingValues
) {
    val context = LocalContext.current
    val cameraState = rememberCameraState()
    val styleState = rememberStyleState()

    // Load saved viewport on initial composition
    LaunchedEffect(Unit) {
        val savedViewport = mapViewModel.loadViewport()
        if (savedViewport != null && targetViewport == null) {
            cameraState.animateTo(savedViewport)
        }
    }

    // Save viewport when composition is disposed
    DisposableEffect(cameraState) {
        onDispose {
            // Save current viewport
            mapViewModel.saveViewport(cameraState.position)
        }
    }

    // Center on target location when provided
    targetViewport?.let { viewport ->
        LaunchedEffect(viewport) {
            cameraState.animateTo(viewport)
        }
    }

    // Start continuous location updates when we have permission
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            mapViewModel.startContinuousLocationUpdates(context)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onMapInteraction() }
    ) {
        // Validate port before using it in URL
        if (port > 0 && port < 65536) {
            MaplibreMap(
                modifier = Modifier.fillMaxSize(),
                cameraState = cameraState,
                baseStyle = BaseStyle.Uri("http://127.0.0.1:$port/style.json"),
                styleState = styleState,
                options = MapOptions(ornamentOptions = OrnamentOptions.AllDisabled)
            ) {
                val location by mapViewModel.locationFlow.collectAsState()
                location?.let { LocationPuck(it) }
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
    context: Context
) {
    val isLocating by mapViewModel.isLocating.collectAsState()
    val hasPendingLocationRequest by mapViewModel.hasPendingLocationRequest.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
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
                    .padding(bottom = dimensionResource(dimen.padding_minor)),
                onClick = {
                    // Request location permissions if we don't have them
                    if (!hasLocationPermission) {
                        mapViewModel.markLocationRequestPending()
                        onRequestLocationPermission()
                    } else {
                        // Also fetch a single location and animate camera to it
                        coroutineScope.launch {
                            mapViewModel.fetchLocationAndCreateCameraPosition(context)
                                ?.let { position ->
                                    cameraState.animateTo(position)
                                }
                        }
                    }
                }
            ) {
                if (isLocating || hasPendingLocationRequest) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = stringResource(string.locate_me_content_description)
                    )
                }
            }
        }
    }
}
