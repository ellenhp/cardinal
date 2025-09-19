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

package earth.maps.cardinal.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import earth.maps.cardinal.data.OfflineArea
import earth.maps.cardinal.data.OfflineAreaRepository
import earth.maps.cardinal.tileserver.TileDownloadForegroundService
import earth.maps.cardinal.tileserver.calculateTileRange
import earth.maps.cardinal.ui.BoundingBox
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OfflineAreasViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val offlineAreaRepository: OfflineAreaRepository,
) : ViewModel() {

    val offlineAreas = mutableStateOf<List<OfflineArea>>(emptyList())
    val isDownloading = mutableStateOf(false)
    val isPaused = mutableStateOf(false)
    val downloadProgress = mutableIntStateOf(0)
    val totalTiles = mutableIntStateOf(0)
    val currentAreaName = mutableStateOf("")

    // New unified progress properties
    val unifiedProgress = mutableFloatStateOf(0f) // 0.0 to 1.0
    val currentStage =
        mutableStateOf(TileDownloadForegroundService.DownloadStage.BASEMAP)

    // Service binding infrastructure
    private var serviceBinder: TileDownloadForegroundService.TileDownloadBinder? = null
    private var progressJob: Job? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Connected to TileDownloadForegroundService")
            serviceBinder = service as TileDownloadForegroundService.TileDownloadBinder
            isBound = true
            syncWithOngoingDownloads()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Disconnected from TileDownloadForegroundService")
            serviceBinder = null
            isBound = false
            progressJob?.cancel()
            progressJob = null
            // Reset progress state when service disconnects
            resetProgressState()
        }

        override fun onBindingDied(name: ComponentName?) {
            Log.d(TAG, "Binding to TileDownloadForegroundService died")
            serviceBinder = null
            isBound = false
            progressJob?.cancel()
            progressJob = null
            resetProgressState()
        }

        override fun onNullBinding(name: ComponentName?) {
            Log.d(TAG, "Null binding to TileDownloadForegroundService")
            serviceBinder = null
            isBound = false
            resetProgressState()
        }
    }

    init {
        loadOfflineAreas()
        bindToService()
    }

    /**
     * Called when ViewModel is created to sync with any ongoing downloads
     */
    private fun syncWithOngoingDownloads() {
        viewModelScope.launch {
            // Give service binding a moment to establish
            kotlinx.coroutines.delay(500)

            if (isBound && serviceBinder != null) {
                val service = serviceBinder!!.getService()
                // Check if service is currently downloading
                val isServiceDownloading = service.isDownloading.value
                if (isServiceDownloading) {
                    Log.d(TAG, "Detected ongoing download in service, syncing UI state")
                    // The StateFlow observations should handle the rest
                } else {
                    Log.d(TAG, "No ongoing downloads detected in service")
                }
            } else {
                Log.d(TAG, "Service not bound yet, will sync when connection established")
            }
        }
    }

    private fun loadOfflineAreas() {
        viewModelScope.launch {
            offlineAreaRepository.getAllOfflineAreas().collect { areas ->
                offlineAreas.value = areas
                // Reset progress state based on current offline areas status
                resetProgressState()
            }
        }
    }

    fun startDownload(
        boundingBox: BoundingBox, name: String
    ) {
        serviceBinder?.getService()?.startDownload(
            boundingBox,
            OFFLINE_AREA_MIN_ZOOM,
            OFFLINE_AREA_MAX_ZOOM,
            name
        )
    }

    fun deleteOfflineArea(offlineArea: OfflineArea) {
        viewModelScope.launch {
            // Delete tiles from the single database
            serviceBinder?.getService()?.deleteTilesForArea(offlineArea.id)

            // Delete the offline area entry from Room database
            offlineAreaRepository.deleteOfflineArea(offlineArea)
        }
    }

    /**
     * Calculate the estimated number of tiles for a bounding box and zoom range
     */
    fun estimateTileCount(
        boundingBox: BoundingBox, minZoom: Int, maxZoom: Int
    ): Int {
        // Use the same logic as in tileDownloadManager
        var totalTiles = 0

        for (zoom in minZoom..kotlin.math.min(maxZoom, 14)) {
            val (minX, maxX, minY, maxY) = calculateTileRange(
                boundingBox, zoom
            )
            totalTiles += (maxX - minX + 1) * (maxY - minY + 1)
        }

        return totalTiles
    }

    /**
     * Bind to the TileDownloadForegroundService
     */
    private fun bindToService() {
        if (!isBound) {
            Log.d(TAG, "Binding to TileDownloadForegroundService")
            val intent = Intent(context, TileDownloadForegroundService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    /**
     * Unbind from the TileDownloadForegroundService
     */
    private fun unbindFromService() {
        if (isBound) {
            Log.d(TAG, "Unbinding from TileDownloadForegroundService")
            context.unbindService(serviceConnection)
            serviceBinder = null
            isBound = false
        }
        progressJob?.cancel()
        progressJob = null
    }

    /**
     * Reset progress state when service is not available
     */
    private fun resetProgressState() {
        // Only reset if we're sure there's no ongoing download or processing
        // Check both offline areas status and service state (if available)
        val hasActiveAreas = offlineAreas.value.any {
            it.isIncomplete()
        }

        if (!hasActiveAreas) {
            // No downloading or processing areas in database
            isDownloading.value = false
            downloadProgress.intValue = 0
            totalTiles.intValue = 0
            currentAreaName.value = ""
            Log.d(TAG, "Reset progress state - no active downloads in database")
        } else {
            Log.d(TAG, "Not resetting progress state - active areas exist in database")
        }
    }

    /**
     * Clean up when ViewModel is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        unbindFromService()
    }

    companion object {
        const val OFFLINE_AREA_MIN_ZOOM = 5
        const val OFFLINE_AREA_MAX_ZOOM = 14
        private const val TAG = "OfflineAreasViewModel"
    }
}
