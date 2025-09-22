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

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.gson.Gson
import earth.maps.cardinal.R
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.drawable
import earth.maps.cardinal.bottomsheet.BottomSheetScaffold
import earth.maps.cardinal.bottomsheet.BottomSheetScaffoldState
import earth.maps.cardinal.bottomsheet.BottomSheetValue
import earth.maps.cardinal.bottomsheet.rememberBottomSheetScaffoldState
import earth.maps.cardinal.bottomsheet.rememberBottomSheetState
import earth.maps.cardinal.data.AppPreferenceRepository
import earth.maps.cardinal.data.LatLng
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.RoutingMode
import earth.maps.cardinal.data.room.OfflineArea
import earth.maps.cardinal.routing.RouteRepository
import earth.maps.cardinal.ui.settings.AccessibilitySettingsScreen
import earth.maps.cardinal.ui.settings.AdvancedSettingsScreen
import earth.maps.cardinal.ui.settings.PrivacySettingsScreen
import earth.maps.cardinal.viewmodel.DirectionsViewModel
import earth.maps.cardinal.viewmodel.HomeViewModel
import earth.maps.cardinal.viewmodel.ManagePlacesViewModel
import earth.maps.cardinal.viewmodel.MapViewModel
import earth.maps.cardinal.viewmodel.OfflineAreasViewModel
import earth.maps.cardinal.viewmodel.PlaceCardViewModel
import earth.maps.cardinal.viewmodel.SettingsViewModel
import io.github.dellisd.spatialk.geojson.BoundingBox
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import uniffi.ferrostar.Route

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(
    navController: NavHostController,
    mapViewModel: MapViewModel,
    port: Int?,
    onRequestLocationPermission: () -> Unit,
    hasLocationPermission: Boolean,
    routeRepository: RouteRepository,
    appPreferenceRepository: AppPreferenceRepository,
) {
    val mapPins = remember { mutableStateListOf<Position>() }
    val cameraState = rememberCameraState()
    var fabHeight by remember { mutableStateOf(0.dp) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var selectedOfflineArea by remember { mutableStateOf<OfflineArea?>(null) }

    // Route state for displaying on map
    var currentRoute by remember { mutableStateOf<Route?>(null) }

    val bottomSheetState = rememberBottomSheetState(
        initialValue = BottomSheetValue.Collapsed
    )
    val droppedPinName = stringResource(R.string.dropped_pin)
    var screenHeightDp by remember { mutableStateOf(0.dp) }
    var screenWidthDp by remember { mutableStateOf(0.dp) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                screenHeightDp = with(density) { it.size.height.toDp() }
                screenWidthDp = with(density) { it.size.width.toDp() }
            },

        ) {
        if (port != null && port != -1) {
            MapView(
                port = port,
                mapViewModel = mapViewModel,
                onMapInteraction = {
                    if (navController.currentBackStackEntry?.destination?.route?.startsWith("place_card") == true) {
                        navController.popBackStack()
                    }
                },
                onMapPoiClick = {
                    NavigationUtils.navigate(navController, Screen.PlaceCard(it))
                },
                onTransitStopClick = {
                    NavigationUtils.navigate(navController, Screen.PlaceCard(it))
                },
                onDropPin = {
                    val place = Place(
                        name = droppedPinName,
                        type = "",
                        icon = "place",
                        latLng = it,
                        address = null,
                        isMyLocation = false
                    )
                    NavigationUtils.navigate(navController, Screen.PlaceCard(place))
                },
                onRequestLocationPermission = onRequestLocationPermission,
                hasLocationPermission = hasLocationPermission,
                fabInsets = PaddingValues(
                    start = 0.dp, top = 0.dp, end = 0.dp, bottom = if (screenHeightDp > fabHeight) {
                        screenHeightDp - fabHeight
                    } else {
                        0.dp
                    }
                ),
                cameraState = cameraState,
                mapPins = mapPins,
                appPreferences = appPreferenceRepository,
                selectedOfflineArea = selectedOfflineArea,
                currentRoute = currentRoute
            )
        } else {
            LaunchedEffect(key1 = port) {
                Log.e("AppContent", "Tileserver port is $port, can't display a map!")
            }
        }

        Box(
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            BirdSettingsFab(navController)
        }
    }

    NavHost(
        navController = navController, startDestination = Screen.HOME
    ) {
        composable(
            Screen.HOME,
            enterTransition = { slideInVertically(initialOffsetY = { it }) },
            exitTransition = { fadeOut(animationSpec = tween(600)) }) {
            var homeInSearchScreen by rememberSaveable { mutableStateOf(false) }
            var peekHeight by remember { mutableStateOf(0.dp) }
            val bottomSheetState = rememberBottomSheetState(
                initialValue = BottomSheetValue.Collapsed
            )
            val scaffoldState =
                rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

            val viewModel: HomeViewModel = hiltViewModel()
            val managePlacesViewModel: ManagePlacesViewModel = hiltViewModel()
            val focusManager = LocalFocusManager.current
            val imeController = LocalSoftwareKeyboardController.current
            if (bottomSheetState.isExpanded) {
                BackHandler {
                    homeInSearchScreen = false
                }
            }

            LaunchedEffect(homeInSearchScreen) {
                if (homeInSearchScreen) {
                    bottomSheetState.expand()
                } else {
                    imeController?.hide()
                    focusManager.clearFocus(force = true)
                    bottomSheetState.collapse()
                }
            }

            LaunchedEffect(Unit) {
                mapPins.clear()
            }

            CardinalScaffold(
                scaffoldState = scaffoldState,
                peekHeight = peekHeight,
                sheetGesturesEnabled = !homeInSearchScreen,
                fabHeightCallback = {
                    fabHeight = it
                },
                content = {
                    HomeScreen(
                        viewModel = viewModel,
                        managePlacesViewModel = managePlacesViewModel,
                        onPlaceSelected = { place ->
                            coroutineScope.launch {
                                imeController?.hide()
                                // Collapse the bottom sheet but don't touch homeExpanded.
                                scaffoldState.bottomSheetState.collapse()
                            }
                            NavigationUtils.navigate(navController, Screen.PlaceCard(place))
                        },
                        onPeekHeightChange = { peekHeight = it },
                        homeInSearchScreen = homeInSearchScreen,
                        onSearchFocusChange = {
                            if (it) {
                                coroutineScope.launch {
                                    bottomSheetState.expand()
                                }
                                homeInSearchScreen = true
                            }
                        })
                })
        }

        composable(
            Screen.PLACE_CARD,
            enterTransition = { slideInVertically(initialOffsetY = { it }) },
            exitTransition = { fadeOut(animationSpec = tween(600)) }) { backStackEntry ->
            var peekHeight by remember { mutableStateOf(0.dp) }
            val bottomSheetState = rememberBottomSheetState(
                initialValue = BottomSheetValue.Collapsed
            )
            val scaffoldState =
                rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

            LaunchedEffect(key1 = Unit) {
                // The place card starts partially expanded.
                coroutineScope.launch {
                    scaffoldState.bottomSheetState.collapse()
                }
            }

            val viewModel: PlaceCardViewModel = hiltViewModel()
            val placeJson = backStackEntry.arguments?.getString("place")
            val place = placeJson?.let { Gson().fromJson(it, Place::class.java) }
            place?.let { place ->
                viewModel.setPlace(place)
                LaunchedEffect(place) {
                    mapPins.clear()
                    val position = Position(place.latLng.longitude, place.latLng.latitude)
                    // Clear any existing pins and add the new one to ensure only one pin is shown at a time
                    mapPins.clear()
                    mapPins.add(position)

                    val previousBackStackEntry = navController.previousBackStackEntry
                    val shouldFlyToPoi =
                        previousBackStackEntry?.destination?.route?.startsWith(Screen.HOME) == true

                    // Only animate if we're entering from the home screen, as opposed to e.g. popping from the
                    // settings screen. This is brittle and may break if we end up with more entry points.
                    if (shouldFlyToPoi) {
                        coroutineScope.launch {
                            cameraState.animateTo(
                                CameraPosition(
                                    target = position, zoom = 15.0
                                ), duration = appPreferenceRepository.animationSpeedDurationValue
                            )
                        }
                    }
                }

                CardinalScaffold(
                    scaffoldState = scaffoldState, peekHeight = peekHeight,
                    content = {
                        PlaceCardScreen(place = place, viewModel = viewModel, onBack = {
                            navController.popBackStack()
                        }, onGetDirections = { place ->
                            NavigationUtils.navigate(
                                navController, Screen.Directions(fromPlace = null, toPlace = place)
                            )
                        }, onPeekHeightChange = { peekHeight = it })
                    },
                    fabHeightCallback = {
                        fabHeight = it
                    },
                )
            }
        }

        composable(
            Screen.OFFLINE_AREAS,
            enterTransition = { slideInVertically(initialOffsetY = { it }) },
            exitTransition = { fadeOut(animationSpec = tween(600)) }) {
            var peekHeight by remember { mutableStateOf(0.dp) }
            val bottomSheetState = rememberBottomSheetState(
                initialValue = BottomSheetValue.Collapsed
            )
            val scaffoldState =
                rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

            LaunchedEffect(key1 = Unit) {
                mapPins.clear()
                peekHeight = screenHeightDp / 3 // Approx, empirical
                coroutineScope.launch {
                    scaffoldState.bottomSheetState.collapse()
                }
            }
            DisposableEffect(key1 = Unit) {
                onDispose {
                    selectedOfflineArea = null
                }
            }
            val viewModel: OfflineAreasViewModel = hiltViewModel()
            val snackBarHostState = remember { SnackbarHostState() }

            // Track the current viewport reactively
            var currentViewport by remember { mutableStateOf(cameraState.projection?.queryVisibleRegion()) }

            // Update viewport when camera state changes
            LaunchedEffect(cameraState.position) {
                currentViewport = cameraState.projection?.queryVisibleRegion()
            }

            currentViewport?.let { visibleRegion ->
                CardinalScaffold(
                    scaffoldState = scaffoldState,
                    peekHeight = peekHeight,
                    fabHeightCallback = {
                        fabHeight = it
                    },
                    content = {
                        OfflineAreasScreen(
                            currentViewport = visibleRegion,
                            currentZoom = cameraState.position.zoom,
                            viewModel = viewModel,
                            snackBarHostState = snackBarHostState,
                            onDismiss = {
                                navController.popBackStack()
                            },
                            onAreaSelected = { area ->
                                coroutineScope.launch {
                                    scaffoldState.bottomSheetState.collapse()
                                    cameraState.animateTo(
                                        boundingBox = BoundingBox(
                                            area.west, area.south, area.east, area.north
                                        ),
                                        padding = PaddingValues(
                                            screenWidthDp / 4,
                                            screenHeightDp / 4,
                                            screenWidthDp / 4,
                                            screenHeightDp / 2
                                        ),
                                        duration = appPreferenceRepository.animationSpeedDurationValue
                                    )
                                }
                                selectedOfflineArea = area
                            })
                    },
                )
            }
        }

        composable(Screen.SETTINGS) {
            LaunchedEffect(key1 = Unit) {
                mapPins.clear()
            }

            val scaffoldState = rememberBottomSheetScaffoldState()

            LaunchedEffect(key1 = Unit) {
                // The settings screen is always fully expanded.
                coroutineScope.launch {
                    scaffoldState.bottomSheetState.expand()
                }
            }

            Scaffold(
                snackbarHost = { SnackbarHost(remember { SnackbarHostState() }) },
                content = { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        SettingsScreen(
                            navController = navController,
                            viewModel = hiltViewModel(),
                        )
                    }
                })
        }

        composable(Screen.OFFLINE_SETTINGS) {
            LaunchedEffect(key1 = Unit) {
                mapPins.clear()
            }

            val scaffoldState = rememberBottomSheetScaffoldState()
            val snackBarHostState = remember { SnackbarHostState() }

            LaunchedEffect(key1 = Unit) {
                // The privacy settings screen is always fully expanded.
                coroutineScope.launch {
                    scaffoldState.bottomSheetState.expand()
                }
            }

            val viewModel: SettingsViewModel = hiltViewModel()
            Scaffold(snackbarHost = { SnackbarHost(snackBarHostState) }, content = { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    PrivacySettingsScreen(
                        viewModel = viewModel,
                        onDismiss = { navController.popBackStack() },
                        onNavigateToOfflineAreas = {
                            NavigationUtils.navigate(
                                navController, Screen.OfflineAreas
                            )
                        },
                    )
                }
            })
        }

        composable(Screen.ACCESSIBILITY_SETTINGS) {
            LaunchedEffect(key1 = Unit) {
                mapPins.clear()
            }

            val scaffoldState = rememberBottomSheetScaffoldState()
            val snackBarHostState = remember { SnackbarHostState() }

            LaunchedEffect(key1 = Unit) {
                // The accessibility settings screen is always fully expanded.
                coroutineScope.launch {
                    scaffoldState.bottomSheetState.expand()
                }
            }

            val viewModel: SettingsViewModel = hiltViewModel()
            Scaffold(snackbarHost = { SnackbarHost(snackBarHostState) }, content = { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    AccessibilitySettingsScreen(
                        viewModel = viewModel, onDismiss = { navController.popBackStack() })
                }
            })
        }

        composable(Screen.ADVANCED_SETTINGS) {
            LaunchedEffect(key1 = Unit) {
                mapPins.clear()
            }

            val scaffoldState = rememberBottomSheetScaffoldState()
            val snackBarHostState = remember { SnackbarHostState() }

            LaunchedEffect(key1 = Unit) {
                // The advanced settings screen is always fully expanded.
                coroutineScope.launch {
                    scaffoldState.bottomSheetState.expand()
                }
            }

            val viewModel: SettingsViewModel = hiltViewModel()
            Scaffold(snackbarHost = { SnackbarHost(snackBarHostState) }, content = { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    AdvancedSettingsScreen(
                        viewModel = viewModel, onDismiss = { navController.popBackStack() })
                }
            })
        }

        composable(Screen.ROUTING_PROFILES) {
            LaunchedEffect(key1 = Unit) {
                mapPins.clear()
                // The routing profiles screen is always fully expanded.
                coroutineScope.launch {
                    bottomSheetState.expand()
                }
            }
            Scaffold(content = { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    RoutingProfilesScreen(
                        navController = navController
                    )
                }
            })
        }

        composable(Screen.PROFILE_EDITOR) { backStackEntry ->
            LaunchedEffect(key1 = Unit) {
                mapPins.clear()
            }

            val scaffoldState = rememberBottomSheetScaffoldState()
            val snackBarHostState = remember { SnackbarHostState() }

            LaunchedEffect(key1 = Unit) {
                // The profile editor screen is always fully expanded.
                coroutineScope.launch {
                    scaffoldState.bottomSheetState.expand()
                }
            }

            val profileId = backStackEntry.arguments?.getString("profileId")
            Scaffold(snackbarHost = { SnackbarHost(snackBarHostState) }, content = { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    ProfileEditorScreen(
                        navController = navController,
                        profileId = profileId,
                        snackBarHostState = snackBarHostState
                    )
                }
            })
        }

        composable(
            Screen.DIRECTIONS,
            enterTransition = { slideInVertically(initialOffsetY = { it }) },
            exitTransition = { fadeOut(animationSpec = tween(600)) }) { backStackEntry ->
            var peekHeight by remember { mutableStateOf(0.dp) }
            val bottomSheetState = rememberBottomSheetState(
                initialValue = BottomSheetValue.Collapsed
            )
            val scaffoldState =
                rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

            LaunchedEffect(key1 = Unit) {
                // The directions screen starts partially expanded.
                coroutineScope.launch {
                    scaffoldState.bottomSheetState.collapse()
                }
            }

            val viewModel: DirectionsViewModel = hiltViewModel()
            val currentLocation = mapViewModel.locationFlow.collectAsState().value

            // Handle initial place setup
            LaunchedEffect(key1 = Unit) {
                val fromPlaceJson = backStackEntry.arguments?.getString("fromPlace")
                val fromPlace =
                    fromPlaceJson?.let { Gson().fromJson(Uri.decode(it), Place::class.java) }
                val toPlaceJson = backStackEntry.arguments?.getString("toPlace")
                val toPlace =
                    toPlaceJson?.let { Gson().fromJson(Uri.decode(it), Place::class.java) }

                if (fromPlace != null) {
                    viewModel.updateFromPlace(fromPlace)
                } else if (currentLocation != null) {
                    viewModel.updateFromPlace(
                        viewModel.createMyLocationPlace(
                            LatLng(
                                latitude = currentLocation.latitude,
                                longitude = currentLocation.longitude,
                            )
                        )
                    )
                }
                viewModel.updateToPlace(toPlace)
            }

            val polylinePadding = PaddingValues(
                start = screenWidthDp / 4,
                top = screenHeightDp / 4,
                end = screenWidthDp / 4,
                bottom = screenHeightDp / 2
            )

            // Handle route display and camera animation
            RouteDisplayHandler(
                viewModel = viewModel,
                cameraState = cameraState,
                appPreferences = appPreferenceRepository,
                padding = polylinePadding,
                onRouteUpdate = { route -> currentRoute = route })
            DisposableEffect(key1 = Unit) {
                onDispose {
                    currentRoute = null
                }
            }

            CardinalScaffold(
                scaffoldState = scaffoldState, peekHeight = peekHeight,
                content = {
                    DirectionsScreen(
                        viewModel = viewModel,
                        onPeekHeightChange = { peekHeight = it },
                        onBack = { navController.popBackStack() },
                        onFullExpansionRequired = {
                            coroutineScope.launch {
                                scaffoldState.bottomSheetState.expand()
                            }
                        },
                        navController = navController,
                        hasLocationPermission = hasLocationPermission,
                        onRequestLocationPermission = onRequestLocationPermission,
                        appPreferences = appPreferenceRepository
                    )
                },
                fabHeightCallback = {
                    fabHeight = it
                },
            )
        }
        composable(Screen.TURN_BY_TURN) { backStackEntry ->
            val routeId = backStackEntry.arguments?.getString("routeId")
            val routingModeJson = backStackEntry.arguments?.getString("routingMode")

            val ferrostarRoute = routeId?.let {
                try {
                    routeRepository.getRoute(it)
                } catch (_: Exception) {
                    null
                }
            }

            val routingMode = routingModeJson?.let {
                try {
                    Gson().fromJson(it, RoutingMode::class.java)
                } catch (_: Exception) {
                    RoutingMode.AUTO
                }
            } ?: RoutingMode.AUTO

            port?.let { port ->
                TurnByTurnNavigationScreen(
                    port = port, mode = routingMode, route = ferrostarRoute
                )
            }
        }

    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardinalScaffold(
    scaffoldState: BottomSheetScaffoldState,
    peekHeight: Dp,
    content: @Composable () -> Unit,
    fabHeightCallback: (Dp) -> Unit,
    sheetGesturesEnabled: Boolean = true
) {
    val density = LocalDensity.current

    val snackBarHostState = remember { SnackbarHostState() }
    val bottomInset = with(density) {
        WindowInsets.safeContent.getBottom(density).toDp()
    }
    val handleHeight = 48.dp

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetGesturesEnabled = sheetGesturesEnabled,
        sheetPeekHeight = peekHeight + bottomInset + handleHeight,
        snackbarHost = { SnackbarHost(snackBarHostState) },
        sheetBackgroundColor = BottomSheetDefaults.ContainerColor,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned {
                        fabHeightCallback(with(density) { it.positionInRoot().y.toDp() })
                    }) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = handleHeight)
                        .align(Alignment.CenterHorizontally)
                ) {
                    if (sheetGesturesEnabled) {
                        BottomSheetDefaults.DragHandle()
                    }
                }
                content()
            }
            Spacer(modifier = Modifier.height(bottomInset))
        },
        content = {})
}

@Composable
fun BirdSettingsFab(navController: NavController) {
    // Avatar icon button in top left
    Box {
        FloatingActionButton(
            onClick = { NavigationUtils.navigate(navController, Screen.Settings) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(dimensionResource(dimen.padding))
                .size(64.dp)
                .border(
                    width = 4.dp, color = MaterialTheme.colorScheme.surface, shape = CircleShape
                ),
            containerColor = MaterialTheme.colorScheme.surfaceDim,
            shape = CircleShape
        ) {
            Image(
                modifier = Modifier.size(48.dp),
                painter = painterResource(drawable.cardinal_icon),
                contentDescription = "Cardinal Maps Settings",
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(dimensionResource(dimen.padding_minor))
                .size(24.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            )
            Icon(
                modifier = Modifier.padding(4.dp),
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
