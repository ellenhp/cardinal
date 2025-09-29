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
import android.content.SharedPreferences
import android.text.format.DateFormat
import androidx.core.content.edit
import java.util.Locale

/**
 * Helper class to save and load app preferences using SharedPreferences.
 */
class AppPreferences(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CONTRAST_LEVEL = "contrast_level"
        private const val KEY_ANIMATION_SPEED = "animation_speed"
        private const val KEY_OFFLINE_MODE = "offline_mode"
        private const val KEY_DISTANCE_UNIT = "distance_unit"
        private const val KEY_ALLOW_TRANSIT_IN_OFFLINE_MODE = "allow_transit_in_offline_mode"

        private const val KEY_SHOW_ZOOM_FABS = "show_zoom_fabs"

        private const val KEY_CONTINUOUS_LOCATION_TRACKING = "continuous_location_tracking"

        private const val KEY_LAST_ROUTING_MODE = "last_routing_mode"

        private const val KEY_USE_24_HOUR_FORMAT = "use_24_hour_format"

        // API configuration keys
        private const val KEY_PELIAS_BASE_URL = "pelias_base_url"
        private const val KEY_PELIAS_API_KEY = "pelias_api_key"
        private const val KEY_VALHALLA_BASE_URL = "valhalla_base_url"
        private const val KEY_VALHALLA_API_KEY = "valhalla_api_key"

        // Default values
        private const val DEFAULT_PELIAS_BASE_URL = "https://maps.earth/pelias/v1"
        private const val DEFAULT_VALHALLA_BASE_URL = "https://maps.earth/valhalla/route"
        private const val DEFAULT_LAST_ROUTING_MODE = "auto"

        // Contrast level constants
        const val CONTRAST_LEVEL_STANDARD = 0
        const val CONTRAST_LEVEL_MEDIUM = 1
        const val CONTRAST_LEVEL_HIGH = 2

        // Animation speed constants
        const val ANIMATION_SPEED_SLOW = 0
        const val ANIMATION_SPEED_NORMAL = 1
        const val ANIMATION_SPEED_FAST = 2

        // Offline mode constants
        const val OFFLINE_MODE_DISABLED = false
        const val OFFLINE_MODE_ENABLED = true

        // Distance unit constants
        const val DISTANCE_UNIT_METRIC = 0
        const val DISTANCE_UNIT_IMPERIAL = 1
    }

    /**
     * Saves the contrast level preference.
     */
    fun saveContrastLevel(contrastLevel: Int) {
        prefs.edit {
            putInt(KEY_CONTRAST_LEVEL, contrastLevel)
        }
    }

    /**
     * Loads the saved contrast level preference.
     * Returns CONTRAST_LEVEL_HIGH as default to maintain current behavior.
     */
    fun loadContrastLevel(): Int {
        return prefs.getInt(KEY_CONTRAST_LEVEL, CONTRAST_LEVEL_STANDARD)
    }

    /**
     * Clears the saved contrast level preference.
     */
    fun clearContrastLevel() {
        prefs.edit {
            remove(KEY_CONTRAST_LEVEL)
        }
    }

    /**
     * Saves the animation speed preference.
     */
    fun saveAnimationSpeed(animationSpeed: Int) {
        prefs.edit {
            putInt(KEY_ANIMATION_SPEED, animationSpeed)
        }
    }

    /**
     * Loads the saved animation speed preference.
     * Returns ANIMATION_SPEED_NORMAL as default.
     */
    fun loadAnimationSpeed(): Int {
        return prefs.getInt(KEY_ANIMATION_SPEED, ANIMATION_SPEED_NORMAL)
    }

    /**
     * Clears the saved animation speed preference.
     */
    fun clearAnimationSpeed() {
        prefs.edit {
            remove(KEY_ANIMATION_SPEED)
        }
    }

    /**
     * Saves the offline mode preference.
     */
    fun saveOfflineMode(offlineMode: Boolean) {
        prefs.edit {
            putBoolean(KEY_OFFLINE_MODE, offlineMode)
        }
    }

    /**
     * Loads the saved offline mode preference.
     * Returns OFFLINE_MODE_DISABLED as default.
     */
    fun loadOfflineMode(): Boolean {
        return prefs.getBoolean(KEY_OFFLINE_MODE, OFFLINE_MODE_DISABLED)
    }

    /**
     * Clears the saved offline mode preference.
     */
    fun clearOfflineMode() {
        prefs.edit {
            remove(KEY_OFFLINE_MODE)
        }
    }

    /**
     * Saves the distance unit preference.
     */
    fun saveDistanceUnit(distanceUnit: Int) {
        prefs.edit {
            putInt(KEY_DISTANCE_UNIT, distanceUnit)
        }
    }

    /**
     * Loads the saved distance unit preference.
     * Returns a locale-based default: imperial for US, Liberia, Myanmar; metric for others.
     */
    fun loadDistanceUnit(): Int {
        val defaultUnit = getDefaultDistanceUnitFromLocale()
        return prefs.getInt(KEY_DISTANCE_UNIT, defaultUnit)
    }

    /**
     * Clears the saved distance unit preference.
     */
    fun clearDistanceUnit() {
        prefs.edit {
            remove(KEY_DISTANCE_UNIT)
        }
    }

    /**
     * Saves the allow transit in offline mode preference.
     */
    fun saveAllowTransitInOfflineMode(allowTransitInOfflineMode: Boolean) {
        prefs.edit {
            putBoolean(KEY_ALLOW_TRANSIT_IN_OFFLINE_MODE, allowTransitInOfflineMode)
        }
    }

    /**
     * Loads the saved allow transit in offline mode preference.
     * Returns false as default.
     */
    fun loadAllowTransitInOfflineMode(): Boolean {
        return prefs.getBoolean(KEY_ALLOW_TRANSIT_IN_OFFLINE_MODE, true)
    }

    /**
     * Clears the saved allow transit in offline mode preference.
     */
    fun clearAllowTransitInOfflineMode() {
        prefs.edit {
            remove(KEY_ALLOW_TRANSIT_IN_OFFLINE_MODE)
        }
    }

    /**
     * Saves the show zoom FABs preference.
     */
    fun saveShowZoomFabs(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_SHOW_ZOOM_FABS, enabled)
        }
    }

    /**
     * Loads the saved show zoom FABs preference.
     * Returns true as default.
     */
    fun loadShowZoomFabs(): Boolean {
        return prefs.getBoolean(KEY_SHOW_ZOOM_FABS, true)
    }

    /**
     * Clears the saved show zoom FABs preference.
     */
    fun clearShowZoomFabs() {
        prefs.edit {
            remove(KEY_SHOW_ZOOM_FABS)
        }
    }

    /**
     * Saves the continuous location tracking preference.
     */
    fun saveContinuousLocationTracking(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_CONTINUOUS_LOCATION_TRACKING, enabled)
        }
    }

    /**
     * Loads the saved continuous location tracking preference.
     * Returns true as default (tracking enabled by default).
     */
    fun loadContinuousLocationTracking(): Boolean {
        return prefs.getBoolean(KEY_CONTINUOUS_LOCATION_TRACKING, true)
    }

    /**
     * Saves the last routing mode preference.
     */
    fun saveLastRoutingMode(mode: String) {
        prefs.edit {
            putString(KEY_LAST_ROUTING_MODE, mode)
        }
    }

    /**
     * Loads the saved last routing mode preference.
     * Returns "auto" as default.
     */
    fun loadLastRoutingMode(): String {
        return prefs.getString(KEY_LAST_ROUTING_MODE, DEFAULT_LAST_ROUTING_MODE)
            ?: DEFAULT_LAST_ROUTING_MODE
    }

    /**
     * Saves the use 24-hour format preference.
     */
    fun saveUse24HourFormat(use24Hour: Boolean) {
        prefs.edit {
            putBoolean(KEY_USE_24_HOUR_FORMAT, use24Hour)
        }
    }

    /**
     * Loads the saved use 24-hour format preference.
     * Returns the system default as default value.
     */
    fun loadUse24HourFormat(): Boolean {
        val systemDefault = DateFormat.is24HourFormat(context)
        // Note: we're using the system default as the fallback, but storing user preference
        return prefs.getBoolean(KEY_USE_24_HOUR_FORMAT, systemDefault)
    }

    /**
     * Gets the default distance unit based on the system locale.
     * Returns imperial for countries that use imperial system (US, Liberia, Myanmar),
     * metric for all others.
     */
    private fun getDefaultDistanceUnitFromLocale(): Int {
        val locale = Locale.getDefault()
        val countryCode = locale.country.uppercase()

        // Countries that use imperial system
        return when (countryCode) {
            "US", "LR", "MM" -> DISTANCE_UNIT_IMPERIAL
            else -> DISTANCE_UNIT_METRIC
        }
    }

    // Pelias API configuration methods

    /**
     * Saves the Pelias base URL.
     */
    fun savePeliasBaseUrl(baseUrl: String) {
        prefs.edit {
            putString(KEY_PELIAS_BASE_URL, baseUrl)
        }
    }

    /**
     * Loads the saved Pelias base URL.
     * Returns the default Pelias base URL if none is saved.
     */
    fun loadPeliasBaseUrl(): String {
        return prefs.getString(KEY_PELIAS_BASE_URL, DEFAULT_PELIAS_BASE_URL)
            ?: DEFAULT_PELIAS_BASE_URL
    }

    /**
     * Saves the Pelias API key.
     */
    fun savePeliasApiKey(apiKey: String?) {
        prefs.edit {
            if (apiKey != null) {
                putString(KEY_PELIAS_API_KEY, apiKey)
            } else {
                remove(KEY_PELIAS_API_KEY)
            }
        }
    }

    /**
     * Loads the saved Pelias API key.
     * Returns null if no API key is saved.
     */
    fun loadPeliasApiKey(): String? {
        return prefs.getString(KEY_PELIAS_API_KEY, null)
    }

    // Valhalla API configuration methods

    /**
     * Saves the Valhalla base URL.
     */
    fun saveValhallaBaseUrl(baseUrl: String) {
        prefs.edit {
            putString(KEY_VALHALLA_BASE_URL, baseUrl)
        }
    }

    /**
     * Loads the saved Valhalla base URL.
     * Returns the default Valhalla base URL if none is saved.
     */
    fun loadValhallaBaseUrl(): String {
        return prefs.getString(KEY_VALHALLA_BASE_URL, DEFAULT_VALHALLA_BASE_URL)
            ?: DEFAULT_VALHALLA_BASE_URL
    }

    /**
     * Saves the Valhalla API key.
     */
    fun saveValhallaApiKey(apiKey: String?) {
        prefs.edit {
            if (apiKey != null) {
                putString(KEY_VALHALLA_API_KEY, apiKey)
            } else {
                remove(KEY_VALHALLA_API_KEY)
            }
        }
    }

    /**
     * Loads the saved Valhalla API key.
     * Returns null if no API key is saved.
     */
    fun loadValhallaApiKey(): String? {
        return prefs.getString(KEY_VALHALLA_API_KEY, null)
    }
}
