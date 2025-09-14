package earth.maps.cardinal.tileserver

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import earth.maps.cardinal.R
import earth.maps.cardinal.geocoding.TileProcessor
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.*

class TileDownloadService(
    private val context: Context,
    private val tileProcessor: TileProcessor? = null
) {
    private val TAG = "TileDownloadService"
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())
    private var downloadJob: Job? = null
    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation)
    }

    companion object {
        private const val MAX_BASEMAP_ZOOM = 14
        private const val OFFLINE_DATABASE_NAME = "offline_areas.mbtiles"
        private const val MAX_CONCURRENT_DOWNLOADS = 10
    }

    /**
     * Download tiles for a bounding box and zoom range
     */
    fun downloadTiles(
        north: Double,
        south: Double,
        east: Double,
        west: Double,
        minZoom: Int,
        maxZoom: Int,
        areaId: String,
        name: String,
        progressCallback: (progress: Int, total: Int) -> Unit,
        completionCallback: (success: Boolean, fileSize: Long) -> Unit
    ) {
        downloadJob = coroutineScope.launch {
            downloadTilesInternal(
                north, south, east, west,
                minZoom, maxZoom,
                areaId, name,
                progressCallback, completionCallback
            )
        }
    }

    private suspend fun downloadTilesInternal(
        north: Double,
        south: Double,
        east: Double,
        west: Double,
        minZoom: Int,
        maxZoom: Int,
        areaId: String,
        name: String,
        progressCallback: (progress: Int, total: Int) -> Unit,
        completionCallback: (success: Boolean, fileSize: Long) -> Unit
    ) {
        var db: SQLiteDatabase? = null
        try {
            Log.d(TAG, "Starting tile download for area: $name (ID: $areaId)")
            Log.d(TAG, "Bounds: N=$north, S=$south, E=$east, W=$west, Zoom: $minZoom-$maxZoom")

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
            val totalTiles = calculateTotalTiles(north, south, east, west, minZoom, min(maxZoom, MAX_BASEMAP_ZOOM))
            Log.d(TAG, "Total tiles to download: $totalTiles")

            // Log the number of tiles already in the database
            val existingTileCount = getTileCount(db)
            Log.d(TAG, "Existing tiles in database: $existingTileCount")

            tileProcessor?.beginTileProcessing()
            Log.d(TAG, "Tile processor initialized")

            // Process tiles in chunks to maintain bounded memory usage
            val downloadedTilesData = mutableListOf<Triple<Int, Pair<Int, Int>, ByteArray>>()

            // Initialize downloaded count for progress tracking
            val downloadedCount = AtomicInteger(0)
            val failedCount = AtomicInteger(0)

            try {
                // Materialize all tile coordinates first to simplify processing
                val tileCoordinates = generateTileCoordinates(north, south, east, west, minZoom, min(maxZoom, MAX_BASEMAP_ZOOM))

                Log.d(TAG, "Total tiles to process: ${tileCoordinates.size}")

                for (chunk in tileCoordinates.chunked(MAX_CONCURRENT_DOWNLOADS)) {
                    // Process this batch with parallel downloads
                    val results = processBatch(chunk, areaId, progressCallback, totalTiles, downloadedCount, failedCount)
                    // Filter out null results and add to our list
                    downloadedTilesData.addAll(results.filterNotNull())
                }

                // Batch insert all downloaded tiles in a transaction
                batchInsertTiles(db, downloadedTilesData, areaId)

                Log.d(TAG, "Download complete: ${downloadedTilesData.size} tiles downloaded, ${failedCount.get()} failed")
            } finally {
                tileProcessor?.endTileProcessing()
                Log.d(TAG, "Tile processing completed")
            }

            // Log the number of tiles in the database after download
            val finalTileCount = getTileCount(db)
            Log.d(TAG, "Final tiles in database after download: $finalTileCount")

            // Store area metadata
            Log.d(TAG, "Storing area metadata for $areaId")
            storeAreaMetadata(db, areaId, north, south, east, west, minZoom, maxZoom, name)

            db.close()
            db = null

            // Get the actual file size
            val fileSize = outputFile.length()

            Log.d(TAG, "Tile download completed. ${downloadedTilesData.size} tiles downloaded, ${failedCount.get()} failed. File size: $fileSize bytes")
            completionCallback(true, fileSize)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading tiles for area $name (ID: $areaId)", e)
            completionCallback(false, 0L)
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
     * Calculate total number of tiles for all zoom levels
     */
    private fun calculateTotalTiles(
        north: Double,
        south: Double,
        east: Double,
        west: Double,
        minZoom: Int,
        maxZoom: Int
    ): Int {
        var totalTiles = 0
        for (zoom in minZoom..maxZoom) {
            val (minX, maxX, minY, maxY) = calculateTileRange(north, south, east, west, zoom)
            val zoomTileCount = (maxX - minX + 1) * (maxY - minY + 1)
            totalTiles += zoomTileCount
            Log.d(TAG, "Zoom $zoom: tiles from ($minX,$minY) to ($maxX,$maxY), count: $zoomTileCount")
        }
        return totalTiles
    }

    /**
     * Generate all tile coordinates for the given bounds and zoom levels
     */
    private fun generateTileCoordinates(
        north: Double,
        south: Double,
        east: Double,
        west: Double,
        minZoom: Int,
        maxZoom: Int
    ): List<Triple<Int, Int, Int>> {
        val tileCoordinates = mutableListOf<Triple<Int, Int, Int>>()

        for (zoom in minZoom..maxZoom) {
            val (minX, maxX, minY, maxY) = calculateTileRange(north, south, east, west, zoom)

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
        progressCallback: (progress: Int, total: Int) -> Unit,
        totalTiles: Int,
        downloadedCount: AtomicInteger,
        failedCount: AtomicInteger
    ): List<Triple<Int, Pair<Int, Int>, ByteArray>?> {
        // Create a coroutine scope for this batch of downloads
        val batchScope = CoroutineScope(Dispatchers.IO + Job())

        // Process this batch with parallel downloads
        val downloadTasks = chunk.map { (z, xCoord, yCoord) ->
            batchScope.async {
                val (success, data) = downloadTile(z, xCoord, yCoord, areaId)
                if (success && data != null) {
                    // Update progress
                    val currentProgress = downloadedCount.incrementAndGet()
                    progressCallback(currentProgress, totalTiles)
                    
                    // Convert XYZ to TMS coordinate system for MBTiles
                    // MBTiles uses TMS (Tile Map Service) coordinate system where Y=0 is at the bottom
                    // Most map libraries use XYZ coordinate system where Y=0 is at the top
                    // Conversion formula: TMS_Y = 2^zoom - 1 - XYZ_Y
                    val tmsY = (2.0.pow(z.toDouble()) - 1 - yCoord).toInt()
                    Triple(z, Pair(xCoord, tmsY), data)
                } else {
                    // Track failed tiles
                    failedCount.incrementAndGet()
                    null
                }
            }
        }

        // Wait for all downloads in this batch to complete
        return downloadTasks.awaitAll()
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

        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS tile_index ON tiles (zoom_level, tile_column, tile_row, area_id)")

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
     * Calculate tile range for a bounding box at a specific zoom level
     */
    fun calculateTileRange(
        north: Double,
        south: Double,
        east: Double,
        west: Double,
        zoom: Int
    ): TileRange {
        // Convert latitude/longitude to tile coordinates using Web Mercator projection
        // Formula: 
        // x = (lon + 180) / 360 * 2^zoom
        // y = (1 - ln(tan(lat * π/180) + sec(lat * π/180)) / π) / 2 * 2^zoom

        // Calculate tile coordinates for northwest corner (max latitude, min longitude)
        val nwX = lonToTileX(west, zoom)
        val nwY = latToTileY(north, zoom)

        // Calculate tile coordinates for southeast corner (min latitude, max longitude)
        val seX = lonToTileX(east, zoom)
        val seY = latToTileY(south, zoom)

        // Return the range, ensuring proper min/max values
        // Note: Y coordinates increase downward in tile systems
        return TileRange(
            minX = min(nwX, seX),
            maxX = max(nwX, seX),
            minY = min(nwY, seY),
            maxY = max(nwY, seY)
        )
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

    /**
     * Download a single tile and return its data
     */
    private suspend fun downloadTile(
        zoom: Int,
        x: Int,
        y: Int,
        layer: String
    ): Pair<Boolean, ByteArray?> = withContext(Dispatchers.IO) {
        try {
            // Build the URL for the tile
            val urlTemplate = context.getString(R.string.tile_url_template)
            val url = urlTemplate
                .replace("{z}", zoom.toString())
                .replace("{x}", x.toString())
                .replace("{y}", y.toString())

            Log.v(TAG, "Downloading tile $layer/$zoom/$x/$y from $url")

            // Use ktor to get the tile data
            val response = httpClient.get(url)
            val data = response.body<ByteArray>()

            Log.v(
                TAG,
                "Downloaded tile $layer/$zoom/$x/$y, size: ${data.size} bytes, status: ${response.status}"
            )

            // Notify the tile processor if available
            tileProcessor?.let { processor ->
                try {
                    processor.processTile(data, zoom, x, y)
                    Log.v(TAG, "Processed tile $layer/$zoom/$x/$y with tile processor")
                } catch (e: Exception) {
                    Log.w(TAG, "Error processing tile $layer/$zoom/$x/$y in tile processor", e)
                }
            }

            Pair(true, data)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading tile $layer/$zoom/$x/$y via HTTP", e)
            Pair(false, null)
        }
    }

    data class TileRange(
        val minX: Int,
        val maxX: Int,
        val minY: Int,
        val maxY: Int
    )

    private data class TileCoordinates(
        val zoomLevel: Int,
        val tileColumn: Int,
        val tileRow: Int
    )

    /**
     * Batch insert tiles into the database using a transaction
     */
    private fun batchInsertTiles(
        db: SQLiteDatabase,
        tilesData: List<Triple<Int, Pair<Int, Int>, ByteArray>>,
        areaId: String
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
                    val (x, tmsY) = coords
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
        north: Double,
        south: Double,
        east: Double,
        west: Double,
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
            statement.bindDouble(3, north)
            statement.bindDouble(4, south)
            statement.bindDouble(5, east)
            statement.bindDouble(6, west)
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
                outputFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE
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
        var cursor: android.database.Cursor? = null
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
    private fun isTileSharedWithOtherAreas(db: SQLiteDatabase, tile: TileCoordinates, areaId: String): Boolean {
        var cursor: android.database.Cursor? = null
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
            "tiles",
            "zoom_level = ? AND tile_column = ? AND tile_row = ? AND area_id = ?",
            arrayOf(
                tile.zoomLevel.toString(),
                tile.tileColumn.toString(),
                tile.tileRow.toString(),
                areaId
            )
        )
    }

    /**
     * Cancel the current download if one is in progress
     */
    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
    }

    /**
     * Get the total number of tiles in the database
     */
    private fun getTileCount(db: SQLiteDatabase): Int {
        var cursor: android.database.Cursor? = null
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
}
