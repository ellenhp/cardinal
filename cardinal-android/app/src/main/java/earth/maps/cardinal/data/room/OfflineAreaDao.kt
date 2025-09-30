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
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineAreaDao {
    @Query("SELECT * FROM offline_areas")
    fun getAllOfflineAreas(): Flow<List<OfflineArea>>

    @Query("SELECT * FROM offline_areas WHERE id = :id")
    suspend fun getOfflineAreaById(id: String): OfflineArea?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineArea(offlineArea: OfflineArea)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineAreas(offlineAreas: List<OfflineArea>)

    @Update
    suspend fun updateOfflineArea(offlineArea: OfflineArea)

    @Delete
    suspend fun deleteOfflineArea(offlineArea: OfflineArea)

    @Query("DELETE FROM offline_areas")
    suspend fun deleteAllOfflineAreas()
}
