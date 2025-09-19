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

package earth.maps.cardinal.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class AppPreferenceRepository @Inject constructor(
    context: Context
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val appPreferences = AppPreferences(context)

    private val _contrastLevel = MutableStateFlow(AppPreferences.CONTRAST_LEVEL_STANDARD)
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

    private val _offlineMode = MutableStateFlow(AppPreferences.OFFLINE_MODE_DISABLED)
    val offlineMode: StateFlow<Boolean> = _offlineMode.asStateFlow()

    private val _distanceUnit = MutableStateFlow(AppPreferences.DISTANCE_UNIT_METRIC)
    val distanceUnit: StateFlow<Int> = _distanceUnit.asStateFlow()

    // Pelias API configuration
    private val _peliasApiConfig = MutableStateFlow(
        ApiConfiguration(
            baseUrl = appPreferences.loadPeliasBaseUrl(),
            apiKey = appPreferences.loadPeliasApiKey()
        )
    )
    val peliasApiConfig: StateFlow<ApiConfiguration> = _peliasApiConfig.asStateFlow()

    // Valhalla API configuration
    private val _valhallaApiConfig = MutableStateFlow(
        ApiConfiguration(
            baseUrl = appPreferences.loadValhallaBaseUrl(),
            apiKey = appPreferences.loadValhallaApiKey()
        )
    )
    val valhallaApiConfig: StateFlow<ApiConfiguration> = _valhallaApiConfig.asStateFlow()

    init {
        loadContrastLevel()
        loadAnimationSpeed()
        loadOfflineMode()
        loadDistanceUnit()
        loadApiConfigurations()
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

    fun setOfflineMode(offlineMode: Boolean) {
        _offlineMode.value = offlineMode
        viewModelScope.launch {
            appPreferences.saveOfflineMode(offlineMode)
        }
    }

    private fun loadOfflineMode() {
        val offlineMode = appPreferences.loadOfflineMode()
        _offlineMode.value = offlineMode
    }

    fun setDistanceUnit(distanceUnit: Int) {
        _distanceUnit.value = distanceUnit
        viewModelScope.launch {
            appPreferences.saveDistanceUnit(distanceUnit)
        }
    }

    private fun loadDistanceUnit() {
        val distanceUnit = appPreferences.loadDistanceUnit()
        _distanceUnit.value = distanceUnit
    }

    private fun loadApiConfigurations() {
        // Load Pelias configuration
        val peliasBaseUrl = appPreferences.loadPeliasBaseUrl()
        val peliasApiKey = appPreferences.loadPeliasApiKey()
        _peliasApiConfig.value = ApiConfiguration(peliasBaseUrl, peliasApiKey)

        // Load Valhalla configuration
        val valhallaBaseUrl = appPreferences.loadValhallaBaseUrl()
        val valhallaApiKey = appPreferences.loadValhallaApiKey()
        _valhallaApiConfig.value = ApiConfiguration(valhallaBaseUrl, valhallaApiKey)
    }

    // Pelias API configuration methods
    fun setPeliasBaseUrl(baseUrl: String) {
        val currentConfig = _peliasApiConfig.value
        val newConfig = ApiConfiguration(baseUrl, currentConfig.apiKey)
        _peliasApiConfig.value = newConfig
        viewModelScope.launch {
            appPreferences.savePeliasBaseUrl(baseUrl)
        }
    }

    fun setPeliasApiKey(apiKey: String?) {
        val currentConfig = _peliasApiConfig.value
        val newConfig = ApiConfiguration(currentConfig.baseUrl, apiKey)
        _peliasApiConfig.value = newConfig
        viewModelScope.launch {
            appPreferences.savePeliasApiKey(apiKey)
        }
    }

    // Valhalla API configuration methods
    fun setValhallaBaseUrl(baseUrl: String) {
        val currentConfig = _valhallaApiConfig.value
        val newConfig = ApiConfiguration(baseUrl, currentConfig.apiKey)
        _valhallaApiConfig.value = newConfig
        viewModelScope.launch {
            appPreferences.saveValhallaBaseUrl(baseUrl)
        }
    }

    fun setValhallaApiKey(apiKey: String?) {
        val currentConfig = _valhallaApiConfig.value
        val newConfig = ApiConfiguration(currentConfig.baseUrl, apiKey)
        _valhallaApiConfig.value = newConfig
        viewModelScope.launch {
            appPreferences.saveValhallaApiKey(apiKey)
        }
    }
}
