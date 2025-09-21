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

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPlaceDao {
    @Query("SELECT * FROM saved_places WHERE id = :placeId")
    fun getPlaceAsFlow(placeId: String): Flow<SavedPlace>

    @Query("SELECT * FROM saved_places WHERE id = :placeId")
    suspend fun getPlace(placeId: String): SavedPlace?

    @Query(
        """
        SELECT sp.* FROM saved_places sp
        INNER JOIN list_items li ON sp.id = li.itemId
        WHERE li.listId = :listId AND li.itemType = 'PLACE'
        ORDER BY li.position
    """
    )
    fun getPlacesInList(listId: String): Flow<List<SavedPlace>>

    @Query("SELECT * FROM saved_places")
    fun getAllPlacesAsFlow(): Flow<List<SavedPlace>>

    @Query("SELECT * FROM saved_places")
    suspend fun getAllPlaces(): List<SavedPlace>

    @Insert
    suspend fun insertPlace(place: SavedPlace)

    @Update
    suspend fun updatePlace(place: SavedPlace)

    @Delete
    suspend fun deletePlace(place: SavedPlace)
}
