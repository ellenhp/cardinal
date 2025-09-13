package earth.maps.cardinal.tileserver

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import earth.maps.cardinal.data.AppPreferenceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TileserverService : Service() {
    private lateinit var tileserver: Tileserver
    private val TAG = "TileserverService"
    
    // For accessing offline mode preference
    private lateinit var appPreferenceRepository: AppPreferenceRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Binder given to clients
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): TileserverService = this@TileserverService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Creating tile server service")
        
        // Initialize the AppPreferenceRepository
        appPreferenceRepository = AppPreferenceRepository(this)
        
        tileserver = Tileserver(this) // Pass context to Tileserver
        tileserver.start()
        Log.d(TAG, "Tile server service created and started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Tile server service started with intent: $intent, flags: $flags, startId: $startId")
        return START_STICKY // Restart service if it's killed
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Destroying tile server service")
        tileserver.stop()
        Log.d(TAG, "Tile server service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun getPort(): Int {
        return tileserver.getPort()
    }
}
