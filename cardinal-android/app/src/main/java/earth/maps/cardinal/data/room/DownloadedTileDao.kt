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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedTileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedTile(tile: DownloadedTile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTile(tile: DownloadedTile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedTiles(tiles: List<DownloadedTile>)

    @Query("SELECT * FROM downloaded_tiles WHERE id = :id")
    suspend fun getTileById(id: String): DownloadedTile?

    @Query("SELECT * FROM downloaded_tiles WHERE areaId = :areaId")
    suspend fun getDownloadedTilesForArea(areaId: String): List<DownloadedTile>

    @Query("SELECT * FROM downloaded_tiles WHERE areaId = :areaId AND tileType = :tileType")
    suspend fun getDownloadedTilesForAreaAndType(
        areaId: String,
        tileType: TileType
    ): List<DownloadedTile>

    @Query("SELECT COUNT(*) FROM downloaded_tiles WHERE areaId = :areaId AND tileType = :tileType")
    suspend fun getDownloadedTileCountForAreaAndType(areaId: String, tileType: TileType): Int

    @Query("SELECT COUNT(*) FROM downloaded_tiles WHERE areaId = :areaId")
    suspend fun getDownloadedTileCountForArea(areaId: String): Int

    @Query("SELECT * FROM downloaded_tiles WHERE areaId = :areaId AND tileType = :tileType")
    fun getDownloadedTilesForAreaAndTypeFlow(
        areaId: String,
        tileType: TileType
    ): Flow<List<DownloadedTile>>

    @Query("DELETE FROM downloaded_tiles WHERE areaId = :areaId")
    suspend fun deleteDownloadedTilesForArea(areaId: String)

    @Query("DELETE FROM downloaded_tiles WHERE areaId = :areaId AND tileType = :tileType")
    suspend fun deleteDownloadedTilesForAreaAndType(areaId: String, tileType: TileType)

    @Query("DELETE FROM downloaded_tiles")
    suspend fun deleteAllDownloadedTiles()

    // Check if a specific basemap tile exists
    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_tiles WHERE areaId = :areaId AND tileType = 'BASEMAP' AND zoom = :zoom AND tileX = :tileX AND tileY = :tileY)")
    suspend fun isBasemapTileDownloaded(areaId: String, zoom: Int, tileX: Int, tileY: Int): Boolean

    // Check if a specific Valhalla tile exists
    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_tiles WHERE areaId = :areaId AND tileType = 'VALHALLA' AND hierarchyLevel = :hierarchyLevel AND tileIndex = :tileIndex)")
    suspend fun isValhallaTileDownloaded(
        areaId: String,
        hierarchyLevel: Int,
        tileIndex: Int
    ): Boolean

    // Get all areas that have downloaded tiles (for recovery purposes)
    @Query("SELECT DISTINCT areaId FROM downloaded_tiles")
    suspend fun getAreasWithDownloadedTiles(): List<String>
}
