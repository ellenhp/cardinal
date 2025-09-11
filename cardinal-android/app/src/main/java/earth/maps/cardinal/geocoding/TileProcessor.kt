package earth.maps.cardinal.geocoding

/**
 * Interface for processing map tiles for geocoding purposes.
 */
interface TileProcessor {
    /**
     * Indicates the beginning of a tile processing transaction.
     */
    suspend fun beginTileProcessing()

    /**
     * Indicates the end of a tile processing transaction.
     */
    suspend fun endTileProcessing()

    /**
     * Process a map tile for geocoding. This function must be thread-safe.
     * @param tileData The raw tile data (MVT format)
     * @param zoom The zoom level of the tile
     * @param x The x coordinate of the tile
     * @param y The y coordinate of the tile
     */
    suspend fun processTile(tileData: ByteArray, zoom: Int, x: Int, y: Int)
}
