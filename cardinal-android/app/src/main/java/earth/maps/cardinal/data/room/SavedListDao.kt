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
