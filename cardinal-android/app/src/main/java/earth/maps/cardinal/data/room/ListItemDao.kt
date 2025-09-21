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
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ListItemDao {
    @Insert
    suspend fun insertItem(item: ListItem)

    @Delete
    suspend fun deleteItem(item: ListItem)

    @Query("DELETE FROM list_items WHERE listId = :listId")
    suspend fun clearList(listId: String)

    @Query(
        """
        UPDATE list_items 
        SET position = :newPosition 
        WHERE listId = :listId AND itemId = :itemId AND itemType = :itemType
    """
    )
    suspend fun updateItemPosition(
        listId: String,
        itemId: String,
        itemType: ItemType,
        newPosition: Int
    )

    @Query(
        """
        SELECT * FROM list_items 
        WHERE listId = :listId 
        ORDER BY position
    """
    )
    fun getItemsInListAsFlow(listId: String): Flow<List<ListItem>>

    @Query(
        """
        SELECT * FROM list_items 
        WHERE listId = :listId 
        ORDER BY position
    """
    )
    suspend fun getItemsInList(listId: String): List<ListItem>

    // For moving items between lists
    @Query(
        """
        UPDATE list_items 
        SET listId = :newListId, position = :newPosition
        WHERE itemId = :itemId
    """
    )
    suspend fun moveItem(
        itemId: String,
        newListId: String,
        newPosition: Int
    )

    // Reorder all items when positions change
    @Transaction
    suspend fun reorderItems(listId: String, items: List<ListItem>) {
        clearList(listId)
        items.forEachIndexed { index, item ->
            insertItem(item.copy(position = index))
        }
    }
}
