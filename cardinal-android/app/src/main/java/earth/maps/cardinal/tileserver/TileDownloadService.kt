package earth.maps.cardinal.tileserver

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import earth.maps.cardinal.geocoding.TileProcessor
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.Triple

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
        private const val TILE_URL_TEMPLATE = "https://pmtiles.ellenhp.workers.dev/planet-250825.pmtiles/planet-250825/{z}/{x}/{y}.mvt"
    }

    /**
     * Download tiles for a bounding box and zoom range
     * @param north Northern latitude boundary
     * @param south Southern latitude boundary
     * @param east Eastern longitude boundary
     * @param west Western longitude boundary
     * @param minZoom Minimum zoom level
     * @param maxZoom Maximum zoom level
     * @param areaId Unique identifier for the offline area
     * @param progressCallback Callback to report download progress
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
            var db: SQLiteDatabase? = null
            try {
                // Use offline database for all downloads
                val outputFile = File(context.filesDir, OFFLINE_DATABASE_NAME)
                val dbExists = outputFile.exists()

                db = SQLiteDatabase.openOrCreateDatabase(outputFile, null)

                // Initialize MBTiles schema only if database is new
                if (!dbExists) {
                    initializeMbtilesSchema(db)
                }

                // Calculate tile ranges for each zoom level
                var totalTiles = 0

                for (zoom in minZoom..min(maxZoom, MAX_BASEMAP_ZOOM)) {
                    val (minX, maxX, minY, maxY) = calculateTileRange(
                        north,
                        south,
                        east,
                        west,
                        zoom
                    )
                    totalTiles += (maxX - minX + 1) * (maxY - minY + 1)
                }

                var downloadedTiles = 0
                try {
                    tileProcessor?.beginTileProcessing()
                    // Pre-compile the insert statement for better performance
                    val insertStatement = db.compileStatement(
                        "INSERT OR REPLACE INTO tiles (zoom_level, tile_column, tile_row, tile_data, area_id) VALUES (?, ?, ?, ?, ?)"
                    )

                    try {
                        // Process tiles in a streaming fashion with bounded parallelization
                        // to avoid loading all tile coordinates into memory at once
                        val maxConcurrentDownloads = 10
                        
                        for (zoom in minZoom..min(maxZoom, MAX_BASEMAP_ZOOM)) {
                            val (minX, maxX, minY, maxY) = calculateTileRange(
                                north,
                                south,
                                east,
                                west,
                                zoom
                            )
                            
                            // Process tiles in chunks for this zoom level to maintain bounded memory usage
                            var x = minX
                            while (x <= maxX) {
                                val xEnd = min(x + maxConcurrentDownloads - 1, maxX)
                                
                                // Create a coroutine scope for this batch of downloads
                                val batchScope = CoroutineScope(Dispatchers.IO + Job())
                                
                                // Collect tile coordinates for this batch
                                val batchCoordinates = mutableListOf<Triple<Int, Int, Int>>()
                                for (xBatch in x..xEnd) {
                                    for (y in minY..maxY) {
                                        batchCoordinates.add(Triple(zoom, xBatch, y))
                                    }
                                }
                                
                                // Process this batch with parallel downloads
                                val downloadTasks = batchCoordinates.map { (z, xCoord, yCoord) ->
                                    batchScope.async {
                                        val success = downloadAndStoreTile(
                                            z,
                                            xCoord,
                                            yCoord,
                                            areaId,
                                            insertStatement
                                        )
                                        if (success) {
                                            downloadedTiles++
                                        }
                                        progressCallback(downloadedTiles, totalTiles)
                                    }
                                }
                                
                                // Wait for all downloads in this batch to complete
                                val results = downloadTasks.awaitAll()

                                x = xEnd + 1
                            }
                        }
                    } finally {
                        // Close the statement
                        try {
                            insertStatement.close()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error closing insert statement", e)
                        }
                    }
                } finally {
                    tileProcessor?.endTileProcessing()
                }
                // Store area metadata
                storeAreaMetadata(db, areaId, north, south, east, west, minZoom, maxZoom, name)

                db.close()
                db = null

                // Get the actual file size
                val fileSize = outputFile.length()

                Log.d(
                    TAG,
                    "Tile download completed. $downloadedTiles tiles downloaded. File size: $fileSize bytes"
                )
                completionCallback(true, fileSize)
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading tiles", e)
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
        val n = Math.pow(2.0, zoom.toDouble()).toInt()

        // Handle longitude wrapping (when crossing the antimeridian)
        val normalizedWest = if (west > 180) west - 360 else if (west < -180) west + 360 else west
        val normalizedEast = if (east > 180) east - 360 else if (east < -180) east + 360 else east

        // Calculate tile coordinates for west and east boundaries
        var x1 = n * (normalizedWest + 180.0) / 360.0
        var x2 = n * (normalizedEast + 180.0) / 360.0

        // Handle case where bounding box crosses the antimeridian
        if (normalizedWest > normalizedEast) {
            // Swap x1 and x2 so that x1 is the left boundary and x2 is the right boundary
            val temp = x1
            x1 = x2
            x2 = temp
        }

        // Calculate tile coordinates for north and south boundaries
        // Note: Web Mercator projection - north has smaller y values than south
        val latRad1 = Math.toRadians(north)
        val latRad2 = Math.toRadians(south)
        val y1 = n * (1.0 - Math.log(Math.tan(latRad1) + 1.0 / Math.cos(latRad1)) / Math.PI) / 2.0
        val y2 = n * (1.0 - Math.log(Math.tan(latRad2) + 1.0 / Math.cos(latRad2)) / Math.PI) / 2.0

        // Ensure proper min/max calculation
        // For x: west to east (left to right)
        // Use floor for minX to get the tile containing the west boundary
        // Use ceil for maxX to get the tile containing the east boundary
        val minX = max(0, Math.floor(x1).toInt())
        val maxX = min(n - 1, Math.ceil(x2).toInt())

        // For y: north to south (top to bottom, but in tile coordinates, y increases downward)
        // y1 corresponds to north (smaller value), y2 corresponds to south (larger value)
        // Use floor for minY to get the tile containing the north boundary
        // Use ceil for maxY to get the tile containing the south boundary
        val minY = max(0, Math.floor(y1).toInt())
        val maxY = min(n - 1, Math.ceil(y2).toInt())

        return TileRange(minX, maxX, minY, maxY)
    }

    /**
     * Download and store a single tile
     */
    private suspend fun downloadAndStoreTile(
        zoom: Int,
        x: Int,
        y: Int,
        layer: String,
        insertStatement: android.database.sqlite.SQLiteStatement
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Build the URL for the tile
            val url = TILE_URL_TEMPLATE
                .replace("{z}", zoom.toString())
                .replace("{x}", x.toString())
                .replace("{y}", y.toString())
            
            // Use ktor to get the tile data
            val data = httpClient.get(url).body<ByteArray>()

            // Convert XYZ to TMS coordinate system for MBTiles
            // MBTiles uses TMS (Tile Map Service) coordinate system where Y=0 is at the bottom
            // Most map libraries use XYZ coordinate system where Y=0 is at the top
            // Conversion formula: TMS_Y = 2^zoom - 1 - XYZ_Y
            val tmsY = (2.0.pow(zoom.toDouble()) - 1 - y).toInt()

            // Store in database with area_id
            // Reuse pre-compiled statement for better performance
            insertStatement.bindLong(1, zoom.toLong())
            insertStatement.bindLong(2, x.toLong())
            insertStatement.bindLong(3, tmsY.toLong())
            insertStatement.bindBlob(4, data)
            insertStatement.bindString(5, layer)  // layer parameter now contains the areaId
            insertStatement.executeInsert()
            insertStatement.clearBindings()

            // Notify the tile processor if available
            tileProcessor?.let { processor ->
                try {
                    processor.processTile(data, zoom, x, y)
                } catch (e: Exception) {
                    Log.w(TAG, "Error processing tile $layer/$zoom/$x/$y in tile processor", e)
                }
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading tile $layer/$zoom/$x/$y via HTTP", e)
            false
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
            val tilesToDelete = mutableListOf<TileCoordinates>()
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
                    tilesToDelete.add(TileCoordinates(zoomLevel, tileColumn, tileRow))
                }
            } finally {
                cursor?.close()
            }

            // For each tile, check if it's shared with other areas
            var actuallyDeletedTiles = 0
            for (tile in tilesToDelete) {
                // Check if this tile is used by other areas
                cursor = null
                try {
                    cursor = db.rawQuery(
                        "SELECT COUNT(*) FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ? AND area_id != ?",
                        arrayOf(
                            tile.zoomLevel.toString(),
                            tile.tileColumn.toString(),
                            tile.tileRow.toString(),
                            areaId
                        )
                    )
                    if (cursor.moveToFirst() && cursor.getInt(0) == 0) {
                        // Tile is not shared with other areas, we can delete it
                        val deleted = db.delete(
                            "tiles",
                            "zoom_level = ? AND tile_column = ? AND tile_row = ? AND area_id = ?",
                            arrayOf(
                                tile.zoomLevel.toString(),
                                tile.tileColumn.toString(),
                                tile.tileRow.toString(),
                                areaId
                            )
                        )
                        actuallyDeletedTiles += deleted
                    }
                    // If tile is shared, we don't delete it
                } finally {
                    cursor?.close()
                }
            }

            Log.d(
                TAG,
                "Deleted $actuallyDeletedTiles tiles for area ID: $areaId (some may have been shared and not deleted)"
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
     * Cancel the current download if one is in progress
     */
    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
    }
}
