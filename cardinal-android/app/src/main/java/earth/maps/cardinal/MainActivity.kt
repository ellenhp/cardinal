package earth.maps.cardinal

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import earth.maps.cardinal.data.AppPreferenceRepository
import earth.maps.cardinal.data.RoutingMode
import earth.maps.cardinal.routing.FerrostarWrapperRepository
import earth.maps.cardinal.tileserver.TileserverService
import earth.maps.cardinal.ui.AppContent
import earth.maps.cardinal.ui.NavigationCoordinator
import earth.maps.cardinal.ui.Screen
import earth.maps.cardinal.ui.TurnByTurnNavigationScreen
import earth.maps.cardinal.ui.theme.AppTheme
import earth.maps.cardinal.viewmodel.MapViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appPreferenceRepository: AppPreferenceRepository

    private var tileserverService: TileserverService? = null
    private var bound by mutableStateOf(false)
    private var port by mutableStateOf<Int?>(null)
    private var hasLocationPermission by mutableStateOf(false)

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val TAG = "MainActivity"
    }

    private fun requestLocationPermission() {
        requestPermissions(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to TileserverService, cast the IBinder and get TileserverService instance
            val binder = service as TileserverService.LocalBinder
            tileserverService = binder.getService()
            bound = true
            // Get the port
            port = tileserverService?.getPort()
            Log.d(TAG, "Connected to tile server service on port: $port")
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
            Log.d(TAG, "Disconnected from tile server service")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val contrastLevel by appPreferenceRepository.contrastLevel.collectAsState()
            AppTheme(contrastLevel = contrastLevel) {
                val navController = rememberNavController()
                val mapViewModel: MapViewModel = hiltViewModel()

                NavHost(
                    navController = navController,
                    startDestination = "main"
                ) {
                    composable("main") {
                        val innerNavController = rememberNavController()
                        val coordinator = NavigationCoordinator(
                            mainNavController = navController,
                            bottomSheetNavController = innerNavController
                        )

                        AppContent(
                            navController = innerNavController,
                            mapViewModel = mapViewModel,
                            port = port,
                            onRequestLocationPermission = { requestLocationPermission() },
                            hasLocationPermission = hasLocationPermission,
                            appPreferenceRepository = appPreferenceRepository,
                            navigationCoordinator = coordinator
                        )
                    }

                    composable("turn_by_turn?ferrostarRoute={ferrostarRoute}&routingMode={routingMode}") { backStackEntry ->
                        val ferrostarRouteJson = backStackEntry.arguments?.getString("ferrostarRoute")
                        val routingModeJson = backStackEntry.arguments?.getString("routingMode")
                        
                        val ferrostarRoute = ferrostarRouteJson?.let { 
                            try {
                                Gson().fromJson(it, uniffi.ferrostar.Route::class.java)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        
                        val routingMode = routingModeJson?.let {
                            try {
                                Gson().fromJson(it, RoutingMode::class.java)
                            } catch (e: Exception) {
                                RoutingMode.AUTO
                            }
                        } ?: RoutingMode.AUTO
                        
                        port?.let { port ->
                            TurnByTurnNavigationScreen(
                                port = port,
                                mode = routingMode,
                                route = ferrostarRoute
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Start the tile server service
        val serviceIntent = Intent(this, TileserverService::class.java)
        startService(serviceIntent)

        // Bind to the service
        bindService(serviceIntent, connection, BIND_AUTO_CREATE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int
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
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }
}
