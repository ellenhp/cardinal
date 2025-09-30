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

package earth.maps.cardinal.ui.core

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
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.PolylineUtils
import earth.maps.cardinal.data.RoutingMode
import earth.maps.cardinal.routing.RouteRepository
import earth.maps.cardinal.ui.directions.DirectionsScreen
import earth.maps.cardinal.ui.directions.DirectionsViewModel
import earth.maps.cardinal.ui.directions.RouteDisplayHandler
import earth.maps.cardinal.ui.directions.TurnByTurnNavigationScreen
import earth.maps.cardinal.ui.home.HomeScreen
import earth.maps.cardinal.ui.home.HomeViewModel
import earth.maps.cardinal.ui.home.NearbyScreenContent
import earth.maps.cardinal.ui.home.NearbyViewModel
import earth.maps.cardinal.ui.home.OfflineAreasScreen
import earth.maps.cardinal.ui.home.OfflineAreasViewModel
import earth.maps.cardinal.ui.home.TransitScreenContent
import earth.maps.cardinal.ui.home.TransitScreenViewModel
import earth.maps.cardinal.ui.place.PlaceCardScreen
import earth.maps.cardinal.ui.place.PlaceCardViewModel
import earth.maps.cardinal.ui.saved.ManagePlacesScreen
import earth.maps.cardinal.ui.settings.AccessibilitySettingsScreen
import earth.maps.cardinal.ui.settings.AdvancedSettingsScreen
import earth.maps.cardinal.ui.settings.PrivacySettingsScreen
import earth.maps.cardinal.ui.settings.ProfileEditorScreen
import earth.maps.cardinal.ui.settings.RoutingProfilesScreen
import earth.maps.cardinal.ui.settings.SettingsScreen
import earth.maps.cardinal.ui.settings.SettingsViewModel
import io.github.dellisd.spatialk.geojson.BoundingBox
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState

