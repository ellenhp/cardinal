package earth.maps.cardinal.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Helper class to save and load contrast preferences using SharedPreferences.
 */
class ContrastPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("contrast_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CONTRAST_LEVEL = "contrast_level"
        
        // Contrast level constants
        const val CONTRAST_LEVEL_STANDARD = 0
        const val CONTRAST_LEVEL_MEDIUM = 1
        const val CONTRAST_LEVEL_HIGH = 2
    }

    /**
     * Saves the contrast level preference.
     */
    fun saveContrastLevel(contrastLevel: Int) {
        prefs.edit()
            .putInt(KEY_CONTRAST_LEVEL, contrastLevel)
            .apply()
    }

    /**
     * Loads the saved contrast level preference.
     * Returns CONTRAST_LEVEL_HIGH as default to maintain current behavior.
     */
    fun loadContrastLevel(): Int {
        return prefs.getInt(KEY_CONTRAST_LEVEL, CONTRAST_LEVEL_HIGH)
    }

    /**
     * Clears the saved contrast level preference.
     */
    fun clearContrastLevel() {
        prefs.edit()
            .remove(KEY_CONTRAST_LEVEL)
            .apply()
    }
}
