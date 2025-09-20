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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import earth.maps.cardinal.data.room.ListContent
import earth.maps.cardinal.data.room.ListContentItem
import earth.maps.cardinal.data.room.ListItem
import earth.maps.cardinal.data.room.PlaceContent
import earth.maps.cardinal.data.room.SavedListRepository
import earth.maps.cardinal.data.room.SavedPlaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedPlacesViewModel @Inject constructor(
    private val savedListRepository: SavedListRepository,
    private val savedPlaceRepository: SavedPlaceRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<SavedPlacesUiState>(SavedPlacesUiState.Loading)
    val uiState: StateFlow<SavedPlacesUiState> = _uiState.asStateFlow()
    
    var isEditMode by mutableStateOf(false)
        private set
        
    var selectedItems by mutableStateOf<Set<String>>(emptySet())
        private set
        
    private var currentListId: String? = null
    
    init {
        loadRootList()
    }
    
    fun loadRootList() {
        viewModelScope.launch {
            _uiState.value = SavedPlacesUiState.Loading
            
            // Get or create the root "Saved Places" list
            val rootList = savedListRepository.getRootList().getOrNull()
            
            if (rootList != null) {
                currentListId = rootList.id
                loadListContent(rootList.id)
            } else {
                // Create the root list if it doesn't exist
                val result = savedListRepository.createList(
                    name = "Saved Places",
                    description = "All your saved places",
                    isRoot = true
                )
                
                if (result.isSuccess) {
                    currentListId = result.getOrNull()
                    loadListContent(currentListId!!)
                } else {
                    _uiState.value = SavedPlacesUiState.Error("Failed to create root list")
                }
            }
        }
    }
    
    fun loadListContent(listId: String) {
        viewModelScope.launch {
            _uiState.value = SavedPlacesUiState.Loading
            
            val result = savedListRepository.getListContent(listId)
            if (result.isSuccess) {
                _uiState.value = SavedPlacesUiState.Success(result.getOrNull() ?: emptyList())
            } else {
                _uiState.value = SavedPlacesUiState.Error("Failed to load list content")
            }
        }
    }
    
    fun toggleEditMode() {
        isEditMode = !isEditMode
        if (!isEditMode) {
            selectedItems = emptySet()
        }
    }
    
    fun toggleItemSelection(itemId: String) {
        selectedItems = if (itemId in selectedItems) {
            selectedItems - itemId
        } else {
            selectedItems + itemId
        }
    }
    
    fun selectAllItems() {
        when (val state = _uiState.value) {
            is SavedPlacesUiState.Success -> {
                selectedItems = state.content.map { it.id }.toSet()
            }
            else -> {}
        }
    }
    
    fun clearSelection() {
        selectedItems = emptySet()
    }
    
    fun reorderItems(items: List<ListContent>) {
        viewModelScope.launch {
            currentListId?.let { listId ->
                val listItems = items.mapIndexed { index, content ->
                    ListItem(
                        listId = listId,
                        itemId = content.id,
                        itemType = when (content) {
                            is PlaceContent -> earth.maps.cardinal.data.room.ItemType.PLACE
                            is ListContentItem -> earth.maps.cardinal.data.room.ItemType.LIST
                        },
                        position = index,
                        addedAt = System.currentTimeMillis()
                    )
                }
                
                savedListRepository.reorderItems(listId, listItems)
            }
        }
    }
    
    fun deleteSelectedItems() {
        viewModelScope.launch {
            currentListId?.let { listId ->
                selectedItems.forEach { itemId ->
                    // Determine item type by checking existing content
                    when (val state = _uiState.value) {
                        is SavedPlacesUiState.Success -> {
                            val item = state.content.find { it.id == itemId }
                            if (item != null) {
                                val itemType = when (item) {
                                    is PlaceContent -> earth.maps.cardinal.data.room.ItemType.PLACE
                                    is ListContentItem -> earth.maps.cardinal.data.room.ItemType.LIST
                                }
                                savedListRepository.removeItemFromList(listId, itemId, itemType)
                            }
                        }
                        else -> {}
                    }
                }
                clearSelection()
                loadListContent(listId)
            }
        }
    }
    
    fun moveSelectedItems(targetListId: String) {
        viewModelScope.launch {
            currentListId?.let { sourceListId ->
                selectedItems.forEach { itemId ->
                    // Determine item type by checking existing content
                    when (val state = _uiState.value) {
                        is SavedPlacesUiState.Success -> {
                            val item = state.content.find { it.id == itemId }
                            if (item != null) {
                                val itemType = when (item) {
                                    is PlaceContent -> earth.maps.cardinal.data.room.ItemType.PLACE
                                    is ListContentItem -> earth.maps.cardinal.data.room.ItemType.LIST
                                }
                                savedListRepository.moveItem(itemId, itemType, sourceListId, targetListId)
                            }
                        }
                        else -> {}
                    }
                }
                clearSelection()
                loadListContent(sourceListId)
            }
        }
    }
    
    fun toggleListCollapse(listId: String, isCollapsed: Boolean) {
        viewModelScope.launch {
            savedListRepository.updateList(listId, isCollapsed = !isCollapsed)
            currentListId?.let { loadListContent(it) }
        }
    }
    
    fun updateItemName(itemId: String, newName: String) {
        viewModelScope.launch {
            // This would require updating either the SavedPlace or SavedList
            // For now, we'll just reload the content
            currentListId?.let { loadListContent(it) }
        }
    }
}

sealed class SavedPlacesUiState {
    object Loading : SavedPlacesUiState()
    data class Success(val content: List<ListContent>) : SavedPlacesUiState()
    data class Error(val message: String) : SavedPlacesUiState()
}
