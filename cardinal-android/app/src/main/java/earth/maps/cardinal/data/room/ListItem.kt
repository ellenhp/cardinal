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
