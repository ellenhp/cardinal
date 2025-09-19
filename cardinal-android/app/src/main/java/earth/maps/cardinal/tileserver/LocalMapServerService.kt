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
