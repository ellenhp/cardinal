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
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPlaceDao {
    @Query("SELECT * FROM saved_places WHERE id = :placeId")
    fun getPlaceAsFlow(placeId: String): Flow<SavedPlace?>

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