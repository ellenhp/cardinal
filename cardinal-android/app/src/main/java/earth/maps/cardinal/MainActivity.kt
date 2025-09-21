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

package earth.maps.cardinal

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import earth.maps.cardinal.data.AppPreferenceRepository
import earth.maps.cardinal.data.LatLng
import earth.maps.cardinal.data.LocationRepository
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.RoutingMode
import earth.maps.cardinal.routing.FerrostarWrapperRepository
import earth.maps.cardinal.routing.RouteRepository
import earth.maps.cardinal.tileserver.LocalMapServerService
import earth.maps.cardinal.tileserver.PermissionRequest
import earth.maps.cardinal.tileserver.PermissionRequestManager
import earth.maps.cardinal.ui.AppContent
import earth.maps.cardinal.ui.NavigationCoordinator
import earth.maps.cardinal.ui.TurnByTurnNavigationScreen
import earth.maps.cardinal.ui.theme.AppTheme
import earth.maps.cardinal.viewmodel.MapViewModel
import kotlinx.coroutines.launch
import java.lang.Double.parseDouble
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appPreferenceRepository: AppPreferenceRepository

    @Inject
    lateinit var ferrostarWrapperRepository: FerrostarWrapperRepository

    @Inject
    lateinit var permissionRequestManager: PermissionRequestManager

    @Inject
    lateinit var locationRepository: LocationRepository

    @Inject
    lateinit var routeRepository: RouteRepository

    private var localMapServerService: LocalMapServerService? = null
    private var bound by mutableStateOf(false)
    private var port by mutableStateOf<Int?>(null)
    private var hasLocationPermission by mutableStateOf(false)
    private var deepLinkDestination by mutableStateOf<String?>(null)

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val TAG = "MainActivity"
        const val EXTRA_DEEP_LINK_DESTINATION = "deep_link_destination"
        const val DEEP_LINK_OFFLINE_AREAS = "offline_areas"
    }

    private fun requestLocationPermission() {
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
            ), LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    // Permission request launcher for notification permission (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Notification permission granted")
            lifecycleScope.launch {
                permissionRequestManager.onPermissionGranted(PermissionRequest.NotificationPermission)
            }
        } else {
            Log.d(TAG, "Notification permission denied")
            lifecycleScope.launch {
                permissionRequestManager.onPermissionDenied(PermissionRequest.NotificationPermission)
            }
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalMapServerService, cast the IBinder and get LocalMapServerService instance
            val binder = service as LocalMapServerService.LocalBinder
            localMapServerService = binder.getService()
            bound = true
            // Get the port
            port = localMapServerService?.getPort()
            Log.d(TAG, "Connected to tile server service on port: $port")

            // Configure Ferrostar to use the local routing endpoint
            port?.let { port ->
                val routingEndpoint = "http://127.0.0.1:$port/route"
                ferrostarWrapperRepository.setValhallaEndpoint(routingEndpoint)
                Log.d(TAG, "Configured Ferrostar to use local routing endpoint: $routingEndpoint")
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
            Log.d(TAG, "Disconnected from tile server service")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        intent?.takeIf { it.action == Intent.ACTION_VIEW }?.let { intent ->
            val data: Uri? = intent.data
            if (data != null && data.scheme != null && data.scheme.equals("geo")) {
                handleGeoIntent(data)
            }

            // Check for deep link destination
            if (deepLinkDestination == null) {
                deepLinkDestination = intent.getStringExtra(EXTRA_DEEP_LINK_DESTINATION)
            }
        }

        setContent {
            val contrastLevel by appPreferenceRepository.contrastLevel.collectAsState()
            AppTheme(contrastLevel = contrastLevel) {
                val navController = rememberNavController()
                val mapViewModel: MapViewModel = hiltViewModel()

                val innerNavController = rememberNavController()
                val coordinator = NavigationCoordinator(
                    mainNavController = navController,
                    bottomSheetNavController = innerNavController,
                    routeRepository
                )
                if (!coordinator.isInHomeScreen()) {
                    BackHandler {
                        coordinator.navigateBack()
                    }
                }

                val coroutineScope = rememberCoroutineScope()
                LaunchedEffect(key1 = deepLinkDestination) {
                    deepLinkDestination?.let {
                        Log.d(TAG, "Deep link: $it")

                        coroutineScope.launch {
                            coordinator.navigateRaw(it)
                        }
                    }
                }
                NavHost(
                    navController = navController, startDestination = "main"
                ) {
                    composable("main") {
                        AppContent(
                            navController = innerNavController,
                            mapViewModel = mapViewModel,
                            port = port,
                            onRequestLocationPermission = { requestLocationPermission() },
                            hasLocationPermission = hasLocationPermission,
                            appPreferenceRepository = appPreferenceRepository,
                            navigationCoordinator = coordinator,
                        )
                    }

                    composable("turn_by_turn?routeId={routeId}&routingMode={routingMode}") { backStackEntry ->
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
        }
    }

    private fun handleGeoIntent(data: Uri) {
        parseGeoIntent(data)?.let { place ->
            val placeJson = Uri.encode(Gson().toJson(place))
            deepLinkDestination = "place_card?place=$placeJson"
        }
    }

    private fun parseGeoIntent(data: Uri): Place? {
        val poiName = data.schemeSpecificPart.split("?q=").getOrNull(1)
        val pathComponents = data.schemeSpecificPart.split("?").first().split(',').mapNotNull {
            try {
                parseDouble(it.trim())
            } catch (_: NumberFormatException) {
                null
            }
        }
        if (pathComponents.size == 2) {
            val lat = pathComponents[0]
            val lng = pathComponents[1]
            return locationRepository.fromNameAndLatLng(poiName, latLng = LatLng(lat, lng))
        }
        return null
    }

    override fun onStart() {
        super.onStart()
        ferrostarWrapperRepository.androidTtsObserver.start()

        // Observe permission requests from services
        lifecycleScope.launch {
            permissionRequestManager.permissionRequests.collect { request ->
                when (request) {
                    is PermissionRequest.NotificationPermission -> {
                        Log.d(TAG, "Handling notification permission request")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            // Permission not required for older Android versions
                            permissionRequestManager.onPermissionGranted(request)
                        }
                    }
                }
            }
        }

        // Start the tile server service
        val serviceIntent = Intent(this, LocalMapServerService::class.java)
        startService(serviceIntent)

        // Bind to the service
        bindService(serviceIntent, connection, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        ferrostarWrapperRepository.androidTtsObserver.stopAndClearQueue()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String?>, grantResults: IntArray, deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                // Check if all permissions were granted
                hasLocationPermission =
                    grantResults.isNotEmpty() && grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ferrostarWrapperRepository.androidTtsObserver.shutdown()
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }
}
