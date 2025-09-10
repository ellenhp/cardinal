package earth.maps.cardinal.tileserver

import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream

/**
 * PMTiles reader for Android implementation
 * Based on the PMTiles v3 specification
 */
class PMTilesReader {
    private val pmtilesFile: File?
    private val pmtilesUrl: String?
    
    private val TAG = "PMTilesReader"
    
    // Header fields
    private var header: Header? = null
    
    // Cache for directories
    private val directoryCache = mutableMapOf<String, List<DirectoryEntry>>()
    
    // PMTiles v3 constants
    private companion object {
        const val HEADER_SIZE = 127
        const val MAX_DIRECTORY_CACHE_ENTRIES = 100
        const val COMPRESSION_UNKNOWN = 0
        const val COMPRESSION_NONE = 1
        const val COMPRESSION_GZIP = 2
        const val COMPRESSION_BROTLI = 3
        const val COMPRESSION_ZSTD = 4
    }
    
    /**
     * PMTiles header structure
     */
    private data class Header(
        val rootDirectoryOffset: Long,
        val rootDirectoryLength: Long,
        val metadataOffset: Long,
        val metadataLength: Long,
        val leafDirectoriesOffset: Long,
        val leafDirectoriesLength: Long,
        val tileDataOffset: Long,
        val tileDataLength: Long,
        val numAddressedTiles: Long,
        val numTileEntries: Long,
        val numTileContents: Long,
        val clustered: Boolean,
        val internalCompression: Int,
        val tileCompression: Int,
        val tileType: Int,
        val minZoom: Int,
        val maxZoom: Int,
        val minLon: Double,
        val minLat: Double,
        val maxLon: Double,
        val maxLat: Double,
        val centerZoom: Int,
        val centerLon: Double,
        val centerLat: Double
    )
    
    /**
     * Directory entry structure
     */
    private data class DirectoryEntry(
        val tileId: Long,
        val offset: Long,
        val length: Long,
        val runLength: Long
    )
    
    constructor(pmtilesFile: File) {
        this.pmtilesFile = pmtilesFile
        this.pmtilesUrl = null
        // Defer header reading until needed to avoid blocking the main thread
    }
    
    constructor(pmtilesUrl: String) {
        this.pmtilesFile = null
        this.pmtilesUrl = pmtilesUrl
        // Defer header reading until needed to avoid blocking the main thread
    }
    
    /**
     * Read the PMTiles header
     */
    private fun readHeader() {
        try {
            val headerData = if (pmtilesFile != null) {
                if (!pmtilesFile.exists()) {
                    throw IllegalArgumentException("PMTiles file does not exist: ${pmtilesFile.absolutePath}")
                }
                RandomAccessFile(pmtilesFile, "r").use { file ->
                    val headerBytes = ByteArray(HEADER_SIZE)
                    file.readFully(headerBytes)
                    headerBytes
                }
            } else if (pmtilesUrl != null) {
                fetchRange(pmtilesUrl, 0, (HEADER_SIZE - 1).toLong())
            } else {
                throw IllegalStateException("No file or URL provided")
            }
            
            val buffer = ByteBuffer.wrap(headerData)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            
            // Check magic number
            val magic = ByteArray(7)
            buffer.get(magic)
            val magicString = String(magic)
            if (magicString != "PMTiles") {
                throw IllegalArgumentException("Invalid PMTiles file: wrong magic number")
            }
            
            // Check version
            val version = buffer.get().toInt()
            if (version != 3) {
                throw IllegalArgumentException("Unsupported PMTiles version: $version")
            }
            
            // Read header fields
            val rootDirectoryOffset = buffer.long
            val rootDirectoryLength = buffer.long
            val metadataOffset = buffer.long
            val metadataLength = buffer.long
            val leafDirectoriesOffset = buffer.long
            val leafDirectoriesLength = buffer.long
            val tileDataOffset = buffer.long
            val tileDataLength = buffer.long
            val numAddressedTiles = buffer.long
            val numTileEntries = buffer.long
            val numTileContents = buffer.long
            val clustered = buffer.get().toInt() == 1
            val internalCompression = buffer.get().toInt()
            val tileCompression = buffer.get().toInt()
            val tileType = buffer.get().toInt()
            val minZoom = buffer.get().toInt()
            val maxZoom = buffer.get().toInt()
            
            // Read min position (longitude, latitude)
            val minLon = buffer.int.toDouble() / 10000000.0
            val minLat = buffer.int.toDouble() / 10000000.0
            
            // Read max position (longitude, latitude)
            val maxLon = buffer.int.toDouble() / 10000000.0
            val maxLat = buffer.int.toDouble() / 10000000.0
            
            // Read center zoom
            val centerZoom = buffer.get().toInt()
            
            // Read center position (longitude, latitude)
            val centerLon = buffer.int.toDouble() / 10000000.0
            val centerLat = buffer.int.toDouble() / 10000000.0
            
            header = Header(
                rootDirectoryOffset,
                rootDirectoryLength,
                metadataOffset,
                metadataLength,
                leafDirectoriesOffset,
                leafDirectoriesLength,
                tileDataOffset,
                tileDataLength,
                numAddressedTiles,
                numTileEntries,
                numTileContents,
                clustered,
                internalCompression,
                tileCompression,
                tileType,
                minZoom,
                maxZoom,
                minLon,
                minLat,
                maxLon,
                maxLat,
                centerZoom,
                centerLon,
                centerLat
            )
            
            Log.d(TAG, "PMTiles header read successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error reading PMTiles header", e)
            throw e
        }
    }
    
