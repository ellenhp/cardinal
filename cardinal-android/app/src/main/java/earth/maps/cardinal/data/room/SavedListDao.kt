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
interface SavedListDao {
    @Query("SELECT * FROM saved_lists WHERE isRoot = 1 LIMIT 1")
    suspend fun getRootList(): SavedList?

    @Query("SELECT * FROM saved_lists WHERE isRoot = 1 LIMIT 1")
    fun getRootListAsFlow(): Flow<SavedList?>

    @Query("SELECT * FROM saved_lists WHERE id = :listId")
    suspend fun getList(listId: String): SavedList?

    @Query("SELECT * FROM saved_lists WHERE id = :listId")
    fun getListAsFlow(listId: String): Flow<SavedList?>

    @Query("SELECT * FROM saved_lists")
    suspend fun getAllLists(): List<SavedList>

    @Query("UPDATE saved_lists SET isCollapsed = CASE WHEN isCollapsed = 0 THEN 1 ELSE 0 END WHERE id = :listId")
    suspend fun toggleExpanded(listId: String): Unit

    @Query(
        """
        SELECT sl.* FROM saved_lists sl
        INNER JOIN list_items li ON sl.id = li.itemId
        WHERE li.listId = :parentListId AND li.itemType = 'LIST'
        ORDER BY li.position
    """
    )
    fun getChildLists(parentListId: String): Flow<List<SavedList>>

    @Insert
    suspend fun insertList(list: SavedList)

    @Update
    suspend fun updateList(list: SavedList)

    @Delete
    suspend fun deleteList(list: SavedList)
}