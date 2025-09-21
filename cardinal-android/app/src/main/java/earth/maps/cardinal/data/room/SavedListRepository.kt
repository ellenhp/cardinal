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

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedListRepository @Inject constructor(
    @param:ApplicationContext private val context: Context, database: AppDatabase
) {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val listDao = database.savedListDao()
    private val listItemDao = database.listItemDao()
    private val placeDao = database.savedPlaceDao()

    init {
        coroutineScope.launch {
            cleanupUnparentedElements()
        }
    }

    /**
     * Creates a new list.
     */
    suspend fun createList(
        name: String,
        description: String? = null,
        isRoot: Boolean = false,
        isCollapsed: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val id = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()
            val list = SavedList(
                id = id,
                name = name,
                description = description,
                isRoot = isRoot,
                isCollapsed = isCollapsed,
                createdAt = timestamp,
                updatedAt = timestamp
            )

            listDao.insertList(list)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates an existing list.
     */
    suspend fun updateList(
        listId: String,
        name: String? = null,
        description: String? = null,
        isCollapsed: Boolean? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existingList = listDao.getList(listId) ?: return@withContext Result.failure(
                IllegalArgumentException("List not found")
            )

            val updatedList = existingList.copy(
                name = name ?: existingList.name,
                description = description ?: existingList.description,
                isCollapsed = isCollapsed ?: existingList.isCollapsed,
                updatedAt = System.currentTimeMillis()
            )

            listDao.updateList(updatedList)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a list.
     */
    suspend fun deleteList(listId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val list = listDao.getList(listId) ?: return@withContext Result.failure(
                IllegalArgumentException("List not found")
            )

            listDao.deleteList(list)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets a list by ID.
     */
    suspend fun getListById(listId: String): Result<SavedList?> = withContext(Dispatchers.IO) {
        try {
            val list = listDao.getList(listId)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cleanupUnparentedElements() {
        if (listDao.getRootList() == null) {
            listDao.insertList(
                SavedList.createList(
                    context.getString(earth.maps.cardinal.R.string.saved_places_title_case),
                    isRoot = true,
                )
            )
        }
        val rootList = listDao.getRootList()
        if (rootList == null) {
            Log.e(TAG, "Failed to find a root list immediately after ensuring one exists.")
            return
        }
        val potentiallyUnparentedLists = listDao.getAllLists().map { it.id }.toMutableSet()
        val potentiallyUnparentedPlaces = placeDao.getAllPlaces().map { it.id }.toMutableSet()
        for (list in listDao.getAllLists()) {
            if (list.isRoot) {
                potentiallyUnparentedLists.remove(list.id)
                continue
            }
            val listItems = listItemDao.getItemsInList(list.id)
            for (listItem in listItems) {
                potentiallyUnparentedLists.remove(listItem.itemId)
                potentiallyUnparentedPlaces.remove(listItem.itemId)
            }
        }
        for (unparentedList in potentiallyUnparentedLists) {
            addItemToList(rootList.id, itemId = unparentedList, itemType = ItemType.LIST)
        }
        for (unparentedPlace in potentiallyUnparentedPlaces) {
            addItemToList(rootList.id, itemId = unparentedPlace, itemType = ItemType.PLACE)
        }
    }

    /**
     * Gets the root list.
     */
    suspend fun getRootList(): Result<SavedList?> = withContext(Dispatchers.IO) {
        try {
            val list = listDao.getRootList()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets child lists of a parent list.
     */
    fun getChildLists(parentListId: String): Flow<List<SavedList>> {
        return listDao.getChildLists(parentListId)
    }

    /**
     * Adds an item to a list.
     */
    suspend fun addItemToList(
        listId: String, itemId: String, itemType: ItemType
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Get the current max position in the list
            val currentItems = listItemDao.getItemsInList(listId)
            val maxPosition = if (currentItems.isNotEmpty()) {
                currentItems.maxByOrNull { it.position }?.position ?: -1
            } else {
                -1
            }
            val newPosition = maxPosition + 1

            val listItem = ListItem(
                listId = listId,
                itemId = itemId,
                itemType = itemType,
                position = newPosition,
                addedAt = System.currentTimeMillis()
            )

            listItemDao.insertItem(listItem)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Removes an item from a list.
     */
    suspend fun removeItemFromList(
        listId: String, itemId: String, itemType: ItemType
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val listItem = ListItem(
                listId = listId,
                itemId = itemId,
                itemType = itemType,
                position = 0, // Not used in deletion
                addedAt = 0 // Not used in deletion
            )

            listItemDao.deleteItem(listItem)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Moves an item from one list to another.
     */
    suspend fun moveItem(
        itemId: String, itemType: ItemType, oldListId: String, newListId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Get the current max position in the new list
            val currentItems = listItemDao.getItemsInList(newListId)
            val maxPosition = if (currentItems.isNotEmpty()) {
                currentItems.maxByOrNull { it.position }?.position ?: -1
            } else {
                -1
            }
            val newPosition = maxPosition + 1

            listItemDao.moveItem(itemId, itemType, oldListId, newListId, newPosition)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reorders items in a list.
     */
    suspend fun reorderItems(
        listId: String, items: List<ListItem>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            listItemDao.reorderItems(listId, items)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets the hierarchical content of a list for UI display.
     * Returns a list of ListContent items (either PlaceContent or ListContentItem).
     */
    suspend fun getListContent(listId: String): Result<List<ListContent>> =
        withContext(Dispatchers.IO) {
            try {
                val items = listItemDao.getItemsInList(listId)
                val contentList = mutableListOf<ListContent>()

                for (item in items) {
                    when (item.itemType) {
                        ItemType.PLACE -> {
                            val savedPlace = placeDao.getPlace(item.itemId)
                            if (savedPlace != null) {
                                contentList.add(
                                    PlaceContent(
                                        id = savedPlace.id,
                                        name = savedPlace.customName ?: savedPlace.name,
                                        type = savedPlace.type,
                                        icon = savedPlace.icon,
                                        customName = savedPlace.customName,
                                        customDescription = savedPlace.customDescription,
                                        position = item.position
                                    )
                                )
                            }
                        }

                        ItemType.LIST -> {
                            val savedList = listDao.getList(item.itemId)
                            if (savedList != null) {
                                contentList.add(
                                    ListContentItem(
                                        id = savedList.id,
                                        name = savedList.name,
                                        description = savedList.description,
                                        isCollapsed = savedList.isCollapsed,
                                        position = item.position
                                    )
                                )
                            }
                        }
                    }
                }

                // Sort by position
                contentList.sortBy { it.position }
                Result.success(contentList)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    companion object {
        private const val TAG = "SavedListRepository"
    }
}
