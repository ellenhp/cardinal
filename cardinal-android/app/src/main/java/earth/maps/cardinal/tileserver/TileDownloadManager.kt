package earth.maps.cardinal.tileserver

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.IBinder
import android.util.Log
import earth.maps.cardinal.R
import earth.maps.cardinal.data.DownloadStatus
import earth.maps.cardinal.data.DownloadedTile
import earth.maps.cardinal.data.DownloadedTileDao
import earth.maps.cardinal.data.OfflineArea
import earth.maps.cardinal.data.OfflineAreaDao
import earth.maps.cardinal.data.TileType
import earth.maps.cardinal.geocoding.TileProcessor
import earth.maps.cardinal.ui.BoundingBox
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.tan

class TileDownloadManager(
    private val context: Context,
    private val downloadedTileDao: DownloadedTileDao,
    private val offlineAreaDao: OfflineAreaDao,
    private val tileProcessor: TileProcessor? = null
) {
    private val TAG = "TileDownloadManager"
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())
    private var downloadJob: Job? = null
    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation)
    }

    // Service binding infrastructure
    private var serviceBinder: TileDownloadForegroundService.TileDownloadBinder? = null
    private var progressJob: Job? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Connected to TileDownloadForegroundService")
            serviceBinder = service as TileDownloadForegroundService.TileDownloadBinder
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Disconnected from TileDownloadForegroundService")
            serviceBinder = null
            isBound = false
        }
    }

    companion object {
        private const val MAX_BASEMAP_ZOOM = 14
        private const val OFFLINE_DATABASE_NAME = "offline_areas.mbtiles"
        private const val MAX_CONCURRENT_DOWNLOADS = 10
        private const val MAX_RETRY_COUNT = 3
    }

    /**
     * Determines if the basemap download phase is complete for the given area
     */
    private suspend fun isBasemapPhaseComplete(areaId: String): Boolean {
        val expectedTileCount =
            downloadedTileDao.getDownloadedTileCountForAreaAndType(areaId, TileType.BASEMAP)
        Log.d(TAG, "Found $expectedTileCount basemap tiles for area $areaId")

        val existingArea = offlineAreaDao.getOfflineAreaById(areaId)
        if (existingArea == null) return false

        // Calculate expected total basemap tiles
        val totalExpectedBasemapTiles = calculateTotalTiles(
            boundingBox = existingArea.boundingBox(),
            existingArea.minZoom,
            minOf(existingArea.maxZoom, MAX_BASEMAP_ZOOM)
        )

        Log.d(
            TAG,
            "Basemap phase for area $areaId: $expectedTileCount/$totalExpectedBasemapTiles tiles downloaded"
        )
        return expectedTileCount >= totalExpectedBasemapTiles
    }

    /**
     * Determines if the Valhalla download phase is complete for the given area
     */
    private suspend fun isValhallaPhaseComplete(areaId: String): Boolean {
        val valhallaTiles = ValhallaTileUtils.tilesForBoundingBox(
            offlineAreaDao.getOfflineAreaById(areaId)?.boundingBox() ?: return false
        )

        val expectedValhallaTileCount =
            downloadedTileDao.getDownloadedTileCountForAreaAndType(areaId, TileType.VALHALLA)
        Log.d(
            TAG,
            "Valhalla phase for area $areaId: $expectedValhallaTileCount/${valhallaTiles.size} tiles downloaded"
        )
        return expectedValhallaTileCount >= valhallaTiles.size
    }

    /**
     * Determines which phase the download should resume from based on current progress
     */
    private suspend fun determineResumePhase(areaId: String): DownloadStatus {
        val existingArea =
            offlineAreaDao.getOfflineAreaById(areaId) ?: return DownloadStatus.DOWNLOADING_BASEMAP

        return when (existingArea.status) {
            DownloadStatus.PENDING -> DownloadStatus.DOWNLOADING_BASEMAP
            DownloadStatus.DOWNLOADING_BASEMAP -> {
                if (isBasemapPhaseComplete(areaId)) {
                    DownloadStatus.DOWNLOADING_VALHALLA
                } else {
                    DownloadStatus.DOWNLOADING_BASEMAP
                }
            }

            DownloadStatus.DOWNLOADING_VALHALLA -> DownloadStatus.DOWNLOADING_VALHALLA
            DownloadStatus.PROCESSING_GEOCODER -> DownloadStatus.PROCESSING_GEOCODER
            DownloadStatus.COMPLETED -> DownloadStatus.COMPLETED
            DownloadStatus.FAILED -> DownloadStatus.DOWNLOADING_BASEMAP
        }
    }

    /**
     * Download tiles for a bounding box and zoom range using the foreground service
     */
    fun downloadTiles(
        boundingBox: BoundingBox, minZoom: Int, maxZoom: Int, areaId: String, name: String
    ) {
        downloadJob = coroutineScope.launch {
            try {
                Log.d(TAG, "Starting download for area: $name (ID: $areaId)")

                // Check if this area already exists in the database
                val existingArea = offlineAreaDao.getOfflineAreaById(areaId)
                if (existingArea != null) {
                    Log.d(
                        TAG,
                        "Area $areaId already exists in database with status: ${existingArea.status}"
                    )

                    // Determine which phase to resume from based on current progress
                    val resumePhase = determineResumePhase(areaId)
                    Log.d(TAG, "Determined resume phase for area $areaId: $resumePhase")

                    // If completed, nothing to do
                    if (resumePhase == DownloadStatus.COMPLETED) {
                        Log.d(TAG, "Area $areaId is already completed, skipping download")
                        return@launch
                    }

                    // Update status to indicate we're resuming
                    val updatedArea = existingArea.copy(status = resumePhase)
                    offlineAreaDao.updateOfflineArea(updatedArea)
                    Log.d(TAG, "Updated area $areaId status to $resumePhase for resume")

                } else {
                    // Create OfflineArea and insert into database
                    val offlineArea = OfflineArea(
                        id = areaId,
                        name = name,
                        north = boundingBox.north,
                        south = boundingBox.south,
                        east = boundingBox.east,
                        west = boundingBox.west,
                        minZoom = minZoom,
                        maxZoom = maxZoom,
                        downloadDate = System.currentTimeMillis(),
                        fileSize = 0L,
                        status = DownloadStatus.DOWNLOADING_BASEMAP,
                    )

                    offlineAreaDao.insertOfflineArea(offlineArea)
                    Log.d(TAG, "Created offline area: $areaId with status ${offlineArea.status}")
                }

                // Bind to the service first to ensure we can update progress
                bindToService()

                // Wait for service binding with timeout
                val bindTimeout = 3000L // 3 seconds
                val bindStartTime = System.currentTimeMillis()
                while (!isBound && (System.currentTimeMillis() - bindStartTime) < bindTimeout) {
                    delay(100)
                }

                if (!isBound) {
                    Log.w(TAG, "Service binding timeout, starting service anyway")
                }

                // Start the foreground service
                val intent = Intent(context, TileDownloadForegroundService::class.java).apply {
                    action = TileDownloadForegroundService.ACTION_START_DOWNLOAD
                }
                context.startForegroundService(intent)
                Log.d(TAG, "Started foreground service for download")

            } catch (e: Exception) {
                Log.e(TAG, "Error starting download for area $areaId", e)
                // Update the area status to FAILED
                val area = offlineAreaDao.getOfflineAreaById(areaId)
                if (area != null) {
                    val failedArea = area.copy(status = DownloadStatus.FAILED)
                    offlineAreaDao.updateOfflineArea(failedArea)
                }
            }
        }
    }

    internal suspend fun downloadTilesInternal(
        boundingBox: BoundingBox, minZoom: Int, maxZoom: Int, areaId: String, name: String
    ) {
        var db: SQLiteDatabase? = null
        var basemapResult: Pair<Int, Int> = Pair(0, 0)
        var valhallaResult: Pair<Int, Int> = Pair(0, 0)

        try {
            Log.d(TAG, "Starting tile download for area: $name (ID: $areaId)")
            Log.d(
                TAG,
                "Bounds: N=${boundingBox.north}, S=${boundingBox.south}, E=${boundingBox.east}, W=${boundingBox.west}, Zoom: $minZoom-$maxZoom"
            )

            // Determine what phase to resume from
            val resumePhase = determineResumePhase(areaId)
            Log.d(TAG, "Resuming download from phase: $resumePhase")

            // Check if we've been resumed from paused state - if so, continue existing download
            // Note: Resume from pause should continue with the current phase, not restart determination

            // Update service progress - starting download
            // Note: We don't send an initial update with all zeros as this can cause stage jumping
            // Instead, we'll wait until we have actual totals to report

            // Use offline database for all downloads
            val outputFile = File(context.filesDir, OFFLINE_DATABASE_NAME)
            val dbExists = outputFile.exists()
            Log.d(TAG, "Using database file: ${outputFile.absolutePath}, exists: $dbExists")

            db = SQLiteDatabase.openOrCreateDatabase(outputFile, null)

            // Initialize MBTiles schema only if database is new
            if (!dbExists) {
                Log.d(TAG, "Initializing new MBTiles schema")
                initializeMbtilesSchema(db)
            }

            // Calculate total tiles to download
            val totalBasemapTiles = calculateTotalTiles(
                boundingBox, minZoom, min(maxZoom, MAX_BASEMAP_ZOOM)
            )
            val totalValhallaTiles = ValhallaTileUtils.tilesForBoundingBox(boundingBox).size

            Log.d(TAG, "Total basemap tiles to download: $totalBasemapTiles")
            Log.d(TAG, "Total valhalla tiles to download: $totalValhallaTiles")

            // Get current progress counts for accurate resume
            val downloadedBasemapTiles =
                downloadedTileDao.getDownloadedTileCountForAreaAndType(areaId, TileType.BASEMAP)
            val downloadedValhallaTiles =
                downloadedTileDao.getDownloadedTileCountForAreaAndType(areaId, TileType.VALHALLA)

            Log.d(
                TAG,
                "Already downloaded: $downloadedBasemapTiles basemap tiles, $downloadedValhallaTiles valhalla tiles"
            )

            val currentStage = if (downloadedBasemapTiles != totalBasemapTiles) {
                TileDownloadForegroundService.DownloadStage.BASEMAP
            } else if (downloadedValhallaTiles != totalValhallaTiles) {
                TileDownloadForegroundService.DownloadStage.VALHALLA
            } else {
                TileDownloadForegroundService.DownloadStage.PROCESSING
            }
            val stageProgress = when (currentStage) {
                TileDownloadForegroundService.DownloadStage.BASEMAP -> downloadedBasemapTiles
                TileDownloadForegroundService.DownloadStage.VALHALLA -> downloadedValhallaTiles
                TileDownloadForegroundService.DownloadStage.PROCESSING -> 0
            }
            val stageTotal = when (currentStage) {
                TileDownloadForegroundService.DownloadStage.BASEMAP -> totalBasemapTiles
                TileDownloadForegroundService.DownloadStage.VALHALLA -> totalValhallaTiles
                TileDownloadForegroundService.DownloadStage.PROCESSING -> 1
            }

            // Update service progress with current totals and progress
            serviceBinder?.getService()?.updateProgress(
                areaId = areaId,
                areaName = name,
                currentStage = currentStage,
                stageProgress = stageProgress,
                stageTotal = stageTotal,
                isCompleted = false,
                hasError = false
            )

            // Log the number of tiles already in the database
            val existingTileCount = getTileCount(db)
            Log.d(TAG, "Existing tiles in database: $existingTileCount")

            tileProcessor?.beginTileProcessing()
            Log.d(TAG, "Tile processor initialized")

            // Download basemap tiles if needed (skip if already complete or resuming after)
            if (resumePhase == DownloadStatus.DOWNLOADING_BASEMAP) {
                Log.d(
                    TAG, "Starting/continuing basemap tile download for area: $name (ID: $areaId)"
                )

                // Update status to DOWNLOADING_BASEMAP
                val downloadingArea = offlineAreaDao.getOfflineAreaById(areaId)
                if (downloadingArea != null) {
                    val updatedArea =
                        downloadingArea.copy(status = DownloadStatus.DOWNLOADING_BASEMAP)
                    offlineAreaDao.updateOfflineArea(updatedArea)
                }

                basemapResult = downloadBasemapTiles(
                    boundingBox, minZoom, min(maxZoom, MAX_BASEMAP_ZOOM), areaId, name
                )

                Log.d(
                    TAG,
                    "Basemap download complete: ${basemapResult.first} new tiles downloaded, ${basemapResult.second} failed"
                )

                // Mark basemap phase as complete
                val basemapCompleteArea = offlineAreaDao.getOfflineAreaById(areaId)
                if (basemapCompleteArea != null) {
                    val updatedArea =
                        basemapCompleteArea.copy(status = DownloadStatus.DOWNLOADING_VALHALLA)
                    offlineAreaDao.updateOfflineArea(updatedArea)
                    Log.d(TAG, "Updated area $areaId status to DOWNLOADING_VALHALLA")
                }

                // Update progress to show basemap completion
                // Ensure we maintain consistent progress values to prevent stage jumping
                serviceBinder?.getService()?.updateProgress(
                    areaId = areaId,
                    areaName = name,
                    currentStage = TileDownloadForegroundService.DownloadStage.VALHALLA,
                    stageProgress = 0,
                    stageTotal = totalValhallaTiles,
                    isCompleted = false,
                    hasError = false
                )
            } else {
                Log.d(TAG, "Skipping basemap download for area $areaId (already completed)")
            }

            // Download Valhalla tiles (always attempted after basemap or when resuming from Valhalla phase)
            if (resumePhase == DownloadStatus.DOWNLOADING_BASEMAP || resumePhase == DownloadStatus.DOWNLOADING_VALHALLA) {

                Log.d(
                    TAG, "Starting/continuing Valhalla tile download for area: $name (ID: $areaId)"
                )

                // Update status to DOWNLOADING_VALHALLA if not already
                val currentArea = offlineAreaDao.getOfflineAreaById(areaId)
                if (currentArea != null && currentArea.status != DownloadStatus.DOWNLOADING_VALHALLA) {
                    val updatedArea = currentArea.copy(status = DownloadStatus.DOWNLOADING_VALHALLA)
                    offlineAreaDao.updateOfflineArea(updatedArea)
                }

                valhallaResult = downloadValhallaTiles(
                    boundingBox, areaId, db!!, name
                )

                Log.d(
                    TAG,
                    "Valhalla download complete: ${valhallaResult.first} new tiles downloaded, ${valhallaResult.second} failed"
                )
            } else {
                Log.d(TAG, "Skipping Valhalla download for area $areaId (already completed)")
            }

            // Note: Tiles are already inserted into database during download process
            // No need for additional batch insert here

            // Log the number of tiles in the database after download
            val finalTileCount = getTileCount(db)
            Log.d(TAG, "Final tiles in database after download: $finalTileCount")

            // Store area metadata
            Log.d(TAG, "Storing area metadata for $areaId")
            storeAreaMetadata(db, areaId, boundingBox, minZoom, maxZoom, name)

            db.close()
            db = null

            // Get the actual file size
            val fileSize = outputFile.length()

            val totalBasemapDownloaded = basemapResult.first + downloadedBasemapTiles
            val totalValhallaDownloaded = valhallaResult.first + downloadedValhallaTiles

            Log.d(
                TAG,
                "Tile download completed. $totalBasemapDownloaded/${totalBasemapTiles} basemap tiles, $totalValhallaDownloaded/${totalValhallaTiles} valhalla tiles downloaded. File size: $fileSize bytes"
            )

            // Update offline area status to PROCESSING
            val area = offlineAreaDao.getOfflineAreaById(areaId)
            if (area != null) {
                val processingArea = area.copy(
                    status = DownloadStatus.PROCESSING_GEOCODER, fileSize = fileSize
                )
                offlineAreaDao.updateOfflineArea(processingArea)
            }

            // Update service progress - downloads completed, now processing
            // Ensure we maintain consistent progress values to prevent stage jumping
            serviceBinder?.getService()?.updateProgress(
                areaId = areaId,
                areaName = name,
                currentStage = TileDownloadForegroundService.DownloadStage.PROCESSING,
                stageProgress = 0,
                stageTotal = 1, // This is a bad estimate obviously but all that matters for now is that we are starting from 0%
                isCompleted = false,
                hasError = false
            )

            // Start tile processing phase
            Log.d(TAG, "Starting tile processing phase for area $areaId")
            processDownloadedTiles(areaId)

            // Update offline area status to COMPLETED
            val completedArea = offlineAreaDao.getOfflineAreaById(areaId)
            if (completedArea != null) {
                val finalArea = completedArea.copy(
                    status = DownloadStatus.COMPLETED, fileSize = fileSize
                )
                offlineAreaDao.updateOfflineArea(finalArea)
            }

            // Update service progress - processing completed (unified)
            serviceBinder?.getService()?.updateProgress(
                areaId = areaId,
                areaName = name,
                currentStage = null,
                stageProgress = 0,
                stageTotal = 0,
                isCompleted = true,
                hasError = false
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error downloading tiles for area $name (ID: $areaId)", e)

            // Update service progress - download failed
            serviceBinder?.getService()?.updateProgress(
                areaId = areaId,
                areaName = name,
                currentStage = null,
                stageProgress = 0,
                stageTotal = 0,
                isCompleted = true,
                hasError = true
            )

            // Update offline area status
            val area = offlineAreaDao.getOfflineAreaById(areaId)
            if (area != null) {
                val updatedArea = area.copy(
                    status = DownloadStatus.FAILED, fileSize = 0L
                )
                offlineAreaDao.updateOfflineArea(updatedArea)
            }
        } finally {
            tileProcessor?.endTileProcessing()
            Log.d(TAG, "Tile processing completed")
            // Close database if it's open
            try {
                db?.close()
            } catch (closeException: Exception) {
                Log.e(TAG, "Error closing database", closeException)
            }
        }
    }

    /**
     * Download basemap tiles for the given bounds
     */
    private suspend fun downloadBasemapTiles(
        boundingBox: BoundingBox, minZoom: Int, maxZoom: Int, areaId: String, areaName: String
    ): Pair<Int, Int> {
        var db: SQLiteDatabase? = null
        val downloadedCount = AtomicInteger(0)
        val failedCount = AtomicInteger(0)

        try {
            // Calculate total tiles
            val totalTiles = calculateTotalTiles(boundingBox, minZoom, maxZoom)

            // Materialize all tile coordinates first to simplify processing
            val tileCoordinates =
                generateTileCoordinates(boundingBox, minZoom, maxZoom)

            Log.d(TAG, "Total basemap tiles to process: ${tileCoordinates.size}")

            // Validate consistency between expected tiles and existing tiles in database
            val existingTileCount =
                downloadedTileDao.getDownloadedTileCountForAreaAndType(areaId, TileType.BASEMAP)
            Log.d(TAG, "Found $existingTileCount existing basemap tiles for area $areaId")

            // Calculate remaining tiles to download (not counting already downloaded ones)
            val remainingTiles = maxOf(0, totalTiles - existingTileCount)
            Log.d(
                TAG,
                "Remaining basemap tiles to download: $remainingTiles (total: $totalTiles, existing: $existingTileCount)"
            )

            // Open MBTiles database for tile insertion
            val outputFile = File(context.filesDir, OFFLINE_DATABASE_NAME)
            val dbExists = outputFile.exists()
            Log.d(TAG, "Opening MBTiles database for tile insertion: ${outputFile.absolutePath}")
            db = SQLiteDatabase.openOrCreateDatabase(outputFile, null)

            // Initialize MBTiles schema only if database is new
            if (!dbExists) {
                Log.d(TAG, "Initializing new MBTiles schema for basemap tiles")
                initializeMbtilesSchema(db)
            }

            for (chunk in tileCoordinates.chunked(MAX_CONCURRENT_DOWNLOADS)) {
                // Process this batch with parallel downloads
                val tileData = processBatch(
                    chunk,
                    areaId,
                    areaName,
                    remainingTiles, // Use remaining tiles instead of total for progress tracking
                    downloadedCount,
                    failedCount
                )

                // Insert the successfully downloaded tiles into MBTiles database
                if (tileData.isNotEmpty()) {
                    Log.d(TAG, "Inserting ${tileData.size} tiles into MBTiles database for chunk")
                    batchInsertTiles(db, tileData, areaId)
                }
            }

            // Final consistency check
            val finalDownloadedCount = downloadedCount.get()
            val finalExistingTileCount =
                downloadedTileDao.getDownloadedTileCountForAreaAndType(areaId, TileType.BASEMAP)
            Log.d(
                TAG,
                "Downloaded $finalDownloadedCount new tiles, total for area: $finalExistingTileCount"
            )

            return Pair(finalDownloadedCount, failedCount.get())
        } finally {
            // Close database
            try {
                db?.close()
            } catch (closeException: Exception) {
                Log.e(TAG, "Error closing MBTiles database in downloadBasemapTiles", closeException)
            }
        }
    }

    /**
     * Download Valhalla tiles for the given bounds
     */
    private suspend fun downloadValhallaTiles(
        boundingBox: BoundingBox,
        areaId: String,
        db: SQLiteDatabase,
        areaName: String
    ): Pair<Int, Int> {
        // Get Valhalla tiles for the bounding box
        val valhallaTiles = ValhallaTileUtils.tilesForBoundingBox(boundingBox)
        val totalValhallaTiles = valhallaTiles.size
        var downloadedCount = 0
        var failedCount = 0

        Log.d(TAG, "Total Valhalla tiles to download: $totalValhallaTiles")

        // Validate consistency between expected tiles and existing tiles in database
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery(
                "SELECT COUNT(*) FROM valhalla_tiles WHERE area_id = ?", arrayOf(areaId)
            )
            if (cursor.moveToFirst()) {
                val existingValhallaTileCount = cursor.getInt(0)
                Log.d(
                    TAG, "Found $existingValhallaTileCount existing Valhalla tiles for area $areaId"
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking existing Valhalla tiles for area $areaId", e)
        } finally {
            cursor?.close()
        }

        // Create valhalla tiles directory
        val valhallaTilesDir = File(context.filesDir, "valhalla_tiles")
        if (!valhallaTilesDir.exists()) {
            valhallaTilesDir.mkdirs()
        }

        // Process Valhalla tiles sequentially (one at a time to avoid memory issues)
        for ((hierarchyLevel, tileIndex) in valhallaTiles) {
            // Check if this Valhalla tile already exists in the database
            var cursor: Cursor? = null
            var tileExists = false
            try {
                cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM valhalla_tiles WHERE hierarchy_level = ? AND tile_index = ? AND area_id = ?",
                    arrayOf(hierarchyLevel.toString(), tileIndex.toString(), areaId)
                )
                if (cursor.moveToFirst() && cursor.getInt(0) > 0) {
                    tileExists = true
                }
            } catch (e: Exception) {
                Log.w(
                    TAG,
                    "Error checking existing Valhalla tile $hierarchyLevel/$tileIndex for area $areaId",
                    e
                )
            } finally {
                cursor?.close()
            }

            // If tile already exists, skip downloading
            if (tileExists) {
                Log.v(
                    TAG,
                    "Skipping already downloaded Valhalla tile $hierarchyLevel/$tileIndex for area $areaId"
                )
                // Update service progress without incrementing counters
                serviceBinder?.getService()?.updateProgress(
                    areaId = areaId,
                    areaName = areaName,
                    currentStage = TileDownloadForegroundService.DownloadStage.VALHALLA,
                    stageProgress = downloadedCount,
                    stageTotal = totalValhallaTiles,
                    isCompleted = false,
                    hasError = false
                )
                continue // Skip to next tile
            }

            val (success, filePath) = downloadValhallaTile(
                hierarchyLevel,
                tileIndex,
            )
            if (success && filePath != null) {
                // Store tile reference in database
                storeValhallaTileReference(db, hierarchyLevel, tileIndex, filePath, areaId)

                // Update progress
                downloadedCount++

                // Update service progress
                serviceBinder?.getService()?.updateProgress(
                    areaId = areaId,
                    areaName = areaName,
                    currentStage = TileDownloadForegroundService.DownloadStage.VALHALLA,
                    stageProgress = downloadedCount,
                    stageTotal = totalValhallaTiles,
                    isCompleted = false,
                    hasError = false
                )
            } else {
                failedCount++
            }
        }

        // Final consistency check
        try {
            cursor = db.rawQuery(
                "SELECT COUNT(*) FROM valhalla_tiles WHERE area_id = ?", arrayOf(areaId)
            )
            if (cursor.moveToFirst()) {
                val finalValhallaTileCount = cursor.getInt(0)
                Log.d(TAG, "Final Valhalla tiles for area $areaId: $finalValhallaTileCount")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking final Valhalla tiles for area $areaId", e)
        } finally {
            cursor?.close()
        }

        return Pair(downloadedCount, failedCount)
    }

    /**
     * Download a single Valhalla tile and save it to disk using streaming to avoid OOM
     */
    private suspend fun downloadValhallaTile(
        hierarchyLevel: Int,
        tileIndex: Int,
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        var fileOutputStream: FileOutputStream? = null
        try {
            val url = ValhallaTileUtils.getTileUrl(
                "https://cardinaldata.airmail.rs/valhalla-250825", hierarchyLevel, tileIndex
            )

            Log.v(TAG, "Downloading Valhalla tile $hierarchyLevel/$tileIndex from $url")

            // Create file path for the tile
            val tileFile = ValhallaTileUtils.getLocalTileFilePath(
                File("${context.filesDir}/valhalla_tiles/"), hierarchyLevel, tileIndex
            )

            // Use streaming approach to avoid loading entire tile into memory
            val statement = httpClient.prepareGet(url)
            val totalBytes = statement.execute { response ->
                // Check response code
                if (response.status.value != 200) {
                    Log.e(
                        TAG,
                        "Error downloading Valhalla tile $hierarchyLevel/$tileIndex: HTTP ${response.status}"
                    )
                    throw Exception("HTTP ${response.status.value}: ${response.status.description}")
                }

                // Get the response channel for streaming
                val channel = response.bodyAsChannel()
                fileOutputStream = FileOutputStream(tileFile)

                // Read and write in chunks to avoid OOM
                val buffer = ByteArray(8192) // 8KB buffer
                var totalBytesRead = 0L

                while (true) {
                    val bytesRead = channel.readAvailable(buffer)
                    if (bytesRead == -1) break // End of stream

                    fileOutputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                }

                totalBytesRead
            }

            Log.v(
                TAG,
                "Downloaded Valhalla tile $hierarchyLevel/$tileIndex, size: $totalBytes bytes, saved to: ${tileFile.absolutePath}"
            )

            Pair(true, tileFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading Valhalla tile $hierarchyLevel/$tileIndex via HTTP", e)
            Pair(false, null)
        } finally {
            // Ensure file output stream is closed
            try {
                fileOutputStream?.close()
            } catch (closeException: Exception) {
                Log.w(
                    TAG,
                    "Error closing file output stream for Valhalla tile $hierarchyLevel/$tileIndex",
                    closeException
                )
            }
        }
    }

    /**
     * Store Valhalla tile reference in database
     */
    private fun storeValhallaTileReference(
        db: SQLiteDatabase, hierarchyLevel: Int, tileIndex: Int, filePath: String, areaId: String
    ) {
        try {
            val statement = db.compileStatement(
                "INSERT OR REPLACE INTO valhalla_tiles (hierarchy_level, tile_index, file_path, area_id) VALUES (?, ?, ?, ?)"
            )
            statement.bindLong(1, hierarchyLevel.toLong())
            statement.bindLong(2, tileIndex.toLong())
            statement.bindString(3, filePath)
            statement.bindString(4, areaId)
            statement.executeInsert()
            statement.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error storing Valhalla tile reference for $hierarchyLevel/$tileIndex", e)
        }
    }

    /**
     * Calculate total number of tiles for all zoom levels
     */
    fun calculateTotalTiles(
        boundingBox: BoundingBox, minZoom: Int, maxZoom: Int
    ): Int {
        var totalTiles = 0
        for (zoom in minZoom..maxZoom) {
            val (minX, maxX, minY, maxY) = calculateTileRange(boundingBox, zoom)
            val zoomTileCount = (maxX - minX + 1) * (maxY - minY + 1)
            totalTiles += zoomTileCount
            Log.d(
                TAG, "Zoom $zoom: tiles from ($minX,$minY) to ($maxX,$maxY), count: $zoomTileCount"
            )
        }
        return totalTiles
    }

    /**
     * Generate all tile coordinates for the given bounds and zoom levels
     */
    private fun generateTileCoordinates(
        boundingBox: BoundingBox, minZoom: Int, maxZoom: Int
    ): List<Triple<Int, Int, Int>> {
        val tileCoordinates = mutableListOf<Triple<Int, Int, Int>>()

        for (zoom in minZoom..maxZoom) {
            val (minX, maxX, minY, maxY) = calculateTileRange(boundingBox, zoom)

            // Collect all tile coordinates for this zoom level
            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    tileCoordinates.add(Triple(zoom, x, y))
                }
            }
        }

        return tileCoordinates
    }

    /**
     * Process a batch of tiles
     */
    private suspend fun processBatch(
        chunk: List<Triple<Int, Int, Int>>,
        areaId: String,
        areaName: String,
        totalTiles: Int,
        downloadedCount: AtomicInteger,
        failedCount: AtomicInteger
    ): List<Triple<Int, Pair<Int, Int>, ByteArray>> {
        val results = mutableListOf<Triple<Int, Pair<Int, Int>, ByteArray>>()

        // Process this batch sequentially (one tile at a time)
        for ((z, xCoord, yCoord) in chunk) {
            // Check if this tile has already been downloaded for this area
            val tileId = "basemap_${areaId}_${z}_${xCoord}_${yCoord}"
            val existingTile = downloadedTileDao.getTileById(tileId)

            if (existingTile != null) {
                // Tile already downloaded, skip WITHOUT incrementing progress
                Log.v(
                    TAG, "Skipping already downloaded tile $z/$xCoord/$yCoord for area $areaId"
                )
                // Don't increment downloadedCount here - only increment for actual new downloads

                // Update service progress with current count (not incremented)
                serviceBinder?.getService()?.updateProgress(
                    areaId = areaId,
                    areaName = areaName,
                    currentStage = TileDownloadForegroundService.DownloadStage.BASEMAP,
                    stageProgress = downloadedCount.get(),
                    stageTotal = totalTiles,
                    isCompleted = false,
                    hasError = false
                )

                // Skip this tile - don't add to results since it was already downloaded
                continue
            }

            val (success, data) = downloadTile(z, xCoord, yCoord, areaId)
            if (success && data != null) {
                // Record this tile as downloaded
                val downloadedTile = DownloadedTile(
                    id = tileId,
                    areaId = areaId,
                    tileType = TileType.BASEMAP,
                    downloadTimestamp = System.currentTimeMillis(),
                    retryCount = 0,
                    zoom = z,
                    tileX = xCoord,
                    tileY = yCoord,
                    hierarchyLevel = null,
                    tileIndex = null
                )
                downloadedTileDao.insertTile(downloadedTile)

                // Update progress
                val currentProgress = downloadedCount.incrementAndGet()

                // Update service progress
                serviceBinder?.getService()?.updateProgress(
                    areaId = areaId,
                    areaName = areaName,
                    currentStage = TileDownloadForegroundService.DownloadStage.BASEMAP,
                    stageProgress = currentProgress,
                    stageTotal = totalTiles,
                    isCompleted = false,
                    hasError = false
                )

                results.add(Triple(z, Pair(xCoord, yCoord), data))
            } else {
                // Track failed tiles and implement retry logic
                val existingFailedTile = downloadedTileDao.getTileById(tileId)
                val retryCount = (existingFailedTile?.retryCount ?: 0) + 1

                if (retryCount < MAX_RETRY_COUNT) {
                    // Record the failed attempt for future retry
                    val failedTile = DownloadedTile(
                        id = tileId,
                        areaId = areaId,
                        tileType = TileType.BASEMAP,
                        downloadTimestamp = System.currentTimeMillis(),
                        retryCount = retryCount,
                        zoom = z,
                        tileX = xCoord,
                        tileY = yCoord,
                        hierarchyLevel = null,
                        tileIndex = null
                    )
                    downloadedTileDao.insertTile(failedTile)
                    Log.w(
                        TAG,
                        "Failed to download tile $z/$xCoord/$yCoord for area $areaId (attempt $retryCount/$MAX_RETRY_COUNT)"
                    )
                } else {
                    Log.e(
                        TAG,
                        "Giving up on tile $z/$xCoord/$yCoord for area $areaId after $retryCount attempts"
                    )
                }

                failedCount.incrementAndGet()
                // Don't add failed tiles to results
            }
        }

        return results
    }

    /**
     * Initialize the MBTiles database schema
     */
    private fun initializeMbtilesSchema(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS metadata (
                name TEXT,
                value TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS tiles (
                zoom_level INTEGER,
                tile_column INTEGER,
                tile_row INTEGER,
                tile_data BLOB,
                area_id TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS valhalla_tiles (
                hierarchy_level INTEGER,
                tile_index INTEGER,
                file_path TEXT,
                area_id TEXT
            )
            """.trimIndent()
        )

        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS tile_index ON tiles (zoom_level, tile_column, tile_row, area_id)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS valhalla_tile_index ON valhalla_tiles (hierarchy_level, tile_index, area_id)")

        // Insert basic metadata
        db.execSQL("INSERT OR REPLACE INTO metadata (name, value) VALUES ('name', 'Cardinal Maps Offline Areas')")
        db.execSQL("INSERT OR REPLACE INTO metadata (name, value) VALUES ('type', 'baselayer')")
        db.execSQL("INSERT OR REPLACE INTO metadata (name, value) VALUES ('version', '1.0')")
        db.execSQL("INSERT OR REPLACE INTO metadata (name, value) VALUES ('description', 'Offline map tiles for Cardinal Maps')")
        db.execSQL("INSERT OR REPLACE INTO metadata (name, value) VALUES ('format', 'pbf')")
        db.execSQL("INSERT OR REPLACE INTO metadata (name, value) VALUES ('minzoom', '0')")
        db.execSQL("INSERT OR REPLACE INTO metadata (name, value) VALUES ('maxzoom', '14')")
        // Specify that we're using TMS coordinate system
        db.execSQL("INSERT OR REPLACE INTO metadata (name, value) VALUES ('scheme', 'tms')")
    }

    /**
     * Download a single tile and return its data
     */
    private suspend fun downloadTile(
        zoom: Int, x: Int, y: Int, layer: String
    ): Pair<Boolean, ByteArray?> = withContext(Dispatchers.IO) {
        try {
            // Build the URL for the tile
            val urlTemplate = context.getString(R.string.tile_url_template)
            val url = urlTemplate.replace("{z}", zoom.toString()).replace("{x}", x.toString())
                .replace("{y}", y.toString())

            Log.v(TAG, "Downloading tile $layer/$zoom/$x/$y from $url")

            // Use ktor to get the tile data
            val response = httpClient.get(url)

            // Check response code
            if (response.status.value != 200) {
                Log.e(TAG, "Error downloading tile $layer/$zoom/$x/$y: HTTP ${response.status}")
                return@withContext Pair(false, null)
            }

            val data = response.body<ByteArray>()

            Log.v(
                TAG,
                "Downloaded tile $layer/$zoom/$x/$y, size: ${data.size} bytes, status: ${response.status}"
            )

            // Tile processing is now postponed until after all downloads complete
            // Processing will happen in batch during the PROCESSING phase

            Pair(true, data)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading tile $layer/$zoom/$x/$y via HTTP", e)
            Pair(false, null)
        }
    }

    /**
     * Batch insert tiles into the database using a transaction
     */
    private fun batchInsertTiles(
        db: SQLiteDatabase, tilesData: List<Triple<Int, Pair<Int, Int>, ByteArray>>, areaId: String
    ) {
        if (tilesData.isEmpty()) {
            Log.d(TAG, "No tiles to insert")
            return
        }

        Log.d(TAG, "Starting batch insert of ${tilesData.size} tiles")

        db.beginTransaction()
        try {
            // Pre-compile the insert statement for better performance
            val insertStatement = db.compileStatement(
                "INSERT OR REPLACE INTO tiles (zoom_level, tile_column, tile_row, tile_data, area_id) VALUES (?, ?, ?, ?, ?)"
            )

            try {
                for ((zoom, coords, data) in tilesData) {
                    val (x, y) = coords
                    val tmsY = (2.0.pow(zoom.toDouble()) - 1 - y).toLong()
                    insertStatement.bindLong(1, zoom.toLong())
                    insertStatement.bindLong(2, x.toLong())
                    insertStatement.bindLong(3, tmsY.toLong())
                    insertStatement.bindBlob(4, data)
                    insertStatement.bindString(5, areaId)
                    insertStatement.executeInsert()
                    insertStatement.clearBindings()
                }

                db.setTransactionSuccessful()
                Log.d(TAG, "Successfully inserted ${tilesData.size} tiles in transaction")
            } finally {
                insertStatement.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during batch insert of tiles", e)
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Store metadata for an offline area
     */
    private fun storeAreaMetadata(
        db: SQLiteDatabase,
        areaId: String,
        boundingBox: BoundingBox,
        minZoom: Int,
        maxZoom: Int,
        name: String
    ) {
        try {
            // Create areas table if it doesn't exist
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS areas (
                    area_id TEXT PRIMARY KEY,
                    name TEXT,
                    north REAL,
                    south REAL,
                    east REAL,
                    west REAL,
                    min_zoom INTEGER,
                    max_zoom INTEGER,
                    download_date INTEGER
                )
                """.trimIndent()
            )

            // Insert or update area metadata
            val statement = db.compileStatement(
                """
                INSERT OR REPLACE INTO areas 
                (area_id, name, north, south, east, west, min_zoom, max_zoom, download_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            )
            statement.bindString(1, areaId)
            statement.bindString(2, name)
            statement.bindDouble(3, boundingBox.north)
            statement.bindDouble(4, boundingBox.south)
            statement.bindDouble(5, boundingBox.east)
            statement.bindDouble(6, boundingBox.west)
            statement.bindLong(7, minZoom.toLong())
            statement.bindLong(8, maxZoom.toLong())
            statement.bindLong(9, System.currentTimeMillis())
            statement.executeInsert()
            statement.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error storing area metadata for $areaId", e)
        }
    }

    /**
     * Delete tiles for a specific area ID from the database
     * Only deletes tiles that are exclusively used by this area (no shared tiles)
     */
    fun deleteTilesForArea(areaId: String): Boolean {
        var db: SQLiteDatabase? = null
        try {
            Log.d(TAG, "Starting tile deletion for area ID: $areaId")

            // Open the offline database
            val outputFile = File(context.filesDir, OFFLINE_DATABASE_NAME)
            if (!outputFile.exists()) {
                Log.d(TAG, "Offline database file does not exist, nothing to delete")
                return true
            }

            db = SQLiteDatabase.openDatabase(
                outputFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE
            )

            // First, get all tiles for this area
            val tilesToDelete = getTilesForArea(db, areaId)
            Log.d(TAG, "Found ${tilesToDelete.size} tiles for area ID: $areaId")

            // For each tile, check if it's shared with other areas
            var actuallyDeletedTiles = 0
            var sharedTiles = 0
            for (tile in tilesToDelete) {
                // Check if this tile is used by other areas
                val isShared = isTileSharedWithOtherAreas(db, tile, areaId)
                if (!isShared) {
                    // Tile is not shared with other areas, we can delete it
                    val deleted = deleteTile(db, tile, areaId)
                    actuallyDeletedTiles += deleted
                    Log.v(
                        TAG,
                        "Deleted tile ${tile.zoomLevel}/${tile.tileColumn}/${tile.tileRow} for area ID: $areaId"
                    )
                } else {
                    sharedTiles++
                    Log.v(
                        TAG,
                        "Skipping shared tile ${tile.zoomLevel}/${tile.tileColumn}/${tile.tileRow} for area ID: $areaId"
                    )
                }
            }

            Log.d(
                TAG,
                "Deleted $actuallyDeletedTiles tiles for area ID: $areaId (shared tiles: $sharedTiles, total: ${tilesToDelete.size})"
            )

            // Delete Valhalla tiles for this area
            val valhallaTilesToDelete = getValhallaTilesForArea(db, areaId)
            Log.d(TAG, "Found ${valhallaTilesToDelete.size} Valhalla tiles for area ID: $areaId")

            var actuallyDeletedValhallaTiles = 0
            var sharedValhallaTiles = 0
            for (valhallaTile in valhallaTilesToDelete) {
                // Check if this Valhalla tile is used by other areas
                val isShared = isValhallaTileSharedWithOtherAreas(db, valhallaTile, areaId)
                if (!isShared) {
                    // Delete the physical file
                    try {
                        val file = File(valhallaTile.filePath)
                        if (file.exists() && file.delete()) {
                            Log.v(TAG, "Deleted Valhalla tile file: ${valhallaTile.filePath}")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error deleting Valhalla tile file: ${valhallaTile.filePath}", e)
                    }

                    // Delete from database
                    val deleted = deleteValhallaTile(db, valhallaTile, areaId)
                    actuallyDeletedValhallaTiles += deleted
                    Log.v(
                        TAG,
                        "Deleted Valhalla tile ${valhallaTile.hierarchyLevel}/${valhallaTile.tileIndex} for area ID: $areaId"
                    )
                } else {
                    sharedValhallaTiles++
                    Log.v(
                        TAG,
                        "Skipping shared Valhalla tile ${valhallaTile.hierarchyLevel}/${valhallaTile.tileIndex} for area ID: $areaId"
                    )
                }
            }

            Log.d(
                TAG,
                "Deleted $actuallyDeletedValhallaTiles Valhalla tiles for area ID: $areaId (shared tiles: $sharedValhallaTiles, total: ${valhallaTilesToDelete.size})"
            )

            // Also delete the area metadata
            val deletedMetadata = db.delete("areas", "area_id = ?", arrayOf(areaId))
            Log.d(TAG, "Deleted $deletedMetadata area metadata entries for area ID: $areaId")

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting tiles for area ID: $areaId", e)
            return false
        } finally {
            // Close database if it's open
            try {
                db?.close()
            } catch (closeException: Exception) {
                Log.e(TAG, "Error closing database", closeException)
            }
        }
    }

    /**
     * Get all tiles for a specific area
     */
    private fun getTilesForArea(db: SQLiteDatabase, areaId: String): List<TileCoordinates> {
        val tiles = mutableListOf<TileCoordinates>()
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery(
                "SELECT zoom_level, tile_column, tile_row FROM tiles WHERE area_id = ?",
                arrayOf(areaId)
            )
            while (cursor.moveToNext()) {
                val zoomLevel = cursor.getInt(0)
                val tileColumn = cursor.getInt(1)
                val tileRow = cursor.getInt(2)
                tiles.add(TileCoordinates(zoomLevel, tileColumn, tileRow))
            }
        } finally {
            cursor?.close()
        }
        return tiles
    }

    /**
     * Check if a tile is shared with other areas
     */
    private fun isTileSharedWithOtherAreas(
        db: SQLiteDatabase, tile: TileCoordinates, areaId: String
    ): Boolean {
        var cursor: Cursor? = null
        return try {
            cursor = db.rawQuery(
                "SELECT COUNT(*) FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ? AND area_id != ?",
                arrayOf(
                    tile.zoomLevel.toString(),
                    tile.tileColumn.toString(),
                    tile.tileRow.toString(),
                    areaId
                )
            )
            cursor.moveToFirst() && cursor.getInt(0) > 0
        } finally {
            cursor?.close()
        }
    }

    /**
     * Delete a specific tile for an area
     */
    private fun deleteTile(db: SQLiteDatabase, tile: TileCoordinates, areaId: String): Int {
        return db.delete(
            "tiles", "zoom_level = ? AND tile_column = ? AND tile_row = ? AND area_id = ?", arrayOf(
                tile.zoomLevel.toString(),
                tile.tileColumn.toString(),
                tile.tileRow.toString(),
                areaId
            )
        )
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
     * Get the total number of tiles in the database
     */
    private fun getTileCount(db: SQLiteDatabase): Int {
        var cursor: Cursor? = null
        return try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM tiles", null)
            if (cursor.moveToFirst()) {
                cursor.getInt(0)
            } else {
                0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting tile count", e)
            0
        } finally {
            cursor?.close()
        }
    }

    private data class ValhallaTileCoordinates(
        val hierarchyLevel: Int, val tileIndex: Int, val filePath: String
    )

    /**
     * Get all Valhalla tiles for a specific area
     */
    private fun getValhallaTilesForArea(
        db: SQLiteDatabase, areaId: String
    ): List<ValhallaTileCoordinates> {
        val tiles = mutableListOf<ValhallaTileCoordinates>()
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery(
                "SELECT hierarchy_level, tile_index, file_path FROM valhalla_tiles WHERE area_id = ?",
                arrayOf(areaId)
            )
            while (cursor.moveToNext()) {
                val hierarchyLevel = cursor.getInt(0)
                val tileIndex = cursor.getInt(1)
                val filePath = cursor.getString(2)
                tiles.add(ValhallaTileCoordinates(hierarchyLevel, tileIndex, filePath))
            }
        } finally {
            cursor?.close()
        }
        return tiles
    }

    /**
     * Check if a Valhalla tile is shared with other areas
     */
    private fun isValhallaTileSharedWithOtherAreas(
        db: SQLiteDatabase, tile: ValhallaTileCoordinates, areaId: String
    ): Boolean {
        var cursor: Cursor? = null
        return try {
            cursor = db.rawQuery(
                "SELECT COUNT(*) FROM valhalla_tiles WHERE hierarchy_level = ? AND tile_index = ? AND area_id != ?",
                arrayOf(
                    tile.hierarchyLevel.toString(), tile.tileIndex.toString(), areaId
                )
            )
            cursor.moveToFirst() && cursor.getInt(0) > 0
        } finally {
            cursor?.close()
        }
    }

    /**
     * Delete a specific Valhalla tile for an area
     */
    private fun deleteValhallaTile(
        db: SQLiteDatabase, tile: ValhallaTileCoordinates, areaId: String
    ): Int {
        return db.delete(
            "valhalla_tiles", "hierarchy_level = ? AND tile_index = ? AND area_id = ?", arrayOf(
                tile.hierarchyLevel.toString(), tile.tileIndex.toString(), areaId
            )
        )
    }

    /**
     * Process all downloaded tiles in batch after downloads are complete
     * Reads tiles from database to avoid keeping them in memory
     */
    private suspend fun processDownloadedTiles(
        areaId: String
    ) = withContext(Dispatchers.IO) {
        var db: SQLiteDatabase? = null
        try {
            // Open database to read tiles
            val outputFile = File(context.filesDir, OFFLINE_DATABASE_NAME)
            db = SQLiteDatabase.openDatabase(
                outputFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY
            )

            Log.d(TAG, "Starting batch processing of tiles for area $areaId")

            // Query all tiles for this area
            val cursor = db.rawQuery(
                "SELECT zoom_level, tile_column, tile_row, tile_data FROM tiles WHERE area_id = ? AND zoom_level = 14",
                arrayOf(areaId)
            )

            val totalTilesToProcess = cursor.count

            var processedCount = 0
            var failedCount = 0

            try {
                val batchSize = 20 // Process in smaller batches to manage memory
                val tileBatch = mutableListOf<Triple<Int, Pair<Int, Int>, ByteArray>>()
                var totalTilesProcessed = 0

                while (cursor.moveToNext()) {
                    val zoom = cursor.getInt(0)
                    val x = cursor.getInt(1)
                    val y = cursor.getInt(2)
                    val data = cursor.getBlob(3)

                    // Add to batch
                    tileBatch.add(Triple(zoom, Pair(x, y), data))
                    totalTilesProcessed++

                    // Process batch when it reaches the batch size
                    if (tileBatch.size >= batchSize) {
                        val (batchProcessed, batchFailed) = processTileBatch(tileBatch, areaId)
                        processedCount += batchProcessed
                        failedCount += batchFailed
                        tileBatch.clear()

                        // Update progress during processing phase
                        serviceBinder?.getService()?.updateProgress(
                            areaId = areaId,
                            areaName = "",
                            currentStage = TileDownloadForegroundService.DownloadStage.PROCESSING,
                            stageProgress = processedCount,
                            stageTotal = totalTilesToProcess,
                            isCompleted = false,
                            hasError = false
                        )

                        // Small delay between batches to prevent overwhelming the system
                        delay(5)
                    }
                }

                // Process remaining tiles in the last batch
                if (tileBatch.isNotEmpty()) {
                    val (batchProcessed, batchFailed) = processTileBatch(tileBatch, areaId)
                    processedCount += batchProcessed
                    failedCount += batchFailed

                    // Final progress update
                    serviceBinder?.getService()?.updateProgress(
                        areaId = areaId,
                        areaName = "",
                        currentStage = TileDownloadForegroundService.DownloadStage.PROCESSING,
                        stageProgress = processedCount,
                        stageTotal = totalTilesToProcess,
                        isCompleted = false,
                        hasError = false
                    )
                }

                Log.d(
                    TAG, "Tile processing completed: $processedCount processed, $failedCount failed"
                )

            } finally {
                cursor.close()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error during batch tile processing for area $areaId", e)
        } finally {
            // Close database
            try {
                db?.close()
            } catch (closeException: Exception) {
                Log.e(TAG, "Error closing database during processing", closeException)
            }
        }
    }

    /**
     * Process a batch of tiles
     */
    private suspend fun processTileBatch(
        tileBatch: List<Triple<Int, Pair<Int, Int>, ByteArray>>, areaId: String
    ): Pair<Int, Int> {
        var processedCount = 0
        var failedCount = 0

        for ((zoom, coords, data) in tileBatch) {
            try {
                val (x, y) = coords
                tileProcessor?.processTile(data, zoom, x, y)
                processedCount++
                Log.v(TAG, "Processed tile $zoom/$x/$y for area $areaId")
            } catch (e: Exception) {
                Log.w(
                    TAG,
                    "Error processing tile $zoom/${coords.first}/${coords.second} for area $areaId",
                    e
                )
                failedCount++
            }
        }

        return Pair(processedCount, failedCount)
    }
}

/**
 * Convert longitude to tile X coordinate
 */
private fun lonToTileX(lon: Double, zoom: Int): Int {
    return ((lon + 180.0) / 360.0 * (1 shl zoom)).toInt()
}

/**
 * Convert latitude to tile Y coordinate
 */
private fun latToTileY(lat: Double, zoom: Int): Int {
    val latRad = Math.toRadians(lat)
    val n = 2.0.pow(zoom.toDouble())
    return ((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / Math.PI) / 2.0 * n).toInt()
}

data class TileRange(
    val minX: Int, val maxX: Int, val minY: Int, val maxY: Int
)

private data class TileCoordinates(
    val zoomLevel: Int, val tileColumn: Int, val tileRow: Int
)

/**
 * Calculate tile range for a bounding box at a specific zoom level
 */
fun calculateTileRange(
    boundingBox: BoundingBox, zoom: Int
): TileRange {
    // Convert latitude/longitude to tile coordinates using Web Mercator projection
    // Formula:
    // x = (lon + 180) / 360 * 2^zoom
    // y = (1 - ln(tan(lat * π/180) + sec(lat * π/180)) / π) / 2 * 2^zoom

    // Calculate tile coordinates for northwest corner (max latitude, min longitude)
    val nwX = lonToTileX(boundingBox.west, zoom)
    val nwY = latToTileY(boundingBox.north, zoom)

    // Calculate tile coordinates for southeast corner (min latitude, max longitude)
    val seX = lonToTileX(boundingBox.east, zoom)
    val seY = latToTileY(boundingBox.south, zoom)

    // Return the range, ensuring proper min/max values
    // Note: Y coordinates increase downward in tile systems
    return TileRange(
        minX = min(nwX, seX), maxX = max(nwX, seX), minY = min(nwY, seY), maxY = max(nwY, seY)
    )
}