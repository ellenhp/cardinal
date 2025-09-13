package earth.maps.cardinal.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Helper class to save and load app preferences using SharedPreferences.
 */
class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CONTRAST_LEVEL = "contrast_level"
        private const val KEY_ANIMATION_SPEED = "animation_speed"
        
        // Contrast level constants
        const val CONTRAST_LEVEL_STANDARD = 0
        const val CONTRAST_LEVEL_MEDIUM = 1
        const val CONTRAST_LEVEL_HIGH = 2
        
        // Animation speed constants
        const val ANIMATION_SPEED_SLOW = 0
        const val ANIMATION_SPEED_NORMAL = 1
        const val ANIMATION_SPEED_FAST = 2
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
        return prefs.getInt(KEY_CONTRAST_LEVEL, CONTRAST_LEVEL_HIGH)
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
}
