package earth.maps.cardinal.ui

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.gson.Gson
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.viewmodel.HomeViewModel
import earth.maps.cardinal.viewmodel.ManagePlacesViewModel
import earth.maps.cardinal.viewmodel.MapViewModel
import earth.maps.cardinal.viewmodel.PlaceCardViewModel
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
    hasLocationPermission: Boolean
) {
    val cameraState = rememberCameraState()
    val mapView: @Composable (onMapInteraction: () -> Unit, fabInsets: PaddingValues, targetViewport: CameraPosition?) -> Unit =
        { onMapInteraction, fabInsets, targetViewport ->
            port?.let { port ->
                MapView(
                    port = port,
                    mapViewModel = mapViewModel,
                    onMapInteraction = onMapInteraction,
                    onRequestLocationPermission = onRequestLocationPermission,
                    hasLocationPermission = hasLocationPermission,
                    fabInsets = fabInsets,
                    targetViewport = targetViewport,
                    cameraState = cameraState
                )
            }
        }

    val scaffoldState = rememberBottomSheetScaffoldState()
    var peekHeight by remember { mutableStateOf(0.dp) }
    var fabHeight by remember { mutableStateOf(0.dp) }
    var targetViewport by remember { mutableStateOf<CameraPosition?>(null) }
    var sheetSwipeEnabled by remember { mutableStateOf(true) }
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetSwipeEnabled = sheetSwipeEnabled,
        modifier = Modifier,
        sheetPeekHeight = peekHeight,
        sheetContent = {
            Box(modifier = Modifier.onGloballyPositioned {
                fabHeight = with(density) { it.positionOnScreen().y.toDp() - 50.dp }
            }) {
                NavHost(
                    navController = navController,
                    startDestination = "home"
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
                            onSearchFocusChange = { isSearchFocused = it }
                        )
                    }

                    composable("place_card?place={place}") { backStackEntry ->
                        val viewModel: PlaceCardViewModel = hiltViewModel()
                        val placeJson = backStackEntry.arguments?.getString("place")
                        val place = placeJson?.let { Gson().fromJson(it, Place::class.java) }
                        place?.let { place ->
                            viewModel.setPlace(place)
                            PlaceCardScreen(
                                place = place,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onGetDirections = { /* TODO: Implement directions functionality */ },
                                onPeekHeightChange = { peekHeight = it }
                            )
                        }
                    }
                }

            }
        },
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                mapView(
                    {},
                    PaddingValues(
                        start = 0.dp,
                        top = 0.dp,
                        end = 0.dp,
                        bottom = configuration.screenHeightDp.dp - fabHeight
                    ),
                    targetViewport
                )

                // Download FAB that displays when zoom level is >= 10 with slide animation
                AnimatedVisibility(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = configuration.screenHeightDp.dp - fabHeight),
                    visible = cameraState.position.zoom >= 7.0,
                    enter = slideInHorizontally(initialOffsetX = { -it }),
                    exit = slideOutHorizontally(targetOffsetX = { -it })
                ) {
                    FloatingActionButton(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 16.dp),
                        onClick = {
                            // TODO: Implement download functionality
                        }
                    ) {
                        Icon(
                            painter = painterResource(earth.maps.cardinal.R.drawable.cloud_download_24dp),
                            contentDescription = "Download"
                        )
                    }
                }
            }
        }
    )
}
