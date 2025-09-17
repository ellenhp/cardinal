package earth.maps.cardinal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import earth.maps.cardinal.data.RoutingMode
import earth.maps.cardinal.data.RoutingProfile
import earth.maps.cardinal.data.RoutingProfileRepository
import earth.maps.cardinal.routing.RoutingOptions
import kotlinx.coroutines.flow.Flow
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
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun createProfile(name: String, routingMode: RoutingMode, options: RoutingOptions) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.createProfile(name, routingMode, options).fold(
                onSuccess = {
                    // Profile created successfully
                },
                onFailure = { error ->
                    _error.value = "Failed to create profile: ${error.message}"
                }
            )

            _isLoading.value = false
        }
    }

    fun updateProfile(profileId: String, name: String? = null, options: RoutingOptions? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.updateProfile(profileId, name, options).fold(
                onSuccess = {
                    // Profile updated successfully
                },
                onFailure = { error ->
                    _error.value = "Failed to update profile: ${error.message}"
                }
            )

            _isLoading.value = false
        }
    }

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

    fun getProfilesForMode(routingMode: RoutingMode): Flow<List<RoutingProfile>> {
        return repository.getProfilesForMode(routingMode)
    }

    fun getOrCreateDefaultProfile(routingMode: RoutingMode, onResult: (Result<Pair<RoutingProfile, RoutingOptions>>) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = repository.getOrCreateDefaultProfile(routingMode)
            onResult(result)

            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}
