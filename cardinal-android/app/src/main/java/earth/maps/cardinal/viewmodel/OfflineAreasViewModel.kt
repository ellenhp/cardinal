package earth.maps.cardinal.viewmodel

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import earth.maps.cardinal.data.DownloadStatus
import earth.maps.cardinal.data.OfflineArea
import earth.maps.cardinal.data.OfflineAreaRepository
import earth.maps.cardinal.tileserver.TileDownloadService
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class OfflineAreasViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val offlineAreaRepository: OfflineAreaRepository,
    private val tileDownloadService: TileDownloadService,
) : ViewModel() {

    val offlineAreas = mutableStateOf<List<OfflineArea>>(emptyList())
    val isDownloading = mutableStateOf(false)
    val downloadProgress = mutableIntStateOf(0)
    val totalTiles = mutableIntStateOf(0)
    val currentAreaName = mutableStateOf("")

    init {
        loadOfflineAreas()
    }

    private fun loadOfflineAreas() {
        viewModelScope.launch {
            offlineAreaRepository.getAllOfflineAreas().collect { areas ->
                offlineAreas.value = areas
            }
        }
    }

    fun startDownload(
        north: Double,
        south: Double,
        east: Double,
        west: Double,
        minZoom: Int,
        maxZoom: Int,
        name: String
    ) {
        viewModelScope.launch {
            isDownloading.value = true
            currentAreaName.value = name

            // Create offline area entry
            val areaId = UUID.randomUUID().toString()
            val offlineArea = OfflineArea(
                id = areaId,
                name = name,
                north = north,
                south = south,
                east = east,
                west = west,
                minZoom = minZoom,
                maxZoom = maxZoom,
                downloadDate = System.currentTimeMillis(),
                fileSize = 0L,
                status = DownloadStatus.DOWNLOADING
            )

            Log.d("OfflineAreasViewModel", "Starting download for area: $name (ID: $areaId)")
            Log.d(
                "OfflineAreasViewModel",
                "Bounds: N=$north, S=$south, E=$east, W=$west, Zoom: $minZoom-$maxZoom"
            )

            offlineAreaRepository.insertOfflineArea(offlineArea)

            // Start tile download using single database
            tileDownloadService.downloadTiles(
                north, south, east, west,
                minZoom, maxZoom,
                areaId,  // Pass areaId instead of filename
                name,    // Pass area name
                { progress, total ->
                    downloadProgress.intValue = progress
                    totalTiles.intValue = total
                    Log.v("OfflineAreasViewModel", "Basemap download progress: $progress/$total")
                },
                valhallaProgressCallback = { progress, total ->
                    Log.v("OfflineAreasViewModel", "Valhalla download progress: $progress/$total")
                },
                { success, fileSize ->
                    Log.d(
                        "OfflineAreasViewModel",
                        "Download completed for area: $name (ID: $areaId), success: $success, file size: $fileSize"
                    )
                    // Update the offline area with the result
                    val updatedArea = if (success) {
                        offlineArea.copy(
                            status = DownloadStatus.COMPLETED,
                            fileSize = fileSize
                        )
                    } else {
                        offlineArea.copy(
                            status = DownloadStatus.FAILED,
                            fileSize = 0L
                        )
                    }
                    viewModelScope.launch {
                        offlineAreaRepository.updateOfflineArea(updatedArea)
                    }

                    isDownloading.value = false
                }
            )
        }
    }

    fun cancelDownload() {
        // Cancel the download
        tileDownloadService.cancelDownload()
        isDownloading.value = false
    }

    fun deleteOfflineArea(offlineArea: OfflineArea) {
        viewModelScope.launch {
            // Delete tiles from the single database
            tileDownloadService.deleteTilesForArea(offlineArea.id)

            // Delete the offline area entry from Room database
            offlineAreaRepository.deleteOfflineArea(offlineArea)
        }
    }

    /**
     * Calculate bounding box from current location and radius
     */
    fun calculateBoundingBox(location: Location, radiusMeters: Double): BoundingBox {
        // Approximate conversion: 1 degree latitude ≈ 111 km
        val latDegreeInMeters = 111000.0
        val lonDegreeInMeters = 111000.0 * Math.cos(Math.toRadians(location.latitude))

        val latDelta = radiusMeters / latDegreeInMeters
        val lonDelta = radiusMeters / lonDegreeInMeters

        return BoundingBox(
            north = location.latitude + latDelta,
            south = location.latitude - latDelta,
            east = location.longitude + lonDelta,
            west = location.longitude - lonDelta
        )
    }

    /**
     * Calculate the estimated number of tiles for a bounding box and zoom range
     */
    fun estimateTileCount(
        north: Double,
        south: Double,
        east: Double,
        west: Double,
        minZoom: Int,
        maxZoom: Int
    ): Int {
        // Use the same logic as in TileDownloadService
        var totalTiles = 0

        for (zoom in minZoom..kotlin.math.min(maxZoom, 14)) {
            val (minX, maxX, minY, maxY) = tileDownloadService.calculateTileRange(
                north,
                south,
                east,
                west,
                zoom
            )
            totalTiles += (maxX - minX + 1) * (maxY - minY + 1)
        }

        return totalTiles
    }

    /**
     * Data class to represent a bounding box
     */
    data class BoundingBox(
        val north: Double,
        val south: Double,
        val east: Double,
        val west: Double
    )
}
