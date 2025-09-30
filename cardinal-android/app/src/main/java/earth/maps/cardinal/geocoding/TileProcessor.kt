/*
 *     Cardinal Maps
 *     Copyright (C) 2025 Cardinal Maps Authors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
