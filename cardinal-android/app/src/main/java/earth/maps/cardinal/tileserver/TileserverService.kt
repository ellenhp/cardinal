package earth.maps.cardinal.tileserver

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log

class TileserverService : Service() {
    private lateinit var tileserver: Tileserver
    private val TAG = "TileserverService"

    // Binder given to clients
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): TileserverService = this@TileserverService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Creating tile server service")
        tileserver = Tileserver(this) // Pass context to Tileserver
        tileserver.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Tile server service started")
        return START_STICKY // Restart service if it's killed
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Destroying tile server service")
        tileserver.stop()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun getPort(): Int {
        return tileserver.getPort()
    }
}
