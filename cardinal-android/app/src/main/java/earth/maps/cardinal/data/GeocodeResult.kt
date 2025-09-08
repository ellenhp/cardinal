package earth.maps.cardinal.data

data class GeocodeResult(
    val latitude: Double,
    val longitude: Double,
    val displayName: String,
    val address: Address? = null
)

data class Address(
    val houseNumber: String? = null,
    val road: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postcode: String? = null,
    val country: String? = null
)
