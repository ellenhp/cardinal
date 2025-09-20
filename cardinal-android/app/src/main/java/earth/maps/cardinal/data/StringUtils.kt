package earth.maps.cardinal.data

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
