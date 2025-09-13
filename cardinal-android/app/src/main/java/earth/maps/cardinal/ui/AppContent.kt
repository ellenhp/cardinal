package earth.maps.cardinal.ui

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.gson.Gson
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.data.ContrastPreferences
import earth.maps.cardinal.data.ContrastRepository
import earth.maps.cardinal.data.OfflineArea
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.viewmodel.HomeViewModel
import earth.maps.cardinal.viewmodel.ManagePlacesViewModel
import earth.maps.cardinal.viewmodel.MapViewModel
import javax.inject.Inject
import earth.maps.cardinal.viewmodel.PlaceCardViewModel
import io.github.dellisd.spatialk.geojson.BoundingBox
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.offline.rememberOfflineManager

enum class AppContentState {
    None,
    ShowingOfflineAreas,
    ShowingSettings
}

typealias AppContentStateSetter = (AppContentState) -> Unit

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(
    navController: NavHostController,
    mapViewModel: MapViewModel,
    port: Int?,
    onRequestLocationPermission: () -> Unit,
    hasLocationPermission: Boolean,
    contrastRepository: ContrastRepository
) {
    val mapPins = remember { mutableStateListOf<Position>() }
    val cameraState = rememberCameraState()
    val offlineManager = rememberOfflineManager()

    val scaffoldState = rememberBottomSheetScaffoldState()
    var peekHeight by remember { mutableStateOf(0.dp) }
    var fabHeight by remember { mutableStateOf(0.dp) }
    var sheetSwipeEnabled by remember { mutableStateOf(true) }
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var appContentState by remember { mutableStateOf(AppContentState.None) }
    var selectedOfflineArea by remember { mutableStateOf<OfflineArea?>(null) }
    val setAppContentState: AppContentStateSetter = { state ->
        appContentState = state
    }

    LaunchedEffect(key1 = Unit) {
        offlineManager.setTileCountLimit(0)
        offlineManager.clearAmbientCache()
    }

    val sheetPeekHeightEmpirical = dimensionResource(dimen.empirical_bottom_sheet_handle_height)

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
                            onSearchFocusChange = { isSearchFocused = it },
                            setAppContentState = setAppContentState)
                    }

                    composable("place_card?place={place}") { backStackEntry ->
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
                                coroutineScope.launch {
                                    cameraState.animateTo(
                                        CameraPosition(
                                            target = position, zoom = 15.0
                                        )
                                    )
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
                                onPeekHeightChange = { peekHeight = it },
                                setAppContentState = setAppContentState)
                        }
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
                        selectedOfflineArea = if (appContentState == AppContentState.ShowingOfflineAreas) selectedOfflineArea else null,
                        setAppContentState = setAppContentState
                    )
                }

                Box(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    // Avatar icon button in top left
                    FloatingActionButton(
                        onClick = { setAppContentState(AppContentState.ShowingSettings) },
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

                // Offline Areas Bottom Sheet
                if (appContentState == AppContentState.ShowingOfflineAreas) {
                    val sheetState = rememberModalBottomSheetState()
                    ModalBottomSheet(
                        onDismissRequest = {
                            appContentState = AppContentState.None
                            selectedOfflineArea = null
                        },
                        sheetState = sheetState
                    ) {
                        cameraState.projection?.queryVisibleRegion()?.let {
                            OfflineAreasScreen(
                                currentViewport = it,
                                onDismiss = {
                                    appContentState = AppContentState.None
                                    selectedOfflineArea = null
                                },
                                onAreaSelected = { area ->
                                    selectedOfflineArea = area
                                    coroutineScope.launch {
                                        sheetState.partialExpand()
                                        cameraState.animateTo(
                                            boundingBox = BoundingBox(
                                                west = area.west,
                                                east = area.east,
                                                north = area.north,
                                                south = area.south
                                            ),
                                            padding = PaddingValues(
                                                start = configuration.screenWidthDp.dp / 4,
                                                end = configuration.screenWidthDp.dp / 4,
                                                top = configuration.screenWidthDp.dp / 4,
                                                bottom = configuration.screenHeightDp.dp * 5 / 8
                                            )
                                        )
                                    }
                                },
                                setAppContentState = setAppContentState
                            )
                        }
                    }
                }

                // Settings Bottom Sheet
                if (appContentState == AppContentState.ShowingSettings) {
                    val sheetState = rememberModalBottomSheetState()
                    ModalBottomSheet(
                        onDismissRequest = { appContentState = AppContentState.None }, sheetState = sheetState
                    ) {
                        SettingsScreen(
                            onDismiss = { appContentState = AppContentState.None },
                            contrastRepository = contrastRepository,
                            setAppContentState = setAppContentState)
                    }
                }
            }
        })
}
