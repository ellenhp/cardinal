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

package earth.maps.cardinal.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import earth.maps.cardinal.data.room.RoutingProfile
import earth.maps.cardinal.data.room.RoutingProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoutingProfilesViewModel @Inject constructor(
    private val repository: RoutingProfileRepository
) : ViewModel() {

    val allProfiles: StateFlow<List<RoutingProfile>> = repository.allProfiles

    private val _isLoading = MutableStateFlow(false)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.deleteProfile(profileId).fold(
                onSuccess = {
                    // Profile deleted successfully
                },
                onFailure = { error ->
                    _error.value = "Failed to delete profile: ${error.message}"
                }
            )

            _isLoading.value = false
        }
    }

    fun setDefaultProfile(profileId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.setDefaultProfile(profileId).fold(
                onSuccess = {
                    // Default profile set successfully
                },
                onFailure = { error ->
                    _error.value = "Failed to set default profile: ${error.message}"
                }
            )

            _isLoading.value = false
        }
    }
}
