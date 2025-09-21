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

package earth.maps.cardinal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import earth.maps.cardinal.data.room.ItemType
import earth.maps.cardinal.data.room.ListItem
import earth.maps.cardinal.data.room.ListItemDao
import earth.maps.cardinal.data.room.SavedList
import earth.maps.cardinal.data.room.SavedListDao
import earth.maps.cardinal.data.room.SavedListRepository
import earth.maps.cardinal.data.room.SavedPlace
import earth.maps.cardinal.data.room.SavedPlaceDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedPlacesViewModel @Inject constructor(
    private val savedListRepository: SavedListRepository,
    private val savedListDao: SavedListDao,
    private val savedPlaceDao: SavedPlaceDao,
    private val listItemDao: ListItemDao,
) : ViewModel() {

    private var currentListId: String? = null

    fun setListId(itemId: String) {
        currentListId = itemId
    }

    fun reorderItems(items: List<ListItem>) {
        viewModelScope.launch {
            currentListId?.let { listId ->
                val listItems = items.mapIndexed { index, item ->
                    ListItem(
                        listId = listId,
                        itemId = item.itemId,
                        itemType = item.itemType,
                        position = index,
                        addedAt = System.currentTimeMillis()
                    )
                }

                savedListRepository.reorderItems(listId, listItems)
            }
        }
    }

    fun toggleListCollapse(listId: String) {
        viewModelScope.launch {
            savedListDao.toggleExpanded(listId)
        }
    }

    fun updateItemName(itemId: String, newName: String) {
    }

    fun reparentPlaceToList(itemId: String, targetListId: String) {
        viewModelScope.launch {
            listItemDao.moveItem(itemId, targetListId, newPosition = 0)
        }
    }

    fun addNewListToRoot(name: String) {
        viewModelScope.launch {
            // Create a new list
            val result = savedListRepository.createList(name)
            if (result.isSuccess) {
                val newListId = result.getOrNull()
                newListId?.let { id ->
                    // Get the root list
                    val rootList = savedListRepository.getRootList()
                    if (rootList.isSuccess) {
                        rootList.getOrNull()?.let { root ->
                            // Add the new list to the root list
                            savedListRepository.addItemToList(root.id, id, ItemType.LIST)
                        }
                    }
                }
            }
        }
    }

    fun observeListChildren(id: String): Flow<List<ListItem>> {
        return listItemDao.getItemsInListAsFlow(id)
    }

    fun observeRootList(): Flow<SavedList?> {
        return savedListDao.getRootListAsFlow()
    }

    fun observeList(listId: String): Flow<SavedList?> {
        return savedListDao.getListAsFlow(listId)
    }

    fun observePlace(placeId: String): Flow<SavedPlace> {
        return savedPlaceDao.getPlaceAsFlow(placeId)
    }

    fun observeIsExpanded(listId: String): Flow<Boolean> {
        return savedListDao.getListAsFlow(listId).map { it?.isCollapsed == false }
    }
}

sealed class SavedPlacesUiState {
    object Loading : SavedPlacesUiState()
    data class Success(val content: SavedList) : SavedPlacesUiState()
    data class Error(val message: String) : SavedPlacesUiState()
}
