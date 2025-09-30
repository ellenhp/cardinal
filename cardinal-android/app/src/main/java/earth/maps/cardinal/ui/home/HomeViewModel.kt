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

package earth.maps.cardinal.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import earth.maps.cardinal.data.GeocodeResult
import earth.maps.cardinal.data.LocationRepository
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.ViewportRepository
import earth.maps.cardinal.data.deduplicateSearchResults
import earth.maps.cardinal.data.room.SavedPlaceDao
import earth.maps.cardinal.data.room.SavedPlaceRepository
import earth.maps.cardinal.geocoding.GeocodingService
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val placeDao: SavedPlaceDao,
    private val geocodingService: GeocodingService,
    private val viewportRepository: ViewportRepository,
    private val locationRepository: LocationRepository,
    private val savedPlaceRepository: SavedPlaceRepository,
) : ViewModel() {

    // Whether the home screen is in a search state.
    private val _searchExpanded = MutableStateFlow(false)

    val searchExpanded: Flow<Boolean> = _searchExpanded

    // Search query flow for debouncing
    private val _searchQueryFlow = MutableStateFlow("")
    private val searchQueryFlow: StateFlow<String> = _searchQueryFlow.asStateFlow()

    var searchQuery by mutableStateOf(
        TextFieldValue()
    )

    val geocodeResults = MutableStateFlow<List<GeocodeResult>>(emptyList())

    val geocodePlaces = geocodeResults.map { list -> list.map { geocodeResultToPlace(it) } }

    var isSearching by mutableStateOf(false)
        private set

    var searchError by mutableStateOf<String?>(null)
        private set

    init {
        // Set up debounced search
        searchQueryFlow.debounce(300) // 300ms delay
            .distinctUntilChanged().onEach { query ->
                if (query.isNotEmpty()) {
                    performSearch(query)
                } else {
                    // Clear results when query is empty
                    geocodeResults.value = emptyList()
                    searchError = null
                }
            }.launchIn(viewModelScope)
    }

    fun updateSearchQuery(query: TextFieldValue) {
        searchQuery = query
        _searchQueryFlow.value = query.text
    }

    fun geocodeResultToPlace(result: GeocodeResult): Place {
        return locationRepository.createSearchResultPlace(result)
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            isSearching = true
            searchError = null
            try {
                // Use current viewport center as focus point for viewport biasing
                val focusPoint = viewportRepository.viewportCenter.value
                geocodingService.geocode(query, focusPoint).collect { results ->
                    geocodeResults.value = deduplicateSearchResults(results)
                    isSearching = false
                }
            } catch (e: Exception) {
                // Handle error
                searchError = e.message ?: "An error occurred during search"
                geocodeResults.value = emptyList()
                isSearching = false
            }
        }
    }

    fun pinnedPlaces(): Flow<List<Place>> {
        return placeDao.getAllPlacesAsFlow().map { placeList ->
            placeList.filter { it.isPinned }.map { savedPlaceRepository.toPlace(it) }
        }
    }

    fun collapseSearch() {
        _searchExpanded.value = false
    }

    fun expandSearch() {
        _searchExpanded.value = true
    }
}
