package earth.maps.cardinal.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContrastRepository @Inject constructor(
    context: Context
) : ViewModel() {

    private val _contrastLevel = MutableStateFlow(ContrastPreferences.CONTRAST_LEVEL_HIGH)
    val contrastLevel: StateFlow<Int> = _contrastLevel.asStateFlow()

    private val contrastPreferences = ContrastPreferences(context)

    init {
        loadContrastLevel()
    }

    fun setContrastLevel(level: Int) {
        _contrastLevel.value = level
        viewModelScope.launch {
            contrastPreferences.saveContrastLevel(level)
        }
    }

    private fun loadContrastLevel() {
        val level = contrastPreferences.loadContrastLevel()
        _contrastLevel.value = level
    }
}
