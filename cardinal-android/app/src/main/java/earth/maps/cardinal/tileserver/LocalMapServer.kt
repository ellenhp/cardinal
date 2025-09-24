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

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import earth.maps.cardinal.R
import earth.maps.cardinal.data.AppPreferences
import earth.maps.cardinal.routing.MultiplexedRoutingService
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.io.File
import java.io.FileOutputStream
import kotlin.math.pow


class LocalMapServer(
    private val context: Context,
    private val appPreferences: AppPreferences,
    private val multiplexedRoutingService: MultiplexedRoutingService,
) {
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? =
        null
    private var port: Int = -1
    private var terrainDatabase: SQLiteDatabase? = null
    private var landcoverDatabase: SQLiteDatabase? = null
    private var basemapDatabase: SQLiteDatabase? = null
    private var offlineAreasDatabase: SQLiteDatabase? = null

    // HTTP client for fetching tiles from the internet
    private val httpClient = HttpClient(io.ktor.client.engine.android.Android) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json()
        }
        install(io.ktor.client.plugins.logging.Logging) {
            level = io.ktor.client.plugins.logging.LogLevel.NONE
        }
        // Configure timeouts
        engine {
            connectTimeout = 10_000
            socketTimeout = 10_000
        }
    }

    fun start() {
        // Initialize the mbtiles databases
        initializeMbtilesDatabases()

        // Find an available port
        val availablePort = findAvailablePort()

        server = embeddedServer(CIO, port = availablePort, host = "127.0.0.1") {
            routing {
                get("/") {
                    call.respondText("Tile Server is running!")
                }

                get("/style_light.json") {
                    try {
                        val styleJson = readAssetFile("style_light.json")
                        val modifiedStyleJson = styleJson.replace("{port}", port.toString())
                        call.respondText(
                            modifiedStyleJson, contentType = ContentType.Application.Json
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading style_light.json", e)
                        call.respondText(
                            "Error reading style_light.json",
                            status = HttpStatusCode.InternalServerError
                        )
                    }
                }

                get("/style_dark.json") {
                    try {
                        val styleJson = readAssetFile("style_dark.json")
                        val modifiedStyleJson = styleJson.replace("{port}", port.toString())
                        call.respondText(
                            modifiedStyleJson, contentType = ContentType.Application.Json
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading style_dark.json", e)
                        call.respondText(
                            "Error reading style_dark.json",
                            status = HttpStatusCode.InternalServerError
                        )
                    }
                }

                // Valhalla-compatible routing endpoint
                post("/route") {
                    try {
                        val requestBody = call.receiveText()
                        Log.d(TAG, "Received routing request: $requestBody")

                        val routeJson = multiplexedRoutingService.getRoute(requestBody)

                        // Return the route response
                        call.respondText(
                            routeJson, contentType = ContentType.Application.Json
                        )

                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing routing request", e)
                        call.respondText(
                            "{\"error\":\"${e.message}\"}",
                            contentType = ContentType.Application.Json,
                            status = HttpStatusCode.InternalServerError
                        )
                    }
                }

                // Serve tiles from terrain.mbtiles.
                get("/terrain/{z}/{x}/{y}.png") {
                    val terrainDatabase = terrainDatabase ?: return@get

                    val z = call.parameters["z"]?.toIntOrNull()
                    val x = call.parameters["x"]?.toLongOrNull()
                    val y = call.parameters["y"]?.toLongOrNull()

                    if (z == null || x == null || y == null) {
                        call.respondText(
                            "Invalid tile coordinates", status = HttpStatusCode.BadRequest
                        )
                        return@get
                    }

                    // MBTiles uses TMS coordinate system, but most map libraries use XYZ
                    // Convert Y coordinate from XYZ to TMS
                    val tmsY = (2.0.pow(z.toDouble()) - 1 - y).toLong()

                    val tileData = getTileData(terrainDatabase, z, x, tmsY)
                    if (tileData != null) {
                        call.respondBytes(tileData, contentType = ContentType.Image.PNG)
                    } else {
                        call.respondBytes(
                            bytes = ByteArray(0),
                            contentType = ContentType.Image.PNG,
                            status = HttpStatusCode.NotFound
                        )
                    }
                }


                // Serve tiles from landcover.mbtiles.
                get("/landcover/{z}/{x}/{y}.pbf") {
                    val landcoverDatabase = landcoverDatabase ?: return@get

                    val z = call.parameters["z"]?.toIntOrNull()
                    val x = call.parameters["x"]?.toLongOrNull()
                    val y = call.parameters["y"]?.toLongOrNull()

                    if (z == null || x == null || y == null) {
                        call.respondText(
                            "Invalid tile coordinates", status = HttpStatusCode.BadRequest
                        )
                        return@get
                    }

                    // MBTiles uses TMS coordinate system, but most map libraries use XYZ
                    // Convert Y coordinate from XYZ to TMS
                    val tmsY = (2.0.pow(z.toDouble()) - 1 - y).toLong()

                    val tileData = getTileData(landcoverDatabase, z, x, tmsY)
                    if (tileData != null) {
                        call.response.header("content-encoding", "gzip")
                        call.respondBytes(tileData, contentType = ContentType.Application.ProtoBuf)
                    } else {
                        call.respondBytes(
                            bytes = ByteArray(0),
                            contentType = ContentType.Application.ProtoBuf,
                            status = HttpStatusCode.NotFound
                        )
                    }
                }

                // Serve tiles from basemap.mbtiles.
                get("/openmaptiles/{z}/{x}/{y}.pbf") {
                    val z = call.parameters["z"]?.toIntOrNull()
                    val x = call.parameters["x"]?.toLongOrNull()
                    val y = call.parameters["y"]?.toLongOrNull()

                    if (z == null || x == null || y == null) {
                        Log.w(TAG, "Invalid tile coordinates: z=$z, x=$x, y=$y")
                        call.respondText(
                            "Invalid tile coordinates", status = HttpStatusCode.BadRequest
                        )
                        return@get
                    }

                    Log.d(TAG, "Requesting tile: /openmaptiles/$z/$x/$y.pbf")

                    // MBTiles uses TMS (Tile Map Service) coordinate system where Y=0 is at the bottom
                    // Most map libraries use XYZ coordinate system where Y=0 is at the top
                    // Convert Y coordinate from XYZ to TMS: TMS_Y = 2^zoom - 1 - XYZ_Y
                    val tmsY = (2.0.pow(z.toDouble()) - 1 - y).toLong()

                    var isGzipped = true

                    val basemapDatabase = basemapDatabase

                    // First try to get tile from built-in database
                    var tileData = if (basemapDatabase != null) {
                        getTileData(basemapDatabase, z, x, tmsY)
                    } else {
                        Log.w(TAG, "Basemap database is null")
                        null
                    }

                    // If not found, try offline databases
                    if (tileData == null) {
                        Log.d(TAG, "Tile not found in basemap database, checking offline databases")
                        tileData = getTileDataFromOfflineDatabases(z, x, y)
                        isGzipped = false
                    } else {
                        Log.d(TAG, "Tile found in basemap database")
                    }

                    if (tileData != null) {
                        Log.d(
                            TAG,
                            "Serving tile /openmaptiles/$z/$x/$y.pbf, size: ${tileData.size} bytes, gzipped: $isGzipped"
                        )
                        // Only set gzip header for built-in database tiles
                        if (isGzipped) {
                            call.response.header("content-encoding", "gzip")
                        }
                        call.respondBytes(tileData, contentType = ContentType.Application.ProtoBuf)
                    } else {
                        // Check if we should fetch from the internet (not in offline mode)
                        val isOfflineMode = isOfflineMode()
                        if (!isOfflineMode) {
                            Log.d(
                                TAG,
                                "Tile not found in local caches, attempting to fetch from internet"
                            )
                            tileData = CoroutineScope(Dispatchers.IO).async {
                                fetchTileFromInternet(
                                    z, x, y
                                )
                            }.await()
                            if (tileData != null) {
                                Log.d(
                                    TAG,
                                    "Successfully fetched tile from internet: /openmaptiles/$z/$x/$y.pbf, size: ${tileData.size} bytes"
                                )
                                call.respondBytes(
                                    tileData, contentType = ContentType.Application.ProtoBuf
                                )
                                return@get
                            }
                        }

                        Log.d(TAG, "Tile not found: /openmaptiles/$z/$x/$y.pbf")
                        // If we respond with NotFound here (as would make sense) it will cause maplibre to cache the
                        // fact that this tile doesn't exist in this source, which we don't want because it will never
                        // retry, nor will it overzoom the previous tiles.
                        call.respondBytes(
                            bytes = ByteArray(0),
                            contentType = ContentType.Application.ProtoBuf,
                            status = HttpStatusCode.BadGateway
                        )
                    }
                }
            }
        }
        server?.start(wait = false)

        port = availablePort

        Log.d(TAG, "Tile server started on port: $port")
    }

    fun stop() {
        server?.stop(1000, 5000)
        server = null
        port = -1

        // Close the databases.
        terrainDatabase?.close()
        terrainDatabase = null
        landcoverDatabase?.close()
        landcoverDatabase = null
        basemapDatabase?.close()
        basemapDatabase = null
        offlineAreasDatabase?.close()
        offlineAreasDatabase = null

        Log.d(TAG, "Tile server stopped")
    }

    fun getPort(): Int {
        return port
    }

    private fun readAssetFile(fileName: String): String {
        return context.assets.open(fileName).use { inputStream ->
            inputStream.bufferedReader().use { reader ->
                reader.readText()
            }
        }
    }

    private fun findAvailablePort(): Int {
        // Try to find an available port starting from 8000
        for (port in 8000..18000) {
            if (isPortAvailable(port)) {
                return port
            }
        }
        Log.w(TAG, "No available ports found.")
        return -1
    }

    private fun isPortAvailable(port: Int): Boolean {
        return try {
            val socket = java.net.ServerSocket(port)
            socket.close()
            true
        } catch (_: java.io.IOException) {
            false
        }
    }

    private fun copyDatabaseToStorage(name: String): File? {
        try {
            Log.d(TAG, "Copying $name to local storage")
            // Copy the mbtiles file from assets to internal storage
            val dbFile = File(context.filesDir, name)

            if (!dbFile.exists()) {
                context.assets.open(name).use { inputStream ->
                    FileOutputStream(dbFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
            return dbFile
        } catch (e: Exception) {
            Log.e(TAG, "Error copying MBTiles database to local storage", e)
        }
        return null
    }

    private fun initializeMbtilesDatabases() {
        try {
            Log.d(TAG, "Copying database files to local storage...")
            val terrainFile = copyDatabaseToStorage("terrain.mbtiles") ?: return
            val landcoverFile = copyDatabaseToStorage("landcover.mbtiles") ?: return
            val basemapFile = copyDatabaseToStorage("basemap.mbtiles") ?: return

            Log.d(TAG, "Opening databases.")
            terrainDatabase = SQLiteDatabase.openDatabase(
                terrainFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY
            )
            landcoverDatabase = SQLiteDatabase.openDatabase(
                landcoverFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY
            )
            basemapDatabase = SQLiteDatabase.openDatabase(
                basemapFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY
            )

            // Open or create the offline areas database
            val offlineAreasFile = File(context.filesDir, OFFLINE_DATABASE_NAME)
            offlineAreasDatabase = SQLiteDatabase.openOrCreateDatabase(
                offlineAreasFile.absolutePath, null
            )

            // Initialize the schema if needed
            initializeOfflineAreasSchema(offlineAreasDatabase!!)

            Log.d(TAG, "Offline areas database opened/created successfully")

            Log.d(TAG, "MBTiles database initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MBTiles database", e)
        }
    }

    private fun initializeOfflineAreasSchema(db: SQLiteDatabase) {
        try {
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

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS tile_index ON tiles (zoom_level, tile_column, tile_row, area_id)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS valhalla_tile_index ON valhalla_tiles (hierarchy_level, tile_index, area_id)")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing offline areas schema", e)
        }
    }

    private fun getTileData(
        database: SQLiteDatabase, zoomLevel: Int, tileColumn: Long, tileRow: Long
    ): ByteArray? {
        var cursor: android.database.Cursor? = null
        return try {
            val query =
                "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?"
            cursor = database.rawQuery(
                query, arrayOf(zoomLevel.toString(), tileColumn.toString(), tileRow.toString())
            )

            if (cursor.moveToFirst()) {
                val blobIndex = cursor.getColumnIndex("tile_data")
                if (blobIndex != -1) {
                    val blob = cursor.getBlob(blobIndex)
                    Log.v(
                        TAG, "Found tile $zoomLevel/$tileColumn/$tileRow, size: ${blob.size} bytes"
                    )
                    blob
                } else {
                    Log.w(TAG, "Blob index -1 for tile $zoomLevel/$tileColumn/$tileRow")
                    null
                }
            } else {
                Log.v(TAG, "Tile $zoomLevel/$tileColumn/$tileRow not found")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving tile data for $zoomLevel/$tileColumn/$tileRow", e)
            null
        } finally {
            cursor?.close()
        }
    }

    /**
     * Get tile data from offline area databases
     */
    private fun getTileDataFromOfflineDatabases(
        zoomLevel: Int, tileColumn: Long, tileRow: Long
    ): ByteArray? {
        // Try the offline areas database
        if (offlineAreasDatabase != null) {
            // Convert XYZ to TMS coordinate system for MBTiles
            val tmsTileRow = (2.0.pow(zoomLevel.toDouble()) - 1 - tileRow).toLong()

            var cursor: android.database.Cursor? = null
            return try {
                // Query the database for tiles from any area
                val query =
                    "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ? LIMIT 1"
                cursor = offlineAreasDatabase?.rawQuery(
                    query,
                    arrayOf(zoomLevel.toString(), tileColumn.toString(), tmsTileRow.toString())
                )

                if (cursor?.moveToFirst() == true) {
                    val blobIndex = cursor.getColumnIndex("tile_data")
                    if (blobIndex != -1) {
                        val blob = cursor.getBlob(blobIndex)
                        Log.d(
                            TAG,
                            "Tile found in offline database: $zoomLevel/$tileColumn/$tileRow (TMS: $tmsTileRow)"
                        )
                        blob
                    } else {
                        Log.w(TAG, "Blob index -1 in offline database")
                        null
                    }
                } else {
                    Log.d(
                        TAG,
                        "Tile not found in offline database: $zoomLevel/$tileColumn/$tileRow (TMS: $tmsTileRow)"
                    )
                    null
                }
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Error retrieving tile data from offline database for $zoomLevel/$tileColumn/$tileRow",
                    e
                )
                null
            } finally {
                cursor?.close()
            }
        }

        Log.d(TAG, "Tile not found in offline database")
        return null
    }

    /**
     * Check if the app is in offline mode
     */
    private fun isOfflineMode(): Boolean {
        return appPreferences.loadOfflineMode()
    }

    /**
     * Fetch tile data from the internet
     */
    private suspend fun fetchTileFromInternet(z: Int, x: Long, y: Long): ByteArray? {
        return try {
            val urlTemplate = context.getString(R.string.tile_url_template)
            val url = urlTemplate.replace("{z}", z.toString()).replace("{x}", x.toString())
                .replace("{y}", y.toString())
            Log.d(TAG, "Fetching tile from internet: $url")

            val response = httpClient.get(url)

            if (response.status == HttpStatusCode.OK) {
                val bytes = response.bodyAsBytes()
                Log.d(TAG, "Successfully fetched tile from internet, size: ${bytes.size} bytes")
                bytes
            } else {
                Log.w(TAG, "Failed to fetch tile from internet, status: ${response.status}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tile from internet", e)
            null
        }
    }

    companion object {
        private const val TAG = "LocalMapServer"
        private const val OFFLINE_DATABASE_NAME = "offline_areas.mbtiles"
    }
}
