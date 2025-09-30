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

import androidx.core.graphics.toColorInt
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Utility class for string operations.
 */
object StringUtils {

    /**
     * Calculates the Levenshtein distance between two strings.
     * The Levenshtein distance is defined as the minimum number of single-character edits
     * (insertions, deletions or substitutions) required to change one string into another.
     *
     * @param s1 the first string
     * @param s2 the second string
     * @return the Levenshtein distance between the two strings
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        // Initialize the first row and column
        for (i in 0..s1.length) {
            dp[i][0] = i
        }
        for (j in 0..s2.length) {
            dp[0][j] = j
        }

        // Fill the DP table
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,       // deletion
                    dp[i][j - 1] + 1,       // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[s1.length][s2.length]
    }
}


// Extension functions for formatting
@OptIn(ExperimentalTime::class)
fun String.formatTime(use24HourFormat: Boolean): String {
    // Parse ISO 8601 time and format to readable time
    return try {
        val instant = Instant.parse(this)
        val localDateTime =
            instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        val hour = localDateTime.hour
        val minute = localDateTime.minute.toString().padStart(2, '0')
        if (use24HourFormat) {
            "${hour.toString().padStart(2, '0')}:$minute"
        } else {
            val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            val amPm = if (hour >= 12) "PM" else "AM"
            "$displayHour:$minute $amPm"
        }
    } catch (_: Exception) {
        this // fallback to original string
    }
}

fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    return if (minutes < 60) {
        "$minutes min"
    } else {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        "${hours}hr ${remainingMinutes}min"
    }
}

fun parseRouteColor(colorString: String?): androidx.compose.ui.graphics.Color? {
    if (colorString.isNullOrBlank()) return null
    return try {
        androidx.compose.ui.graphics.Color("#$colorString".toColorInt())
    } catch (_: Exception) {
        null
    }
}
