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
