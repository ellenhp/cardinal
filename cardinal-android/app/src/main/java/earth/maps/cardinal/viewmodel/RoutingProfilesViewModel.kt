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
