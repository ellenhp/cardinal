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

        localMapServer =
            LocalMapServer(
                this,
                appPreferences,
                multiplexedRoutingService
            ) // Pass context and repository to LocalMapServer
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
