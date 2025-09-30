/*
 *     Cardinal Maps
 *     Copyright (C) 2025 Cardinal Maps Authors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package earth.maps.cardinal.data

import androidx.compose.ui.graphics.Color

fun Color.isYellow(): Boolean {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    if (max == min) return false // gray
    val delta = max - min
    val h = when (max) {
        r -> 60 * (g - b) / delta
        g -> 60 * (2 + (b - r) / delta)
        b -> 60 * (4 + (r - g) / delta)
        else -> 0f
    }
    val hue = if (h < 0) h + 360f else h
    return hue in 30f..75f
}

