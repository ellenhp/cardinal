package earth.maps.cardinal.data

data class Place(
    val id: Int,
    val name: String,
    val type: String,
    val icon: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: Address? = null
)
