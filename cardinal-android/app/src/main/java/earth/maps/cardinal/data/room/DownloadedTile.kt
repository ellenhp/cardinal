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

package earth.maps.cardinal.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/**
 * Entity representing a successfully downloaded tile.
 * Used to track download progress and enable recovery of interrupted downloads.
 */
@Entity(tableName = "downloaded_tiles")
data class DownloadedTile(
    @PrimaryKey val id: String, // composite ID based on tile type
    val areaId: String,
    val tileType: TileType,
    val downloadTimestamp: Long,
    val retryCount: Int = 0,

    // For basemap tiles (x/y/zoom system)
    val zoom: Int? = null,
    val tileX: Int? = null,
    val tileY: Int? = null,

    // For Valhalla tiles (hierarchyLevel/tileIndex system)
    val hierarchyLevel: Int? = null,
    val tileIndex: Int? = null
) {
    companion object {
        /**
         * Create a DownloadedTile for a basemap tile
         */
        fun forBasemapTile(
            areaId: String,
            zoom: Int,
            tileX: Int,
            tileY: Int,
            retryCount: Int = 0
        ): DownloadedTile {
            return DownloadedTile(
                id = "${areaId}_basemap_${zoom}_${tileX}_${tileY}",
                areaId = areaId,
                tileType = TileType.BASEMAP,
                downloadTimestamp = System.currentTimeMillis(),
                retryCount = retryCount,
                zoom = zoom,
                tileX = tileX,
                tileY = tileY
            )
        }

        /**
         * Create a DownloadedTile for a Valhalla tile
         */
        fun forValhallaTile(
            areaId: String,
            hierarchyLevel: Int,
            tileIndex: Int,
            retryCount: Int = 0
        ): DownloadedTile {
            return DownloadedTile(
                id = "${areaId}_valhalla_${hierarchyLevel}_${tileIndex}",
                areaId = areaId,
                tileType = TileType.VALHALLA,
                downloadTimestamp = System.currentTimeMillis(),
                retryCount = retryCount,
                hierarchyLevel = hierarchyLevel,
                tileIndex = tileIndex
            )
        }
    }
}

/**
 * Type of tile being downloaded
 */
enum class TileType {
    BASEMAP,
    VALHALLA
}

/**
 * TypeConverter for TileType enum to work with Room database
 */
class TileTypeConverter {
    @TypeConverter
    fun fromTileType(tileType: TileType): String {
        return tileType.name
    }

    @TypeConverter
    fun toTileType(tileType: String): TileType {
        return TileType.valueOf(tileType)
    }
}
