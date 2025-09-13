package earth.maps.cardinal.data

/**
 * Data class for API configuration containing a base URL and optional API key.
 */
data class ApiConfiguration(
    val baseUrl: String,
    val apiKey: String? = null
)
