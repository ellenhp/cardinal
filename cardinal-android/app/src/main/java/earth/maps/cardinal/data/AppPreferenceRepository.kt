package earth.maps.cardinal.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class AppPreferenceRepository @Inject constructor(
    context: Context
) : ViewModel() {

    private val _contrastLevel = MutableStateFlow(AppPreferences.CONTRAST_LEVEL_HIGH)
    val contrastLevel: StateFlow<Int> = _contrastLevel.asStateFlow()

    private val _animationSpeed = MutableStateFlow(AppPreferences.ANIMATION_SPEED_NORMAL)
    val animationSpeed: StateFlow<Int> = _animationSpeed.asStateFlow()

    val animationSpeedDurationValue: Duration
        get() = when (_animationSpeed.value) {
            AppPreferences.ANIMATION_SPEED_SLOW -> 2000.milliseconds
            AppPreferences.ANIMATION_SPEED_NORMAL -> 1000.milliseconds
            AppPreferences.ANIMATION_SPEED_FAST -> 500.milliseconds
            else -> 1000.milliseconds
        }

    private val appPreferences = AppPreferences(context)

    init {
        loadContrastLevel()
        loadAnimationSpeed()
    }

    fun setContrastLevel(level: Int) {
        _contrastLevel.value = level
        viewModelScope.launch {
            appPreferences.saveContrastLevel(level)
        }
    }

    private fun loadContrastLevel() {
        val level = appPreferences.loadContrastLevel()
        _contrastLevel.value = level
    }

    fun setAnimationSpeed(speed: Int) {
        _animationSpeed.value = speed
        viewModelScope.launch {
            appPreferences.saveAnimationSpeed(speed)
        }
    }

    private fun loadAnimationSpeed() {
        val speed = appPreferences.loadAnimationSpeed()
        _animationSpeed.value = speed
    }
}
