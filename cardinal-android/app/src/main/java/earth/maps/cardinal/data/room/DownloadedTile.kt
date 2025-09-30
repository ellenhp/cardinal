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
