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

