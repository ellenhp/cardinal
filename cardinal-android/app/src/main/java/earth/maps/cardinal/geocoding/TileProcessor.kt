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
