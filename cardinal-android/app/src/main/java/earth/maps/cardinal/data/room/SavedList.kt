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