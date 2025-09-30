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

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "list_items",
    primaryKeys = ["listId", "itemId", "itemType"],
    foreignKeys = [
        ForeignKey(
            entity = SavedList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["listId", "position"]),
        Index(value = ["itemId", "itemType"])
    ]
)
data class ListItem(
    val listId: String,  // The list containing this item
    val itemId: String,  // ID of the place or nested list
    val itemType: ItemType,  // Enum: PLACE or LIST
    val position: Int,  // For ordering items within a list
    val addedAt: Long
)
