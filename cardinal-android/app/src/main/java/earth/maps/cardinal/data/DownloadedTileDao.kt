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

package earth.maps.cardinal.data

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
