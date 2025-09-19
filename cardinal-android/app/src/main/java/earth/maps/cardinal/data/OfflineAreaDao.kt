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
