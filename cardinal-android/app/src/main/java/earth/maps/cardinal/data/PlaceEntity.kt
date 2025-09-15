package earth.maps.cardinal.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val type: String,
    val icon: String,
    val latitude: Double,
    val longitude: Double,
    val houseNumber: String? = null,
    val road: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postcode: String? = null,
    val country: String? = null
) {
    fun toPlace(): Place {
        return Place(
            id = id,
            name = name,
            type = type,
            icon = icon,
            latLng = LatLng(
                latitude = latitude,
                longitude = longitude,
            ),
            address = if (houseNumber != null || road != null || city != null || state != null || postcode != null || country != null) {
                Address(
                    houseNumber = houseNumber,
                    road = road,
                    city = city,
                    state = state,
                    postcode = postcode,
                    country = country
                )
            } else {
                null
            }
        )
    }

    companion object {
        fun fromPlace(place: Place): PlaceEntity {
            return PlaceEntity(
                id = place.id,
                name = place.name,
                type = place.type,
                icon = place.icon,
                latitude = place.latLng.latitude,
                longitude = place.latLng.longitude,
                houseNumber = place.address?.houseNumber,
                road = place.address?.road,
                city = place.address?.city,
                state = place.address?.state,
                postcode = place.address?.postcode,
                country = place.address?.country
            )
        }
    }
}
