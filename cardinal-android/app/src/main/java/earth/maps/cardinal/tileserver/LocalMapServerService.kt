package earth.maps.cardinal.tileserver

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import earth.maps.cardinal.data.AppPreferences
import earth.maps.cardinal.routing.MultiplexedRoutingService
import javax.inject.Inject

@AndroidEntryPoint
class LocalMapServerService : Service() {
    private lateinit var localMapServer: LocalMapServer

    // For accessing offline mode preference
    private lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var multiplexedRoutingService: MultiplexedRoutingService

    // Binder given to clients
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): LocalMapServerService = this@LocalMapServerService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Creating tile server service")

        // Initialize the AppPreferenceRepository
        appPreferences = AppPreferences(this)

        // TODO: Get an instance of MultiplexedRoutingService (via hilt?) and pass it in here.
        localMapServer =
            LocalMapServer(this, appPreferences, multiplexedRoutingService) // Pass context and repository to LocalMapServer
        localMapServer.start()
        Log.d(TAG, "Tile server service created and started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(
            TAG,
            "Tile server service started with intent: $intent, flags: $flags, startId: $startId"
        )
        return START_STICKY // Restart service if it's killed
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Destroying tile server service")
        localMapServer.stop()
        Log.d(TAG, "Tile server service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun getPort(): Int {
        return localMapServer.getPort()
    }

    companion object {
        private const val TAG = "LocalMapServerService"
    }
}
