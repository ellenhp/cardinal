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

package earth.maps.cardinal.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import earth.maps.cardinal.data.CutPasteRepository
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.room.ItemType
import earth.maps.cardinal.data.room.ListContent
import earth.maps.cardinal.data.room.ListItemDao
import earth.maps.cardinal.data.room.SavedList
import earth.maps.cardinal.data.room.SavedListRepository
import earth.maps.cardinal.data.room.SavedPlaceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManagePlacesViewModel @Inject constructor(
    private val savedPlaceRepository: SavedPlaceRepository,
    private val savedListRepository: SavedListRepository,
    private val listItemDao: ListItemDao,
    private val cutPasteRepository: CutPasteRepository,
) : ViewModel() {

    private data class SelectedItem(val itemId: String, val itemType: ItemType) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SelectedItem) return false
            return itemId == other.itemId
        }

        override fun hashCode(): Int {
            return itemId.hashCode()
        }
    }

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
    @OptIn(ExperimentalCoroutinesApi::class)
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

    val clipboard: Flow<Set<String>> = cutPasteRepository.clipboard

    suspend fun setInitialList(listId: String?) {
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

    private suspend fun navigateToList(listId: String) {
        val list = savedListRepository.getListById(listId).getOrNull()
        if (list != null) {
            _currentListId.value = listId
            _currentList.value = list
            clearSelection()
        }
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
    fun createNewListWithSelected(name: String) {
        viewModelScope.launch {
            val currentListId = _currentListId.value ?: return@launch
            val newListId =
                savedListRepository.createList(name = name, parentId = currentListId).getOrNull()
                    ?: return@launch
            val itemsInListCount = listItemDao.getItemsInList(newListId).size

            _selectedItems.value.forEachIndexed { itemIndex, itemId ->
                listItemDao.moveItem(itemId, newListId = newListId, itemIndex + itemsInListCount)
            }
        }
    }

    fun cutSelected() {
        val newClipboard = _selectedItems.value.toSet()
        cutPasteRepository.clipboard.value = newClipboard
    }

    fun pasteSelected() {
        val currentListId = currentListId.value ?: return
        viewModelScope.launch {
            val itemsInListCount = listItemDao.getItemsInList(currentListId).size

            cutPasteRepository.clipboard.value.forEachIndexed { index, id ->
                listItemDao.moveItem(id, currentListId, itemsInListCount + index)
            }
            cutPasteRepository.clipboard.value = emptySet()
        }
    }

    fun updatePlace(
        id: String,
        customName: String?,
        customDescription: String?,
        isPinned: Boolean?
    ) {
        viewModelScope.launch {
            savedPlaceRepository.updatePlace(id, customName, customDescription, isPinned)
        }
    }

    fun updateList(id: String, name: String?, description: String?) {
        viewModelScope.launch {
            savedListRepository.updateList(id, name, description)
        }
    }

    suspend fun getSavedPlace(id: String): Place? {
        return savedPlaceRepository.getPlaceById(id).getOrNull()
            ?.let { savedPlaceRepository.toPlace(it) }
    }
}