val TOOLBAR_HEIGHT_DP = 64.dp

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(
    navController: NavHostController,
    mapViewModel: MapViewModel,
    port: Int?,
    onRequestLocationPermission: () -> Unit,
    hasLocationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    hasNotificationPermission: Boolean,
    routeRepository: RouteRepository,
    appPreferenceRepository: AppPreferenceRepository,
    state: AppContentState = rememberAppContentState(),
) {

    val homeViewModel: HomeViewModel = hiltViewModel()
    val transitViewModel: TransitScreenViewModel = hiltViewModel()
    val nearbyViewModel: NearbyViewModel = hiltViewModel()

    val droppedPinName = stringResource(string.dropped_pin)

    // This is used by nav destinations to determine if it is appropriate of them to update peekHeight.
    val topOfBackStack by navController.currentBackStackEntryAsState()

    // See comment below in onGloballyPositioned for why this is necessary. I'm not happy about it either.
    LaunchedEffect(state.peekHeight) {
        mapViewModel.peekHeight = state.peekHeight
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                state.screenHeightDp = with(state.density) { it.size.height.toDp() }
                state.screenWidthDp = with(state.density) { it.size.width.toDp() }
                // For very annoying reasons, this ViewModel needs to know the size of the screen.
                // Specifically, it is responsible for tracking the state of the "locate me" button across
                // a permission request lifecycle. When the permission request is done, it has zero
                // business calling back into the view to perform the animateTo operation, and in order
                // to perform the animateTo you need to calculate padding based on screen size and peek
                // height. :(
                mapViewModel.screenWidth = state.screenWidthDp
                mapViewModel.screenHeight = state.screenHeightDp
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
                    start = 0.dp,
                    top = 0.dp,
                    end = 0.dp,
                    bottom = if (state.screenHeightDp > state.fabHeight) {
                        state.screenHeightDp - state.fabHeight
                    } else {
                        0.dp
                    }
                ),
                cameraState = state.cameraState,
                mapPins = state.mapPins,
                appPreferences = appPreferenceRepository,
                selectedOfflineArea = state.selectedOfflineArea,
                currentRoute = state.currentRoute,
                currentTransitItinerary = state.currentTransitItinerary
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
            state.showToolbar = true
            HomeScreenComposable(
                viewModel = homeViewModel,
                cameraState = state.cameraState,
                mapPins = state.mapPins,
                peekHeight = state.peekHeight,
                navController = navController,
                onPeekHeightChange = {
                    state.peekHeight = it
                },
                onFabHeightChange = {
                    state.fabHeight = it
                },
                topOfBackStack = topOfBackStack,
                backStackEntry = backStackEntry,
                screenWidthDp = state.screenWidthDp,
                screenHeightDp = state.screenHeightDp,
                appPreferenceRepository = appPreferenceRepository
            )
        }

        composable(
            Screen.NEARBY_POI,
            enterTransition = { slideInVertically(initialOffsetY = { it }) },
            exitTransition = { fadeOut(animationSpec = tween(600)) }) { backStackEntry ->
            state.showToolbar = true

            val bottomSheetState = rememberBottomSheetState(
                initialValue = BottomSheetValue.Collapsed
            )
            val scaffoldState =
                rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

            CardinalAppScaffold(
                scaffoldState = scaffoldState, peekHeight = state.screenHeightDp / 3,
                content = {
                    NearbyScreenContent(viewModel = nearbyViewModel, onPlaceSelected = {
                        NavigationUtils.navigate(navController, Screen.PlaceCard(it))
                    })
                },
                fabHeightCallback = {
                    if (topOfBackStack == backStackEntry) {
                        state.fabHeight = it
                    }
                },
            )

        }

        composable(
            Screen.NEARBY_TRANSIT,
            enterTransition = { slideInVertically(initialOffsetY = { it }) },
            exitTransition = { fadeOut(animationSpec = tween(600)) }) { backStackEntry ->
            state.showToolbar = true

            val bottomSheetState = rememberBottomSheetState(
                initialValue = BottomSheetValue.Collapsed
            )
            val scaffoldState =
                rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

            CardinalAppScaffold(
                scaffoldState = scaffoldState, peekHeight = state.screenHeightDp / 3,
                content = {
                    TransitScreenContent(viewModel = transitViewModel, onRouteClicked = {
                        NavigationUtils.navigate(navController, Screen.PlaceCard(it))
                    })
                },
                fabHeightCallback = {
                    if (topOfBackStack == backStackEntry) {
                        state.fabHeight = it
                    }
                },
            )
        }

        composable(
            Screen.PLACE_CARD,
            enterTransition = { slideInVertically(initialOffsetY = { it }) },
            exitTransition = { fadeOut(animationSpec = tween(600)) }) { backStackEntry ->
            state.showToolbar = false

            val bottomSheetState = rememberBottomSheetState(
                initialValue = BottomSheetValue.Collapsed
            )
            val scaffoldState =
                rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

            LaunchedEffect(key1 = Unit) {
                // The place card starts partially expanded.
                state.coroutineScope.launch {
                    scaffoldState.bottomSheetState.collapse()
                }
            }

            val viewModel: PlaceCardViewModel = hiltViewModel()
            val placeJson = backStackEntry.arguments?.getString("place")
            val place = placeJson?.let { Gson().fromJson(it, Place::class.java) }
            place?.let { place ->
                viewModel.setPlace(place)
                LaunchedEffect(place) {
                    // Clear any existing pins and add the new one to ensure only one pin is shown at a time
                    state.mapPins.clear()
                    state.mapPins.add(place)

                    val previousBackStackEntry = navController.previousBackStackEntry
                    val shouldFlyToPoi =
                        previousBackStackEntry?.destination?.route?.startsWith(Screen.DIRECTIONS) != true

                    // Only animate if we're entering from the home screen, as opposed to e.g. popping from the
                    // settings screen. This is brittle and may break if we end up with more entry points.
                    if (shouldFlyToPoi) {
                        state.coroutineScope.launch {
                            state.cameraState.animateTo(
                                CameraPosition(
                                    target = Position(
                                        latitude = place.latLng.latitude,
                                        longitude = place.latLng.longitude
                                    ), zoom = 15.0, padding = PaddingValues(
                                        start = state.screenWidthDp / 8,
                                        top = state.screenHeightDp / 8,
                                        end = state.screenWidthDp / 8,
                                        bottom = min(
                                            3f * state.screenHeightDp / 4,
                                            state.peekHeight + state.screenHeightDp / 8
                                        )
                                    )
                                ),
                                duration = appPreferenceRepository.animationSpeedDurationValue,
                            )
                        }
                    }
                }

                CardinalAppScaffold(
                    scaffoldState = scaffoldState, peekHeight = state.peekHeight,
                    content = {
                        PlaceCardScreen(place = place, viewModel = viewModel, onBack = {
                            navController.popBackStack()
                        }, onGetDirections = { place ->
                            NavigationUtils.navigate(
                                navController, Screen.Directions(fromPlace = null, toPlace = place)
                            )
                        }, onPeekHeightChange = {
                            if (topOfBackStack == backStackEntry) {
                                state.peekHeight = it
                            }
                        })
                    },
                    showToolbar = false,
                    fabHeightCallback = {
                        if (topOfBackStack == backStackEntry) {
                            state.fabHeight = it
                        }
                    },
                )
            }
        }

        composable(
            Screen.OFFLINE_AREAS,
            enterTransition = { slideInVertically(initialOffsetY = { it }) },
            exitTransition = { fadeOut(animationSpec = tween(600)) }) { backStackEntry ->
            state.showToolbar = true
            val bottomSheetState = rememberBottomSheetState(
                initialValue = BottomSheetValue.Collapsed
            )
            val scaffoldState =
                rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

            LaunchedEffect(key1 = Unit) {
                state.mapPins.clear()
                state.peekHeight = state.screenHeightDp / 3 // Approx, empirical
                state.coroutineScope.launch {
                    scaffoldState.bottomSheetState.collapse()
                }
            }
            DisposableEffect(key1 = Unit) {
                onDispose {
                    state.selectedOfflineArea = null
                }
            }
            val viewModel: OfflineAreasViewModel = hiltViewModel()
            val snackBarHostState = remember { SnackbarHostState() }

            // Track the current viewport reactively
            var currentViewport by remember { mutableStateOf(state.cameraState.projection?.queryVisibleRegion()) }

            // Update viewport when camera state changes
            LaunchedEffect(state.cameraState.position) {
                currentViewport = state.cameraState.projection?.queryVisibleRegion()
            }

            currentViewport?.let { visibleRegion ->
                CardinalAppScaffold(
                    scaffoldState = scaffoldState,
                    peekHeight = state.peekHeight,
                    fabHeightCallback = {
                        if (topOfBackStack == backStackEntry) {
                            state.fabHeight = it
                        }
                    },
                    content = {
                        OfflineAreasScreen(
                            currentViewport = visibleRegion,
                            currentZoom = state.cameraState.position.zoom,
                            viewModel = viewModel,
                            snackBarHostState = snackBarHostState,
                            onDismiss = {
                                navController.popBackStack()
                            },
                            onAreaSelected = { area ->
                                state.coroutineScope.launch {
                                    scaffoldState.bottomSheetState.collapse()
                                    state.cameraState.animateTo(
                                        boundingBox = BoundingBox(
                                            area.west, area.south, area.east, area.north
                                        ),
                                        padding = PaddingValues(
                                            start = state.screenWidthDp / 8,
                                            top = state.screenHeightDp / 8,
                                            end = state.screenWidthDp / 8,
                                            bottom = min(
                                                3f * state.screenHeightDp / 4,
                                                state.peekHeight + state.screenHeightDp / 8
                                            )
                                        ),
                                        duration = appPreferenceRepository.animationSpeedDurationValue
                                    )
                                }
                                state.selectedOfflineArea = area
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
            state.showToolbar = true
            SettingsScreen(
                navController = navController,
                viewModel = hiltViewModel(),
            )
        }

        composable(
            Screen.OFFLINE_SETTINGS,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
        ) {
            state.showToolbar = true
            val viewModel: SettingsViewModel = hiltViewModel()
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

        composable(
            Screen.ACCESSIBILITY_SETTINGS,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
        ) {
            state.showToolbar = true
            val viewModel: SettingsViewModel = hiltViewModel()
            AccessibilitySettingsScreen(
                viewModel = viewModel, onDismiss = { navController.popBackStack() })

        }

        composable(
            Screen.ADVANCED_SETTINGS,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
        ) {
            state.showToolbar = true
            val viewModel: SettingsViewModel = hiltViewModel()
            AdvancedSettingsScreen(
                viewModel = viewModel
            )
        }

        composable(
            Screen.ROUTING_PROFILES,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
        ) {
            state.showToolbar = true
            RoutingProfilesScreen(
                navController = navController
            )
        }

        composable(
            Screen.PROFILE_EDITOR,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
        ) { backStackEntry ->
            LaunchedEffect(key1 = Unit) {
                state.mapPins.clear()
            }

            val snackBarHostState = remember { SnackbarHostState() }

            val profileId = backStackEntry.arguments?.getString("profileId")
            Scaffold(
                snackbarHost = { SnackbarHost(snackBarHostState) },
                contentWindowInsets = WindowInsets.safeDrawing,
                content = { padding ->
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
            state.showToolbar = true

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
            ManagePlacesScreen(
                navController = navController,
                listId = listId,
                parents = parents,
            )
        }

        composable(
            Screen.DIRECTIONS,
            enterTransition = { slideInVertically(initialOffsetY = { it }) },
            exitTransition = { fadeOut(animationSpec = tween(600)) }) { backStackEntry ->
            state.showToolbar = false
            val bottomSheetState =
                rememberBottomSheetState(initialValue = BottomSheetValue.Collapsed)
            val scaffoldState =
                rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

            val viewModel: DirectionsViewModel = hiltViewModel()
            mapViewModel.locationFlow.collectAsState().value

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
                }
                viewModel.updateToPlace(toPlace)
            }

            val polylinePadding = PaddingValues(
                start = state.screenWidthDp / 8,
                top = state.screenHeightDp / 8,
                end = state.screenWidthDp / 8,
                bottom = min(
                    3f * state.screenHeightDp / 4,
                    state.peekHeight + state.screenHeightDp / 8
                )
            )

            // Handle route display and camera animation
            RouteDisplayHandler(
                viewModel = viewModel,
                cameraState = state.cameraState,
                appPreferences = appPreferenceRepository,
                padding = polylinePadding,
                onRouteUpdate = { route -> state.currentRoute = route })
            DisposableEffect(key1 = Unit) {
                onDispose {
                    state.currentRoute = null
                }
            }

            CardinalAppScaffold(
                scaffoldState = scaffoldState, peekHeight = state.peekHeight,
                content = {
                    DirectionsScreen(
                        viewModel = viewModel,
                        onPeekHeightChange = {
                            if (topOfBackStack == backStackEntry) {
                                state.peekHeight = it
                            }
                        },
                        onBack = { navController.popBackStack() },
                        onFullExpansionRequired = {
                            state.coroutineScope.launch {
                                scaffoldState.bottomSheetState.expand()
                            }
                        },
                        navController = navController,
                        hasLocationPermission = hasLocationPermission,
                        onRequestLocationPermission = onRequestLocationPermission,
                        hasNotificationPermission = hasNotificationPermission,
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        appPreferences = appPreferenceRepository
                    )
                },
                showToolbar = false,
                fabHeightCallback = {
                    if (topOfBackStack == backStackEntry) {
                        state.fabHeight = it
                    }
                },
            )
        }

        composable(
            Screen.TRANSIT_ITINERARY_DETAIL,
            enterTransition = { slideInVertically(initialOffsetY = { it }) },
            exitTransition = { fadeOut(animationSpec = tween(600)) }) { backStackEntry ->
            state.showToolbar = false

            val bottomSheetState = rememberBottomSheetState(
                initialValue = BottomSheetValue.Collapsed
            )
            val scaffoldState =
                rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

            LaunchedEffect(key1 = Unit) {
                state.coroutineScope.launch {
                    scaffoldState.bottomSheetState.collapse()
                }
            }

            val itineraryJson = backStackEntry.arguments?.getString("itinerary")
            val itinerary = itineraryJson?.let {
                Gson().fromJson(Uri.decode(it), earth.maps.cardinal.transit.Itinerary::class.java)
            }

            itinerary?.let { itinerary ->
                LaunchedEffect(itinerary) {
                    // Set current transit itinerary for map display
                    state.currentTransitItinerary = itinerary

                    // Extract all leg geometries and calculate bounding box
                    val allPositions = mutableListOf<Position>()

                    itinerary.legs.forEach { leg ->
                        leg.legGeometry?.let { geometry ->
                            try {
                                val positions =
                                    earth.maps.cardinal.data.PolylineUtils.decodePolyline(
                                        geometry.points, geometry.precision
                                    )
                                allPositions.addAll(positions)
                            } catch (e: Exception) {
                                // Ignore decoding errors for individual legs
                            }
                        }
                    }

                    // If we have route geometry, fit camera to the route
                    if (allPositions.isNotEmpty()) {
                        earth.maps.cardinal.data.PolylineUtils.calculateBoundingBox(allPositions)
                            ?.let { boundingBox ->
                                state.coroutineScope.launch {
                                    state.cameraState.animateTo(
                                        boundingBox = BoundingBox(
                                            west = boundingBox.west,
                                            south = boundingBox.south,
                                            east = boundingBox.east,
                                            north = boundingBox.north
                                        ),
                                        padding = PaddingValues(
                                            start = state.screenWidthDp / 8,
                                            top = state.screenHeightDp / 8,
                                            end = state.screenWidthDp / 8,
                                            bottom = min(
                                                3f * state.screenHeightDp / 4,
                                                state.peekHeight + state.screenHeightDp / 8
                                            )
                                        ),
                                        duration = appPreferenceRepository.animationSpeedDurationValue
                                    )
                                }
                            }
                    }

                    // Clear any existing pins
                    state.mapPins.clear()
                }

                DisposableEffect(key1 = Unit) {
                    onDispose {
                        state.currentTransitItinerary = null
                    }
                }

                CardinalAppScaffold(
                    scaffoldState = scaffoldState,
                    peekHeight = state.peekHeight,
                    content = {
                        earth.maps.cardinal.ui.directions.TransitItineraryDetailScreen(
                            itinerary = itinerary, onBack = {
                                navController.popBackStack()
                            }, appPreferences = appPreferenceRepository
                        )
                    },
                    showToolbar = false,
                    fabHeightCallback = {
                        if (topOfBackStack == backStackEntry) {
                            state.fabHeight = it
                        }
                    },
                )
            }
        }

        composable(Screen.TURN_BY_TURN) { backStackEntry ->
            state.showToolbar = false
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
                    port = port,
                    mode = routingMode,
                    route = ferrostarRoute,
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Animated toolbar positioned below the scaffold
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = state.showToolbar,
            enter = slideInVertically(
                initialOffsetY = { it }, animationSpec = tween(300)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it }, animationSpec = tween(300)
            ),
        ) {
            CardinalToolbar(navController, onSearchDoublePress = { homeViewModel.expandSearch() })
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CardinalToolbar(
    navController: NavController, onSearchDoublePress: (() -> Unit)? = null
) {
    FlexibleBottomAppBar {
        IconButton(onClick = {
            NavigationUtils.navigate(
                navController, screen = Screen.ManagePlaces(null), popUpToHome = true
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
                    navController, screen = Screen.HomeSearch, popUpToHome = true
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
                navController, screen = Screen.NearbyTransit, popUpToHome = true
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
                navController, screen = Screen.OfflineAreas, popUpToHome = true
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
    cameraState: CameraState,
    mapPins: SnapshotStateList<Place>,
    peekHeight: Dp,
    navController: NavHostController,
    onPeekHeightChange: (Dp) -> Unit,
    onFabHeightChange: (Dp) -> Unit,
    topOfBackStack: NavBackStackEntry?,
    backStackEntry: NavBackStackEntry,
    screenWidthDp: Dp,
    screenHeightDp: Dp,
    appPreferenceRepository: AppPreferenceRepository,
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
                onSearchFocusChange = {
                    if (it) {
                        viewModel.expandSearch()
                    }
                },
                onResultPinsChange = {
                    mapPins.clear()
                    mapPins.addAll(it)
                },
                onSearchEvent = {
                    viewModel.collapseSearch()
                    coroutineScope.launch {
                        val boundingBox =
                            PolylineUtils.calculateBoundingBox(mapPins.map { it.toPosition() })
                                ?: return@launch
                        cameraState.animateTo(
                            boundingBox = boundingBox.toGeoJsonBoundingBox(),
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
                }
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
            sheetPeekHeight = peekHeight + bottomInset + handleHeight + bottomPadding,
            snackbarHost = { SnackbarHost(snackBarHostState) },
            sheetBackgroundColor = BottomSheetDefaults.ContainerColor,
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
