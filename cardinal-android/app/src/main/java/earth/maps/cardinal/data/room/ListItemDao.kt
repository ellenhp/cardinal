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

    @Query("DELETE FROM list_items WHERE itemId = :itemId AND itemType = :itemType")
    suspend fun orphanItem(itemId: String, itemType: ItemType)
}
