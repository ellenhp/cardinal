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

package earth.maps.cardinal.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import earth.maps.cardinal.data.Address
import earth.maps.cardinal.data.LatLng
import earth.maps.cardinal.data.Place
import kotlin.math.absoluteValue
import kotlin.random.Random

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
    val country: String? = null,
    val countryCode: String? = null
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
            address = if (houseNumber != null || road != null || city != null || state != null || postcode != null || country != null || countryCode != null) {
                Address(
                    houseNumber = houseNumber,
                    road = road,
                    city = city,
                    state = state,
                    postcode = postcode,
                    country = country,
                    countryCode = countryCode
                )
            } else {
                null
            }
        )
    }

    companion object {
        fun fromPlace(place: Place): PlaceEntity {
            return PlaceEntity(
                id = place.id ?: -Random.nextInt().absoluteValue,
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
                country = place.address?.country,
                countryCode = place.address?.countryCode
            )
        }
    }
}