    /**
     * Fetch a range of bytes from a URL
     */
    private fun fetchRange(url: String, start: Long, end: Long): ByteArray {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.setRequestProperty("Range", "bytes=$start-$end")
            connection.connect()
            
            if (connection.responseCode !in 200..299) {
                throw RuntimeException("HTTP error ${connection.responseCode}: ${connection.responseMessage}")
            }
            
            return connection.inputStream.use { it.readBytes() }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching range $start-$end from $url", e)
            throw e
        }
    }
    
    /**
     * Get tile data for a specific tile
     */
    fun getTile(z: Int, x: Int, y: Int): ByteArray? {
        // Load header if not already loaded
        if (header == null) {
            try {
                readHeader()
            } catch (e: Exception) {
                Log.e(TAG, "Error reading header", e)
                return null
            }
        }
        
        val header = this.header
        if (header == null) {
            Log.e(TAG, "Header is null")
            return null
        }
        
        // Check if zoom is within bounds
        if (z < header.minZoom || z > header.maxZoom) {
            Log.e(TAG, "Tile out of source's zoom range")
            return null
        }
        
        try {
            // Convert XYZ to tile ID
            val tileId = zxyToTileId(z, x, y)
            
            // Find tile address
            val tileAddress = findTileAddress(tileId, header.rootDirectoryOffset, header.rootDirectoryLength, 0)
            
            if (tileAddress == null || tileAddress.first == 0L && tileAddress.second == 0L) {
                Log.e(TAG, "Invalid tile address: $tileAddress")
                return null
            }
            
            // Read tile data
            return readTileData(tileAddress.first, tileAddress.second, header.tileCompression)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting tile data for $z/$x/$y", e)
            return null
        }
    }
    
    /**
     * Rotate function for Hilbert curve calculation
     */
    private fun rotate(n: Int, x: Int, y: Int, rx: Int, ry: Int): Pair<Int, Int> {
        if (ry == 0) {
            if (rx != 0) {
                return Pair(n - 1 - y, n - 1 - x)
            }
            return Pair(y, x)
        }
        return Pair(x, y)
    }
    
    /**
     * Convert ZXY coordinates to tile ID using Hilbert curve
     */
    private fun zxyToTileId(z: Int, x: Int, y: Int): Long {
        if (z > 26) {
            throw IllegalArgumentException("Tile zoom level exceeds max safe number limit (26)")
        }
        if (x >= (1 shl z) || y >= (1 shl z)) {
            throw IllegalArgumentException("tile x/y outside zoom level bounds")
        }
        
        var acc = ((1L shl z) * (1L shl z) - 1) / 3
        var a = z - 1
        var tx = x
        var ty = y
        
        var s = 1 shl a
        while (s > 0) {
            val rx = tx and s
            val ry = ty and s
            acc += (((3 * rx) xor ry).toLong() * s)
            
            // Rotate
            val rotated = rotate(s, tx, ty, rx, ry)
            tx = rotated.first
            ty = rotated.second
            
            s = s shr 1
        }
        
        return acc
    }
    
    /**
     * Find tile address (offset, length) for a tile ID using binary search
     */
    private fun findTileAddress(tileId: Long, directoryOffset: Long, directoryLength: Long, depth: Int): Pair<Long, Long>? {
        // Limit directory depth to prevent infinite recursion
        if (depth > 4) {
            Log.w(TAG, "Maximum directory depth exceeded")
            return null
        }
        
        // Load header if not already loaded
        if (header == null) {
            readHeader()
        }
        val header = this.header ?: return null

        // Get directory entries
        val directoryKey = "$directoryOffset|$directoryLength"
        val directory = directoryCache[directoryKey] ?: run {
            val entries = readDirectory(directoryOffset, directoryLength)
            // Cache the directory
            if (directoryCache.size >= MAX_DIRECTORY_CACHE_ENTRIES) {
                // Remove first entry (simple FIFO cache)
                val firstKey = directoryCache.keys.firstOrNull()
                if (firstKey != null) {
                    directoryCache.remove(firstKey)
                }
            }
            directoryCache[directoryKey] = entries
            entries
        }

        // Binary search for the tile ID
        var m = 0
        var n = directory.size - 1
        while (m <= n) {
            val k = (n + m) ushr 1
            val entry = directory[k]
            val cmp = tileId - entry.tileId
            when {
                cmp > 0 -> m = k + 1
                cmp < 0 -> n = k - 1
                else -> {
                    // Found exact match
                    if (entry.runLength > 0) {
                        // This is a tile entry
                        val offset = header.tileDataOffset + entry.offset
                        return Pair(offset, entry.length)
                    } else {
                        // This is a leaf directory entry, recurse
                        val leafOffset = header.leafDirectoriesOffset + entry.offset
                        return findTileAddress(tileId, leafOffset, entry.length, depth + 1)
                    }
                }
            }
        }

        // At this point, m > n
        if (n >= 0) {
            val entry = directory[n]
            if (entry.runLength == 0L) {
                // This is a leaf directory entry, recurse
                val leafOffset = header.leafDirectoriesOffset + entry.offset
                return findTileAddress(tileId, leafOffset, entry.length, depth + 1)
            }
            if (tileId - entry.tileId < entry.runLength) {
                // This is a tile entry within the run length
                val offset = header.tileDataOffset + entry.offset
                return Pair(offset, entry.length)
            }
        }
        
        // Tile not found
        return null
    }
    
    /**
     * Read a directory from the file or URL
     */
    private fun readDirectory(directoryOffset: Long, directoryLength: Long): List<DirectoryEntry> {
        // Load header if not already loaded
        if (header == null) {
            readHeader()
        }
        
        val header = this.header ?: throw IllegalStateException("Header not initialized")
        
        val compressedData = if (pmtilesFile != null) {
            RandomAccessFile(pmtilesFile, "r").use { file ->
                file.seek(directoryOffset)
                val data = ByteArray(directoryLength.toInt())
                file.readFully(data)
                data
            }
        } else if (pmtilesUrl != null) {
            fetchRange(pmtilesUrl, directoryOffset, directoryOffset + directoryLength - 1)
        } else {
            throw IllegalStateException("No file or URL provided")
        }
        
        // Decompress if needed
        val data = when (header.internalCompression) {
            COMPRESSION_GZIP -> decompressGzip(compressedData)
            COMPRESSION_NONE -> compressedData
            else -> {
                Log.w(TAG, "Unsupported internal compression: ${header.internalCompression}")
                compressedData
            }
        }
        
        return decodeDirectory(data)
    }
    
    /**
     * Decode a directory from its binary representation
     */
    private fun decodeDirectory(data: ByteArray): List<DirectoryEntry> {
        val buffer = ByteBuffer.wrap(data)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        
        // Read number of entries
        val numEntries = readVarInt(buffer).toInt()
        
        val entries = mutableListOf<DirectoryEntry>()
        
        // Read tile IDs (delta encoded)
        val tileIds = LongArray(numEntries)
        var lastId = 0L
        for (i in 0 until numEntries) {
            val delta = readVarInt(buffer)
            lastId += delta
            tileIds[i] = lastId
        }
        
        // Read run lengths
        val runLengths = LongArray(numEntries)
        for (i in 0 until numEntries) {
            runLengths[i] = readVarInt(buffer)
        }
        
        // Read lengths
        val lengths = LongArray(numEntries)
        for (i in 0 until numEntries) {
            lengths[i] = readVarInt(buffer)
        }
        
        // Read offsets
        val offsets = LongArray(numEntries)
        var nextByte = 0L
        for (i in 0 until numEntries) {
            val value = readVarInt(buffer)
            if (value == 0L && i > 0) {
                // Offset is contiguous with previous entry
                offsets[i] = nextByte
            } else {
                offsets[i] = value - 1
            }
            nextByte = offsets[i] + lengths[i]
        }
        
        // Create entries
        for (i in 0 until numEntries) {
            entries.add(DirectoryEntry(tileIds[i], offsets[i], lengths[i], runLengths[i]))
        }
        
        return entries
    }
    
    /**
     * Read a variable-length integer from the buffer
     */
    private fun readVarInt(buffer: ByteBuffer): Long {
        var value = 0L
        var shift = 0
        
        while (buffer.hasRemaining()) {
            val b = buffer.get().toInt() and 0xFF
            value = value or ((b and 0x7F).toLong() shl shift)
            if ((b and 0x80) == 0) {
                break
            }
            shift += 7
        }
        
        return value
    }
    
    /**
     * Read tile data from the file or URL
     */
    private fun readTileData(offset: Long, length: Long, compression: Int): ByteArray {
        val data = if (pmtilesFile != null) {
            RandomAccessFile(pmtilesFile, "r").use { file ->
                file.seek(offset)
                val tileData = ByteArray(length.toInt())
                file.readFully(tileData)
                tileData
            }
        } else if (pmtilesUrl != null) {
            fetchRange(pmtilesUrl, offset, offset + length - 1)
        } else {
            throw IllegalStateException("No file or URL provided")
        }
        
        // Decompress if needed
        return when (compression) {
            COMPRESSION_GZIP -> decompressGzip(data)
            COMPRESSION_NONE -> data
            else -> {
                Log.w(TAG, "Unsupported tile compression: $compression")
                data
            }
        }
    }
    
    /**
     * Decompress GZIP-compressed data
     */
    private fun decompressGzip(compressedData: ByteArray): ByteArray {
        return try {
            GZIPInputStream(compressedData.inputStream()).use { gzip ->
                gzip.readBytes()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decompressing GZIP data", e)
            compressedData
        }
    }
    
    /**
     * Get the minimum zoom level
     */
    fun getMinZoom(): Int {
        // Load header if not already loaded
        if (header == null) {
            try {
                readHeader()
            } catch (e: Exception) {
                Log.e(TAG, "Error reading header", e)
                return 0
            }
        }
        return header?.minZoom ?: 0
    }
    
    /**
     * Get the maximum zoom level
     */
    fun getMaxZoom(): Int {
        // Load header if not already loaded
        if (header == null) {
            try {
                readHeader()
            } catch (e: Exception) {
                Log.e(TAG, "Error reading header", e)
                return 0
            }
        }
        return header?.maxZoom ?: 0
    }
    
    /**
     * Get the bounding box
     */
    fun getBounds(): DoubleArray? {
        // Load header if not already loaded
        if (header == null) {
            try {
                readHeader()
            } catch (e: Exception) {
                Log.e(TAG, "Error reading header", e)
                return null
            }
        }
        val header = this.header ?: return null
        return doubleArrayOf(header.minLon, header.minLat, header.maxLon, header.maxLat)
    }
}
