package earth.maps.cardinal.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.Locale

/**
 * Helper class to save and load app preferences using SharedPreferences.
 */
class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CONTRAST_LEVEL = "contrast_level"
        private const val KEY_ANIMATION_SPEED = "animation_speed"
        private const val KEY_OFFLINE_MODE = "offline_mode"
        private const val KEY_DISTANCE_UNIT = "distance_unit"

        // API configuration keys
        private const val KEY_PELIAS_BASE_URL = "pelias_base_url"
        private const val KEY_PELIAS_API_KEY = "pelias_api_key"
        private const val KEY_VALHALLA_BASE_URL = "valhalla_base_url"
        private const val KEY_VALHALLA_API_KEY = "valhalla_api_key"

        // Default values
        private const val DEFAULT_PELIAS_BASE_URL = "https://maps.earth/pelias/v1"
        private const val DEFAULT_VALHALLA_BASE_URL = "https://maps.earth/valhalla/route"

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
