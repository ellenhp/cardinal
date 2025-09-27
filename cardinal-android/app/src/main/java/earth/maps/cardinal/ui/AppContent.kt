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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
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
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.times
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.drawable
import earth.maps.cardinal.R.string
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
import earth.maps.cardinal.viewmodel.MapViewModel
import earth.maps.cardinal.viewmodel.NearbyViewModel
import earth.maps.cardinal.viewmodel.OfflineAreasViewModel
import earth.maps.cardinal.viewmodel.PlaceCardViewModel
import earth.maps.cardinal.viewmodel.SettingsViewModel
import earth.maps.cardinal.viewmodel.TransitScreenViewModel
import io.github.dellisd.spatialk.geojson.BoundingBox
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import uniffi.ferrostar.Route

private val TOOLBAR_HEIGHT_DP = 56.dp

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

    var showToolbar by remember { mutableStateOf(true) }

    // Route state for displaying on map
    var currentRoute by remember { mutableStateOf<Route?>(null) }

    val bottomSheetState = rememberBottomSheetState(
        initialValue = BottomSheetValue.Collapsed
    )
    val droppedPinName = stringResource(string.dropped_pin)
    var screenHeightDp by remember { mutableStateOf(0.dp) }
    var screenWidthDp by remember { mutableStateOf(0.dp) }
    var peekHeight by remember { mutableStateOf(0.dp) }

    val homeViewModel: HomeViewModel = hiltViewModel()
    val transitViewModel: TransitScreenViewModel = hiltViewModel()
    val nearbyViewModel: NearbyViewModel = hiltViewModel()

    // This is used by nav destinations to determine if it is appropriate of them to update peekHeight.
    val topOfBackStack by navController.currentBackStackEntryAsState()

    // See comment below in onGloballyPositioned for why this is necessary. I'm not happy about it either.
    LaunchedEffect(peekHeight) {
        mapViewModel.peekHeight = peekHeight
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                screenHeightDp = with(density) { it.size.height.toDp() }
                screenWidthDp = with(density) { it.size.width.toDp() }
                // For very annoying reasons, this ViewModel needs to know the size of the screen.
                // Specifically, it is responsible for tracking the state of the "locate me" button across
                // a permission request lifecycle. When the permission request is done, it has zero
                // business calling back into the view to perform the animateTo operation, and in order
                // to perform the animateTo you need to calculate padding based on screen size and peek
                // height. :(
                mapViewModel.screenWidth = screenWidthDp
                mapViewModel.screenHeight = screenHeightDp
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
                        description = "",
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
        navController = navController, startDestination = Screen.HOME_SEARCH
    ) {
        composable(
            Screen.HOME_SEARCH,
            enterTransition = { slideInVertically(initialOffsetY = { it }) },
            exitTransition = { fadeOut(animationSpec = tween(600)) }) { backStackEntry ->
            showToolbar = true
            HomeScreenComposable(
                viewModel = homeViewModel,
                mapPins = mapPins,
                peekHeight = peekHeight,
                navController = navController,
                onPeekHeightChange = {
                    peekHeight = it
                },
                onFabHeightChange = {
                    fabHeight = it
                },
                topOfBackStack = topOfBackStack,
                backStackEntry = backStackEntry,
            )
        }

        composable(
            Screen.NEARBY_POI,
            enterTransition = { slideInVertically(initialOffsetY = { it }) },
            exitTransition = { fadeOut(animationSpec = tween(600)) }) { backStackEntry ->
            showToolbar = true

            val bottomSheetState = rememberBottomSheetState(
                initialValue = BottomSheetValue.Collapsed
            )
            val scaffoldState =
                rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

            CardinalAppScaffold(
                scaffoldState = scaffoldState, peekHeight = screenHeightDp / 3,
                content = {
                    NearbyScreenContent(viewModel = nearbyViewModel, onPlaceSelected = {
                        NavigationUtils.navigate(navController, Screen.PlaceCard(it))
                    })
                },
                fabHeightCallback = {
                    if (topOfBackStack == backStackEntry) {
                        fabHeight = it
                    }
                },
            )

        }

        composable(
            Screen.NEARBY_TRANSIT,
            enterTransition = { slideInVertically(initialOffsetY = { it }) },
            exitTransition = { fadeOut(animationSpec = tween(600)) }) { backStackEntry ->
            showToolbar = true

            val bottomSheetState = rememberBottomSheetState(
                initialValue = BottomSheetValue.Collapsed
            )
            val scaffoldState =
                rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

            CardinalAppScaffold(
                scaffoldState = scaffoldState, peekHeight = screenHeightDp / 3,
                content = {
                    TransitScreenContent(viewModel = transitViewModel, onRouteClicked = {
                        NavigationUtils.navigate(navController, Screen.PlaceCard(it))
                    })
                },
                fabHeightCallback = {
                    if (topOfBackStack == backStackEntry) {
                        fabHeight = it
                    }
                },
            )
        }

        composable(
            Screen.PLACE_CARD,
            enterTransition = { slideInVertically(initialOffsetY = { it }) },
            exitTransition = { fadeOut(animationSpec = tween(600)) }) { backStackEntry ->
            showToolbar = false

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
                        previousBackStackEntry?.destination?.route?.startsWith(Screen.DIRECTIONS) != true

                    // Only animate if we're entering from the home screen, as opposed to e.g. popping from the
                    // settings screen. This is brittle and may break if we end up with more entry points.
                    if (shouldFlyToPoi) {
                        coroutineScope.launch {
                            cameraState.animateTo(
                                CameraPosition(
                                    target = position, zoom = 15.0, padding = PaddingValues(
                                        start = screenWidthDp / 8,
                                        top = screenHeightDp / 8,
                                        end = screenWidthDp / 8,
                                        bottom = min(
                                            3f * screenHeightDp / 4, peekHeight + screenHeightDp / 8
                                        )
                                    )
                                ),
                                duration = appPreferenceRepository.animationSpeedDurationValue,
                            )
                        }
                    }
                }

                CardinalAppScaffold(
                    scaffoldState = scaffoldState, peekHeight = peekHeight,
                    content = {
                        PlaceCardScreen(place = place, viewModel = viewModel, onBack = {
                            navController.popBackStack()
                        }, onGetDirections = { place ->
                            NavigationUtils.navigate(
                                navController, Screen.Directions(fromPlace = null, toPlace = place)
                            )
                        }, onPeekHeightChange = {
                            if (topOfBackStack == backStackEntry) {
                                peekHeight = it
                            }
                        })
                    },
                    showToolbar = false,
                    fabHeightCallback = {
                        if (topOfBackStack == backStackEntry) {
                            fabHeight = it
                        }
                    },
                )
            }
        }

        composable(
            Screen.OFFLINE_AREAS,
            enterTransition = { slideInVertically(initialOffsetY = { it }) },
            exitTransition = { fadeOut(animationSpec = tween(600)) }) { backStackEntry ->
            showToolbar = true
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
                CardinalAppScaffold(
                    scaffoldState = scaffoldState,
                    peekHeight = peekHeight,
                    fabHeightCallback = {
                        if (topOfBackStack == backStackEntry) {
                            fabHeight = it
                        }
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
                                            start = screenWidthDp / 8,
                                            top = screenHeightDp / 8,
                                            end = screenWidthDp / 8,
                                            bottom = min(
                                                3f * screenHeightDp / 4,
                                                peekHeight + screenHeightDp / 8
                                            )
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

        composable(
            Screen.SETTINGS,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
        ) {
            showToolbar = true
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

        composable(
            Screen.OFFLINE_SETTINGS,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
        ) {
            showToolbar = true
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

        composable(
            Screen.ACCESSIBILITY_SETTINGS,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
        ) {
            showToolbar = true
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

        composable(
            Screen.ADVANCED_SETTINGS,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
        ) {
            showToolbar = true
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

        composable(
            Screen.ROUTING_PROFILES,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
        ) {
            showToolbar = true
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

        composable(
            Screen.PROFILE_EDITOR,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
        ) { backStackEntry ->
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
            Screen.MANAGE_PLACES,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
        ) { backStackEntry ->
            showToolbar = true
            LaunchedEffect(key1 = Unit) {
                mapPins.clear()
            }

            val snackBarHostState = remember { SnackbarHostState() }

            val listIdRaw = backStackEntry.arguments?.getString("listId")
            // The screen is set up to take a real value, or null. What we end up with at this point (sometimes?)
            // is an empty string instead of null.
            val listId = if (listIdRaw.isNullOrBlank()) {
                null
            } else {
                listIdRaw
            }
            val parentsGson = backStackEntry.arguments?.getString("parents")?.let { Uri.decode(it) }
            val parents: List<String> =
                parentsGson?.let { Gson().fromJson(it, object : TypeToken<List<String>>() {}.type) }
                    ?: emptyList()
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackBarHostState) },
                content = { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        ManagePlacesScreen(
                            paddingValues = PaddingValues(bottom = TOOLBAR_HEIGHT_DP),
                            navController = navController,
                            listId = listId,
                            parents = parents,
                        )
                    }
                })
        }

        composable(
            Screen.DIRECTIONS,
            enterTransition = { slideInVertically(initialOffsetY = { it }) },
            exitTransition = { fadeOut(animationSpec = tween(600)) }) { backStackEntry ->
            showToolbar = false
            val bottomSheetState =
                rememberBottomSheetState(initialValue = BottomSheetValue.Collapsed)
            val scaffoldState =
                rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

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
                start = screenWidthDp / 8,
                top = screenHeightDp / 8,
                end = screenWidthDp / 8,
                bottom = min(3f * screenHeightDp / 4, peekHeight + screenHeightDp / 8)
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

            CardinalAppScaffold(
                scaffoldState = scaffoldState, peekHeight = peekHeight,
                content = {
                    DirectionsScreen(
                        viewModel = viewModel,
                        onPeekHeightChange = {
                            if (topOfBackStack == backStackEntry) {
                                peekHeight = it
                            }
                        },
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
                showToolbar = false,
                fabHeightCallback = {
                    if (topOfBackStack == backStackEntry) {
                        fabHeight = it
                    }
                },
            )
        }

        composable(Screen.TURN_BY_TURN) { backStackEntry ->
            showToolbar = false
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

    Box(modifier = Modifier.fillMaxSize()) {
        // Animated toolbar positioned below the scaffold
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = showToolbar,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            ),
        ) {
            CardinalToolbar(navController, onSearchDoublePress = { homeViewModel.expandSearch() })
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CardinalToolbar(
    navController: NavController,
    onSearchDoublePress: (() -> Unit)? = null
) {
    FlexibleBottomAppBar {
        IconButton(onClick = {
            NavigationUtils.navigate(
                navController,
                screen = Screen.ManagePlaces(null),
                popUpToHome = true
            )
        }) {
            Icon(
                painter = painterResource(drawable.ic_star), contentDescription = stringResource(
                    string.favorites_screen
                )
            )
        }
        IconButton(onClick = {
            NavigationUtils.navigate(navController, screen = Screen.NearbyPoi, popUpToHome = true)
        }) {
            Icon(
                painter = painterResource(drawable.ic_nearby),
                contentDescription = stringResource(string.points_of_interest_nearby)
            )
        }
        FilledIconButton(onClick = {
            if (navController.currentDestination?.route == Screen.HOME_SEARCH) {
                onSearchDoublePress?.invoke()
            } else {
                NavigationUtils.navigate(
                    navController,
                    screen = Screen.HomeSearch,
                    popUpToHome = true
                )
            }
        }) {
            Icon(
                painter = painterResource(drawable.ic_search),
                contentDescription = stringResource(string.search)
            )
        }
        IconButton(onClick = {
            NavigationUtils.navigate(
                navController,
                screen = Screen.NearbyTransit,
                popUpToHome = true
            )
        }) {
            Icon(
                painter = painterResource(drawable.ic_bus_railway),
                contentDescription = stringResource(
                    string.public_transportation_nearby
                )
            )
        }
        IconButton(onClick = {
            NavigationUtils.navigate(
                navController,
                screen = Screen.OfflineAreas,
                popUpToHome = true
            )
        }) {
            Icon(
                painter = painterResource(drawable.cloud_download_24dp),
                contentDescription = stringResource(string.directions)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeScreenComposable(
    viewModel: HomeViewModel,
    mapPins: SnapshotStateList<Position>,
    peekHeight: Dp,
    navController: NavHostController,
    onPeekHeightChange: (Dp) -> Unit,
    onFabHeightChange: (Dp) -> Unit,
    topOfBackStack: NavBackStackEntry?,
    backStackEntry: NavBackStackEntry,
) {
    val coroutineScope = rememberCoroutineScope()
    val searchExpanded: Boolean? by viewModel.searchExpanded.collectAsState(null)
    val bottomSheetState = rememberBottomSheetState(
        initialValue = BottomSheetValue.Collapsed
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

    val focusManager = LocalFocusManager.current
    val imeController = LocalSoftwareKeyboardController.current

    if (searchExpanded == true) {
        BackHandler {
            viewModel.collapseSearch()
        }
    }

    LaunchedEffect(searchExpanded) {
        if (searchExpanded == true) {
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

    CardinalAppScaffold(
        scaffoldState = scaffoldState,
        peekHeight = peekHeight,
        fabHeightCallback = {
            if (topOfBackStack == backStackEntry) {
                onFabHeightChange(it)
            }
        },
        content = {
            HomeScreen(
                viewModel = viewModel,
                onPlaceSelected = { place ->
                    imeController?.hide()

                    // We are intentionally not collapsing search here, but we do set the bottom
                    // sheet state to collapsed to prevent jank on popping back to this screen.
                    coroutineScope.launch {
                        // This should happen before we navigate away otherwise we get a race condition
                        // between setting the anchors and collapsing the sheet. Unfortunately, it
                        // doesn't return until the sheet is fully collapsed, so we queue the navigation
                        // after this and hope for the best.
                        bottomSheetState.collapse()
                    }
                    coroutineScope.launch {
                        NavigationUtils.navigate(navController, Screen.PlaceCard(place))
                    }
                },
                onPeekHeightChange = {
                    if (topOfBackStack == backStackEntry) {
                        onPeekHeightChange(it)
                    }
                },
                homeInSearchScreen = searchExpanded == true,
                onSearchFocusChange = {
                    if (it) {
                        viewModel.expandSearch()
                    }
                },
            )
        })
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CardinalAppScaffold(
    scaffoldState: BottomSheetScaffoldState,
    peekHeight: Dp,
    content: @Composable () -> Unit,
    fabHeightCallback: (Dp) -> Unit,
    appBar: (@Composable () -> Unit)? = null,
    showToolbar: Boolean = true,
) {
    val density = LocalDensity.current
    val snackBarHostState = remember { SnackbarHostState() }
    val bottomInset = with(density) {
        WindowInsets.safeContent.getBottom(density).toDp()
    }
    val handleHeight = 48.dp

    // If toolbar is visible, add padding for it below the scaffold
    val bottomPadding = if (showToolbar) TOOLBAR_HEIGHT_DP else 0.dp

    Box(modifier = Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = peekHeight + bottomInset + handleHeight,
            bottomPadding = bottomPadding,
            snackbarHost = { SnackbarHost(snackBarHostState) },
            sheetBackgroundColor = BottomSheetDefaults.ContainerColor,
            topBar = appBar,
            sheetContent = {
                Column(
                    modifier = Modifier.onGloballyPositioned {
                        fabHeightCallback(with(density) { it.positionInRoot().y.toDp() })
                    }) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = handleHeight)
                            .align(Alignment.CenterHorizontally)
                    ) {
                        BottomSheetDefaults.DragHandle()
                    }
                    content()
                }
                Spacer(modifier = Modifier.height(bottomInset))
            },
            content = {})
    }

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
                painter = painterResource(drawable.ic_settings),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
