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
import earth.maps.cardinal.data.RoutingMode
import earth.maps.cardinal.data.room.RoutingProfileRepository
import earth.maps.cardinal.routing.AutoRoutingOptions
import earth.maps.cardinal.routing.CyclingRoutingOptions
import earth.maps.cardinal.routing.MotorScooterRoutingOptions
import earth.maps.cardinal.routing.MotorcycleRoutingOptions
import earth.maps.cardinal.routing.PedestrianRoutingOptions
import earth.maps.cardinal.routing.RoutingOptions
import earth.maps.cardinal.routing.TruckRoutingOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileEditorViewModel @Inject constructor(
    private val repository: RoutingProfileRepository
) : ViewModel() {

    private val _profileName = MutableStateFlow("")
    val profileName: StateFlow<String> = _profileName.asStateFlow()

    private val _selectedMode = MutableStateFlow(RoutingMode.AUTO)
    val selectedMode: StateFlow<RoutingMode> = _selectedMode.asStateFlow()

    private val _routingOptions = MutableStateFlow<RoutingOptions>(AutoRoutingOptions())
    val routingOptions: StateFlow<RoutingOptions> = _routingOptions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isNewProfile = MutableStateFlow(true)
    val isNewProfile: StateFlow<Boolean> = _isNewProfile.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

    private var currentProfileId: String? = null

    // Track initial values for change detection
    private var initialProfileName: String = ""
    private var initialSelectedMode: RoutingMode = RoutingMode.AUTO
    private var initialRoutingOptions: RoutingOptions = AutoRoutingOptions()

    fun loadProfile(profileId: String?) {
        if (profileId == null) {
            // New profile
            _isNewProfile.value = true
            _profileName.value = ""
            _selectedMode.value = RoutingMode.AUTO
            _routingOptions.value = AutoRoutingOptions()

            // Set initial values for new profile
            initialProfileName = ""
            initialSelectedMode = RoutingMode.AUTO
            initialRoutingOptions = AutoRoutingOptions()
            _hasUnsavedChanges.value = false
        } else {
            // Existing profile
            _isNewProfile.value = false
            currentProfileId = profileId
            loadExistingProfile(profileId)
        }
    }

    private fun loadExistingProfile(profileId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getProfileById(profileId).fold(
                onSuccess = { profile ->
                    profile?.let {
                        val name = it.name
                        val mode = RoutingMode.entries.find { mode ->
                            mode.value == it.routingMode
                        } ?: RoutingMode.AUTO
                        val options = repository.deserializeOptions(it.routingMode, it.optionsJson)
                            ?: return@let

                        // Update state
                        _profileName.value = name
                        _selectedMode.value = mode
                        _routingOptions.value = options

                        // Capture initial values for change detection
                        initialProfileName = name
                        initialSelectedMode = mode
                        initialRoutingOptions = options
                        _hasUnsavedChanges.value = false
                    } ?: run {
                        // Profile doesn't exist, treat as new profile
                        _isNewProfile.value = true
                        _profileName.value = ""
                        _selectedMode.value = RoutingMode.AUTO
                        _routingOptions.value = AutoRoutingOptions()

                        // Set initial values for new profile
                        initialProfileName = ""
                        initialSelectedMode = RoutingMode.AUTO
                        initialRoutingOptions = AutoRoutingOptions()
                        _hasUnsavedChanges.value = false
                    }
                },
                onFailure = { error ->
                    _error.value = "Failed to load profile: ${error.message}"
                }
            )

            _isLoading.value = false
        }
    }

    fun updateProfileName(name: String) {
        _profileName.value = name
        updateHasUnsavedChanges()
    }

    fun updateRoutingMode(mode: RoutingMode) {
        if (mode != _selectedMode.value) {
            // Create new options for the selected mode
            createDefaultOptionsForMode(mode)?.let {
                _selectedMode.value = mode
                _routingOptions.value = it
                updateHasUnsavedChanges()
            }
        }
    }

    private fun createDefaultOptionsForMode(mode: RoutingMode): RoutingOptions? {
        return when (mode) {
            RoutingMode.AUTO -> AutoRoutingOptions()
            RoutingMode.TRUCK -> TruckRoutingOptions()
            RoutingMode.MOTOR_SCOOTER -> MotorScooterRoutingOptions()
            RoutingMode.MOTORCYCLE -> MotorcycleRoutingOptions()
            RoutingMode.BICYCLE -> CyclingRoutingOptions()
            RoutingMode.PEDESTRIAN -> PedestrianRoutingOptions()
            RoutingMode.PUBLIC_TRANSPORT -> null
        }
    }

    fun updateRoutingOptions(options: RoutingOptions) {
        _routingOptions.value = options
        updateHasUnsavedChanges()
    }

    fun saveProfile(onSuccess: () -> Unit) {
        if (_profileName.value.isBlank()) {
            _error.value = "Profile name cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = if (_isNewProfile.value) {
                repository.createProfile(
                    _profileName.value,
                    _selectedMode.value,
                    _routingOptions.value
                )
            } else {
                currentProfileId?.let { profileId ->
                    repository.updateProfile(profileId, _profileName.value, _routingOptions.value)
                } ?: Result.failure(Exception("Profile ID not found"))
            }

            result.fold(
                onSuccess = {
                    onSuccess()
                },
                onFailure = { error ->
                    _error.value = "Failed to save profile: ${error.message}"
                }
            )

            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun updateHasUnsavedChanges() {
        _hasUnsavedChanges.value = _profileName.value != initialProfileName ||
                _selectedMode.value != initialSelectedMode ||
                _routingOptions.value != initialRoutingOptions
    }
}
