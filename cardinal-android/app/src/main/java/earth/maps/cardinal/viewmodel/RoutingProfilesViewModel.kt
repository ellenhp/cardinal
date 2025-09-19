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
import earth.maps.cardinal.data.RoutingProfile
import earth.maps.cardinal.data.RoutingProfileRepository
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
