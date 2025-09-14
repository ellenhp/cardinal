package earth.maps.cardinal.data

data class Place(
    val id: Int,
    val name: String,
    val type: String,
    val icon: String,
    val latitude: Double,
    val longitude: Double,
    val address: Address? = null,
    val isMyLocation: Boolean = false,
)
