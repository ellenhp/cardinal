package earth.maps.cardinal.ui

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.gson.Gson
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.data.AppPreferenceRepository
import earth.maps.cardinal.data.OfflineArea
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.viewmodel.HomeViewModel
import earth.maps.cardinal.viewmodel.ManagePlacesViewModel
import earth.maps.cardinal.viewmodel.MapViewModel
import earth.maps.cardinal.viewmodel.OfflineAreasViewModel
import earth.maps.cardinal.viewmodel.PlaceCardViewModel
import io.github.dellisd.spatialk.geojson.BoundingBox
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(
    navController: NavHostController,
    mapViewModel: MapViewModel,
    port: Int?,
    onRequestLocationPermission: () -> Unit,
    hasLocationPermission: Boolean,
    appPreferenceRepository: AppPreferenceRepository
) {
    val mapPins = remember { mutableStateListOf<Position>() }
    val cameraState = rememberCameraState()
    var peekHeight by remember { mutableStateOf(0.dp) }
    var fabHeight by remember { mutableStateOf(0.dp) }
    var sheetSwipeEnabled by remember { mutableStateOf(true) }
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var selectedOfflineArea by remember { mutableStateOf<OfflineArea?>(null) }

    val sheetPeekHeightEmpirical = dimensionResource(dimen.empirical_bottom_sheet_handle_height)

    var allowPartialExpansion by remember { mutableStateOf(true) }
    val bottomSheetState =
        rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            confirmValueChange = { newState ->
                when (newState) {
                    SheetValue.Hidden -> false // Always false!
                    SheetValue.Expanded -> true // Always true!
                    SheetValue.PartiallyExpanded -> allowPartialExpansion // Allow composables to control this
                }
            })
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState)

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetSwipeEnabled = sheetSwipeEnabled,
        modifier = Modifier,
        sheetPeekHeight = peekHeight + sheetPeekHeightEmpirical,
        sheetContent = {
            Box(modifier = Modifier.onGloballyPositioned {
                fabHeight = with(density) { it.positionOnScreen().y.toDp() - 50.dp }
            }) {
                NavHost(
                    navController = navController, startDestination = "home"
                ) {
                    composable("home") {
                        LaunchedEffect(key1 = Unit) {
                            // The home screen starts partially expanded.
                            coroutineScope.launch {
                                bottomSheetState.partialExpand()
                            }
                        }
                        val viewModel: HomeViewModel = hiltViewModel()
                        val managePlacesViewModel: ManagePlacesViewModel = hiltViewModel()
                        var isSearchFocused by remember { mutableStateOf(false) }
                        val focusManager = LocalFocusManager.current
                        if (isSearchFocused) {
                            BackHandler {
                                focusManager.clearFocus()
                            }
                        }

                        // Automatically expand the bottom sheet and disable swiping when search box is focused
                        LaunchedEffect(isSearchFocused) {
                            sheetSwipeEnabled = !isSearchFocused
                            if (isSearchFocused) {
                                scaffoldState.bottomSheetState.expand()
                            }
                        }

                        HomeScreen(
                            viewModel = viewModel,
                            managePlacesViewModel = managePlacesViewModel,
                            onPlaceSelected = { place ->
                                coroutineScope.launch {
                                    // Clear the focus first. This will happen automatically when the navigation animation
                                    // completes but it's nicer when the keyboard is dismissed immediately.
                                    focusManager.clearFocus()
                                    // Force the sheet to be partially expanded.
                                    scaffoldState.bottomSheetState.partialExpand()
                                }
                                val placeJson = Gson().toJson(place)
                                navController.navigate("place_card?place=$placeJson")
                            },
                            onPeekHeightChange = { peekHeight = it },
                            isSearchFocused = isSearchFocused,
                            onSearchFocusChange = { isSearchFocused = it })
                    }

                    composable("place_card?place={place}") { backStackEntry ->
                        LaunchedEffect(key1 = Unit) {
                            // The place card starts partially expanded.
                            coroutineScope.launch {
                                bottomSheetState.partialExpand()
                            }
                        }
                        val viewModel: PlaceCardViewModel = hiltViewModel()
                        val placeJson = backStackEntry.arguments?.getString("place")
                        val place = placeJson?.let { Gson().fromJson(it, Place::class.java) }
                        place?.let { place ->

                            viewModel.setPlace(place)

                            DisposableEffect(place) {
                                val position = Position(place.longitude, place.latitude)
                                // Clear any existing pins and add the new one to ensure only one pin is shown at a time
                                mapPins.clear()
                                mapPins.add(position)

                                val previousBackStackEntry = navController.previousBackStackEntry
                                val shouldFlyToPoi =
                                    previousBackStackEntry?.destination?.route == Screen.Home.route

                                // Only animate if we're entering from the home screen, as opposed to e.g. popping from the
                                // settings screen. This is brittle and may break if we end up with more entry points.
                                if (shouldFlyToPoi) {
                                    coroutineScope.launch {
                                        cameraState.animateTo(
                                            CameraPosition(
                                                target = position, zoom = 15.0
                                            ),
                                            duration = appPreferenceRepository.animationSpeedDurationValue
                                        )
                                    }
                                }
                                onDispose {
                                    mapPins.clear()
                                }
                            }

                            PlaceCardScreen(
                                place = place,
                                viewModel = viewModel,
                                onBack = {
                                    navController.popBackStack()
                                },
                                onGetDirections = { /* TODO: Implement directions functionality */ },
                                onPeekHeightChange = { peekHeight = it })
                        }
                    }

                    composable(Screen.OfflineAreas.route) {
                        LaunchedEffect(key1 = Unit) {
                            peekHeight = configuration.screenHeightDp.dp / 3
                            // The offline areas screen starts partially expanded.
                            coroutineScope.launch {
                                bottomSheetState.partialExpand()
                            }
                        }
                        DisposableEffect(key1 = Unit) {
                            onDispose {
                                selectedOfflineArea = null
                            }
                        }
                        val viewModel: OfflineAreasViewModel = hiltViewModel()
                        cameraState.projection?.queryVisibleRegion()?.let { visibleRegion ->
                            OfflineAreasScreen(
                                currentViewport = visibleRegion,
                                viewModel = viewModel,
                                onDismiss = { navController.popBackStack() },
                                onAreaSelected = { area ->
                                    coroutineScope.launch {
                                        scaffoldState.bottomSheetState.partialExpand()
                                        cameraState.animateTo(
                                            boundingBox = BoundingBox(
                                                area.west,
                                                area.south,
                                                area.east,
                                                area.north
                                            ),
                                            padding = PaddingValues(
                                                configuration.screenWidthDp.dp / 4,
                                                configuration.screenHeightDp.dp / 4,
                                                configuration.screenWidthDp.dp / 4,
                                                configuration.screenHeightDp.dp / 2
                                            ),
                                            duration = appPreferenceRepository.animationSpeedDurationValue
                                        )
                                    }
                                    selectedOfflineArea = area
                                }
                            )
                        }
                    }

                    composable(Screen.Settings.route) {
                        LaunchedEffect(key1 = Unit) {
                            // Don't allow partial expansion while we're in this state.
                            allowPartialExpansion = false
                            sheetSwipeEnabled = false
                            // The settings screen is always fully expanded.
                            coroutineScope.launch {
                                bottomSheetState.expand()
                            }
                        }
                        DisposableEffect(Unit) {
                            onDispose {
                                // Re-enable partial expansion when leaving this screen
                                allowPartialExpansion = true
                                sheetSwipeEnabled = true
                            }
                        }
                        SettingsScreen(
                            onDismiss = { navController.popBackStack() },
                            appPreferenceRepository = appPreferenceRepository,
                            navController = navController
                        )
                    }
                }

            }
        },
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                port?.let { port ->
                    MapView(
                        port = port,
                        mapViewModel = mapViewModel,
                        onMapInteraction = { },
                        onRequestLocationPermission = onRequestLocationPermission,
                        hasLocationPermission = hasLocationPermission,
                        fabInsets = PaddingValues(
                            start = 0.dp,
                            top = 0.dp,
                            end = 0.dp,
                            bottom = configuration.screenHeightDp.dp - fabHeight
                        ),
                        cameraState = cameraState,
                        mapPins = mapPins,
                        appPreferences = appPreferenceRepository,
                        selectedOfflineArea = selectedOfflineArea
                    )
                }

                Box(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    // Avatar icon button in top left
                    FloatingActionButton(
                        onClick = { navController.navigate(Screen.Settings.route) },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .size(64.dp)
                            .border(
                                width = 4.dp,
                                color = MaterialTheme.colorScheme.surface,
                                shape = CircleShape
                            ),
                        containerColor = MaterialTheme.colorScheme.surfaceDim,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "\uD83E\uDD55", // Carrot emoji
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
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
        })
}
