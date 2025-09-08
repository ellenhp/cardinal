package earth.maps.cardinal.tileserver

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.io.File
import java.io.FileOutputStream
import kotlin.math.pow


class Tileserver(private val context: Context) {
    private val TAG = "Tileserver"
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? =
        null
    private var port: Int = -1
    private var terrainDatabase: SQLiteDatabase? = null
    private var landcoverDatabase: SQLiteDatabase? = null
    private var basemapDatabase: SQLiteDatabase? = null

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

                get("/style.json") {
                    try {
                        val styleJson = readAssetFile("style.json")
                        val modifiedStyleJson = styleJson.replace("{port}", port.toString())
                        call.respondText(
                            modifiedStyleJson,
                            contentType = ContentType.Application.Json
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading style.json", e)
                        call.respondText(
                            "Error reading style.json",
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
                            "Invalid tile coordinates",
                            status = HttpStatusCode.BadRequest
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
                            "Invalid tile coordinates",
                            status = HttpStatusCode.BadRequest
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
                    val basemapDatabase = basemapDatabase ?: return@get

                    val z = call.parameters["z"]?.toIntOrNull()
                    val x = call.parameters["x"]?.toLongOrNull()
                    val y = call.parameters["y"]?.toLongOrNull()

                    if (z == null || x == null || y == null) {
                        call.respondText(
                            "Invalid tile coordinates",
                            status = HttpStatusCode.BadRequest
                        )
                        return@get
                    }

                    // MBTiles uses TMS coordinate system, but most map libraries use XYZ
                    // Convert Y coordinate from XYZ to TMS
                    val tmsY = (2.0.pow(z.toDouble()) - 1 - y).toLong()

                    val tileData = getTileData(basemapDatabase, z, x, tmsY)
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
            // Open the databases
            terrainDatabase =
                SQLiteDatabase.openDatabase(
                    terrainFile.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )
            landcoverDatabase =
                SQLiteDatabase.openDatabase(
                    landcoverFile.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )
            basemapDatabase =
                SQLiteDatabase.openDatabase(
                    basemapFile.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )
            Log.d(TAG, "MBTiles database initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MBTiles database", e)
        }
    }

    private fun getTileData(
        database: SQLiteDatabase,
        zoomLevel: Int,
        tileColumn: Long,
        tileRow: Long
    ): ByteArray? {
        var cursor: android.database.Cursor? = null
        return try {
            val query =
                "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?"
            cursor = database.rawQuery(
                query,
                arrayOf(zoomLevel.toString(), tileColumn.toString(), tileRow.toString())
            )

            if (cursor.moveToFirst()) {
                val blobIndex = cursor.getColumnIndex("tile_data")
                if (blobIndex != -1) {
                    val blob = cursor.getBlob(blobIndex)
                    Log.d(TAG, "Tile size: ${blob.size}")
                    blob
                } else {
                    Log.w(TAG, "Blob index -1")
                    null
                }
            } else {
                Log.d(TAG, "Tile $zoomLevel/$tileColumn/$tileRow not found")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving tile data for $zoomLevel/$tileColumn/$tileRow", e)
            null
        } finally {
            cursor?.close()
        }
    }
}
