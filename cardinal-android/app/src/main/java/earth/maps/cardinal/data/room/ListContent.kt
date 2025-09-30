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

/**
 * A sealed class representing the content of a list.
 * This is used for UI representation where a list can contain either places or other lists.
 */
sealed class ListContent {
    abstract val id: String
    abstract val name: String
    abstract val position: Int
}

/**
 * Represents a place in a list.
 */
data class PlaceContent(
    override val id: String,
    override val name: String,
    val type: String,
    val icon: String,
    val customName: String? = null,
    val customDescription: String? = null,
    val isPinned: Boolean,
    override val position: Int,
) : ListContent()

/**
 * Represents a list in a list (nested list).
 */
data class ListContentItem(
    override val id: String,
    override val name: String,
    val description: String? = null,
    val isCollapsed: Boolean = false,
    override val position: Int,
) : ListContent()
