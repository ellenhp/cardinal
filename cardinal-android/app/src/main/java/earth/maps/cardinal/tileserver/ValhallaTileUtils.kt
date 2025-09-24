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

import earth.maps.cardinal.data.BoundingBox

/**
 * Valhalla tile coordinate system utilities
 */
object ValhallaTileUtils {
    private val levelToSize = mapOf(
        0 to 4.0,
        1 to 1.0,
        2 to 0.25
    )

    /**
     * Get latitude and longitude of tile's bottom-left corner
     */
    fun getLatLon(graphId: Long): Pair<Double, Double> {
        val hierarchyLevel = getHierarchyLevel(graphId)
        val size = levelToSize[hierarchyLevel] ?: 1.0
        val totalColumns = (360 / size).toInt()
        val tileIndex = getTileIndex(graphId)
        val lat = (tileIndex / totalColumns) * size - 90
        val lon = (tileIndex % totalColumns) * size - 180

        return Pair(lat, lon)
    }

    /**
     * Get tile index from latitude and longitude
     */
    fun getTileIndexFromLatLon(
        hierarchyLevel: Int,
        lat: Double,
        lon: Double
    ): Int {
        require(lat in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(lon in -180.0..180.0) { "Longitude must be between -180 and 180" }

        val size = levelToSize[hierarchyLevel] ?: 1.0
        val totalColumns = (360 / size).toInt()
        val numColumns = ((lon + 180) / size).toInt()
        val numRows = ((lat + 90) / size).toInt()

        return numRows * totalColumns + numColumns
    }

    /**
     * Return a list of tiles that intersect the bounding box
     */
    fun tilesForBoundingBox(
        boundingBox: BoundingBox
    ): List<Pair<Int, Int>> {

        // Move these so we can compute percentages
        val adjustedLeft = boundingBox.west + 180
        val adjustedRight = boundingBox.east + 180
        val adjustedBottom = boundingBox.south + 90
        val adjustedTop = boundingBox.north + 90

        val tiles = mutableListOf<Pair<Int, Int>>()

        // For each size of tile
        for ((level, size) in levelToSize) {
            // For each column
            for (x in (adjustedLeft / size).toInt()..(adjustedRight / size).toInt()) {
                // For each row
                for (y in (adjustedBottom / size).toInt()..(adjustedTop / size).toInt()) {
                    // Give back the level and the tile index
                    val tileIndex = (y * (360.0 / size) + x).toInt()
                    tiles.add(Pair(level, tileIndex))
                }
            }
        }

        return tiles
    }

    /**
     * Get hierarchy level from graph ID
     */
    private fun getHierarchyLevel(graphId: Long): Int {
        return (graphId and 0x3).toInt() // Extract the last 2 bits as level (0-3)
    }

    /**
     * Get tile index from graph ID
     */
    private fun getTileIndex(graphId: Long): Int {
        return (graphId shr 2).toInt() // Shift right by 2 bits to get the tile index
    }

    /**
     * Generate the directory path for a Valhalla tile
     * Format: hierarchy/{group}/{id}.gph for levels 0-1
     * Format: hierarchy/{group1}/{group2}/{id}.gph for level 2
     * where group = tile_index / 1000, id = tile_index % 1000
     * and group1 = tile_index / 1000000, group2 = (tile_index / 1000) % 1000 for level 2
     */
    fun getTileDirectoryPath(hierarchyLevel: Int, tileIndex: Int): String {
        return when (hierarchyLevel) {
            2 -> {
                val group1 = tileIndex / 1000000
                val group2 = (tileIndex / 1000) % 1000
                val id = tileIndex % 1000
                "$hierarchyLevel/${group1.toString().padStart(3, '0')}/${
                    group2.toString().padStart(3, '0')
                }/${id.toString().padStart(3, '0')}.gph"
            }

            else -> {
                val group = tileIndex / 1000
                val id = tileIndex % 1000
                "$hierarchyLevel/${group.toString().padStart(3, '0')}/${
                    id.toString().padStart(3, '0')
                }.gph"
            }
        }
    }

    /**
     * Generate the URL for a Valhalla tile
     */
    fun getTileUrl(baseUrl: String, hierarchyLevel: Int, tileIndex: Int): String {
        val path = getTileDirectoryPath(hierarchyLevel, tileIndex)
        return "$baseUrl/$path"
    }

    /**
     * Generate the local file path for a Valhalla tile
     */
    fun getLocalTileFilePath(
        valhallaTilesDir: java.io.File,
        hierarchyLevel: Int,
        tileIndex: Int
    ): java.io.File {
        return when (hierarchyLevel) {
            2 -> {
                val group1 = tileIndex / 1000000
                val group2 = (tileIndex / 1000) % 1000
                val id = tileIndex % 1000

                // Create directory structure: valhalla_tiles/{hierarchy}/{group1}/{group2}/
                val hierarchyDir = java.io.File(valhallaTilesDir, hierarchyLevel.toString())
                val group1Dir = java.io.File(hierarchyDir, group1.toString().padStart(3, '0'))
                val group2Dir = java.io.File(group1Dir, group2.toString().padStart(3, '0'))

                // Ensure directories exist
                group2Dir.mkdirs()

                // Return file path: {group2_dir}/{id}.gph
                java.io.File(group2Dir, "${id.toString().padStart(3, '0')}.gph")
            }

            else -> {
                val group = tileIndex / 1000
                val id = tileIndex % 1000

                // Create directory structure: valhalla_tiles/{hierarchy}/{group}/
                val hierarchyDir = java.io.File(valhallaTilesDir, hierarchyLevel.toString())
                val groupDir = java.io.File(hierarchyDir, group.toString().padStart(3, '0'))

                // Ensure directories exist
                groupDir.mkdirs()

                // Return file path: {group_dir}/{id}.gph
                java.io.File(groupDir, "${id.toString().padStart(3, '0')}.gph")
            }
        }
    }
}
