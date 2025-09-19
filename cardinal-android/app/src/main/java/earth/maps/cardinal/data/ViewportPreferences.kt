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
import org.maplibre.compose.camera.CameraPosition

/**
 * Helper class to save and load viewport preferences using SharedPreferences.
 */
class ViewportPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("viewport_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
        private const val KEY_ZOOM = "zoom"
        private const val KEY_BEARING = "bearing"
        private const val KEY_TILT = "tilt"
        private const val KEY_HAS_SAVED_VIEWPORT = "has_saved_viewport"
    }

    /**
     * Saves the current viewport to SharedPreferences.
     */
    fun saveViewport(cameraPosition: CameraPosition) {
        prefs.edit()
            .putBoolean(KEY_HAS_SAVED_VIEWPORT, true)
            .putDouble(KEY_LATITUDE, cameraPosition.target.latitude)
            .putDouble(KEY_LONGITUDE, cameraPosition.target.longitude)
            .putDouble(KEY_ZOOM, cameraPosition.zoom)
            .putDouble(KEY_BEARING, cameraPosition.bearing)
            .putDouble(KEY_TILT, cameraPosition.tilt)
            .apply()
    }

    /**
     * Loads the saved viewport from SharedPreferences.
     * Returns null if no viewport has been saved.
     */
    fun loadViewport(): CameraPosition? {
        if (!prefs.getBoolean(KEY_HAS_SAVED_VIEWPORT, false)) {
            return null
        }

        val latitude = prefs.getDouble(KEY_LATITUDE, 0.0)
        val longitude = prefs.getDouble(KEY_LONGITUDE, 0.0)
        val zoom = prefs.getDouble(KEY_ZOOM, 0.0)
        val bearing = prefs.getDouble(KEY_BEARING, 0.0)
        val tilt = prefs.getDouble(KEY_TILT, 0.0)

        return CameraPosition(
            target = io.github.dellisd.spatialk.geojson.Position(longitude, latitude),
            zoom = zoom,
            bearing = bearing,
            tilt = tilt
        )
    }

    /**
     * Clears the saved viewport data.
     */
    fun clearViewport() {
        prefs.edit()
            .remove(KEY_HAS_SAVED_VIEWPORT)
            .remove(KEY_LATITUDE)
            .remove(KEY_LONGITUDE)
            .remove(KEY_ZOOM)
            .remove(KEY_BEARING)
            .remove(KEY_TILT)
            .apply()
    }

    /**
     * Extension function to handle Double values in SharedPreferences.
     */
    private fun SharedPreferences.Editor.putDouble(
        key: String,
        value: Double
    ): SharedPreferences.Editor {
        return putLong(key, java.lang.Double.doubleToRawLongBits(value))
    }

    /**
     * Extension function to handle Double values in SharedPreferences.
     */
    private fun SharedPreferences.getDouble(key: String, defaultValue: Double): Double {
        return java.lang.Double.longBitsToDouble(
            getLong(
                key,
                java.lang.Double.doubleToRawLongBits(defaultValue)
            )
        )
    }
}
