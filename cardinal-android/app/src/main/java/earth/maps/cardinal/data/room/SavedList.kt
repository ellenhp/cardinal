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
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "saved_lists")
data class SavedList(
    @PrimaryKey val id: String,  // Using UUID strings for better uniqueness
    val name: String,
    val description: String? = null,
    val isRoot: Boolean = false,  // Flag for the root "Saved Places" list
    val isCollapsed: Boolean = false,  // UI state for collapsible lists
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun createList(
            name: String,
            description: String? = null,
            isCollapsed: Boolean = false,
            isRoot: Boolean = false
        ): SavedList {
            val id = UUID.randomUUID().toString()
            val currentTime = System.currentTimeMillis()
            return SavedList(
                id = id,
                name = name,
                description = description,
                isRoot = isRoot,
                isCollapsed = isCollapsed,
                createdAt = currentTime,
                updatedAt = currentTime
            )
        }
    }
}
