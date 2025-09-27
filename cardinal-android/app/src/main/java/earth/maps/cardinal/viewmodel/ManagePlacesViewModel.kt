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
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.room.ItemType
import earth.maps.cardinal.data.room.ListContent
import earth.maps.cardinal.data.room.ListContentItem
import earth.maps.cardinal.data.room.PlaceContent
import earth.maps.cardinal.data.room.SavedList
import earth.maps.cardinal.data.room.SavedListRepository
import earth.maps.cardinal.data.room.SavedPlace
import earth.maps.cardinal.data.room.SavedPlaceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManagePlacesViewModel @Inject constructor(
    private var savedPlaceRepository: SavedPlaceRepository,
    private val savedListRepository: SavedListRepository
) : ViewModel() {

    // Navigation stack to track the path through lists
    private val _navigationStack = MutableStateFlow<List<String>>(emptyList())
    val navigationStack: StateFlow<List<String>> = _navigationStack

    // Current list being displayed
    private val _currentListId = MutableStateFlow<String?>(null)
    val currentListId: StateFlow<String?> = _currentListId

    // Current list details
    private val _currentList = MutableStateFlow<SavedList?>(null)
    val currentList: StateFlow<SavedList?> = _currentList

    // Selected items for management (set of item IDs)
    private val _selectedItems = MutableStateFlow<Set<String>>(emptySet())
    val selectedItems: StateFlow<Set<String>> = _selectedItems

    // Check if all items in the current list are selected
    val isAllSelected: Flow<Boolean> = combine(
        selectedItems,
        _currentList.flatMapLatest { list ->
            list?.let { savedListRepository.getItemIdsInListAsFlow(it.id) } ?: flowOf(emptySet())
        }
    ) { selected, all -> selected == all }

    // Get the current list name for display
    val currentListName: Flow<String?> = _currentList.map { list ->
        list?.name ?: savedListRepository.getRootList().getOrNull()?.name
    }

    // Get the content of the current list
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentListContent: Flow<List<Flow<ListContent?>>?> = _currentList.flatMapLatest { list ->
        list?.let { savedListRepository.getListContent(it.id) } ?: flowOf(null)
    }

    init {
        viewModelScope.launch {
            // Initialize with root list
            val rootList = savedListRepository.getRootList().getOrNull()
            if (rootList != null) {
                navigateToList(rootList.id)
            }
        }
    }

    fun setInitialList(listId: String?) {
        viewModelScope.launch {
            if (listId != null) {
                navigateToList(listId)
            } else {
                // Use root list as default
                val rootList = savedListRepository.getRootList().getOrNull()
                if (rootList != null) {
                    navigateToList(rootList.id)
                }
            }
        }
    }

    fun navigateToList(listId: String) {
        viewModelScope.launch {
            val list = savedListRepository.getListById(listId).getOrNull()
            if (list != null) {
                _navigationStack.value = _navigationStack.value + listId
                _currentListId.value = listId
                _currentList.value = list
                clearSelection()
            }
        }
    }

    fun navigateBack(): Boolean {
        val currentStack = _navigationStack.value
        if (currentStack.size > 1) {
            val previousStack = currentStack.dropLast(1)
            _navigationStack.value = previousStack
            _currentListId.value = previousStack.lastOrNull()
            viewModelScope.launch {
                _currentListId.value?.let { listId ->
                    _currentList.value = savedListRepository.getListById(listId).getOrNull()
                }
            }
            clearSelection()
            return true
        }
        return false // At root level, can't go back
    }

    fun canNavigateBack(): Boolean = _navigationStack.value.size > 1

    fun convertToPlace(place: SavedPlace): Place {
        return savedPlaceRepository.toPlace(place)
    }

    // Selection management functions
    fun toggleSelection(itemId: String) {
        _selectedItems.value = if (_selectedItems.value.contains(itemId)) {
            _selectedItems.value - itemId
        } else {
            _selectedItems.value + itemId
        }
    }

    fun selectAll() {
        viewModelScope.launch {
            val listId = _currentListId.value ?: return@launch
            val allIds =
                savedListRepository.getItemsInList(listId).getOrNull()?.map { it.itemId }?.toSet()
                    ?: emptySet()
            _selectedItems.value = allIds
        }
    }

    fun clearSelection() {
        _selectedItems.value = emptySet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val listId = _currentListId.value ?: return@launch
            val itemsResult = savedListRepository.getItemsInList(listId)
            if (itemsResult.isFailure) return@launch
            val items = itemsResult.getOrNull() ?: return@launch

            _selectedItems.value.forEach { itemId ->
                val item = items.find { it.itemId == itemId } ?: return@forEach
                when (item.itemType) {
                    ItemType.PLACE -> savedPlaceRepository.deletePlace(itemId)

                    ItemType.LIST -> savedListRepository.deleteList(itemId)
                }
            }
            clearSelection()
        }
    }

    // Placeholder for creating new list with selected places
    fun createNewListWithSelected() {
        // TODO: Implement creating new list with selected places
    }

    // Returns a breadcrumb path for display
    suspend fun getBreadcrumbNames(): List<String> {
        val names = mutableListOf<String>()
        _navigationStack.value.forEach { listId ->
            val list = savedListRepository.getListById(listId).getOrNull()
            list?.let { names.add(it.name) }
        }
        return names
    }
}
