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
import earth.maps.cardinal.data.Place
import java.util.UUID

@Entity(tableName = "saved_places")
data class SavedPlace(
    @PrimaryKey val id: String,  // UUID string
    @Deprecated("Update references to placeId to point to new ID field.") val placeId: Int?,  // Reference to original place ID if from search
    val customName: String? = null,  // User can override name
    val customDescription: String? = null,  // User can add notes
    val name: String,
    val type: String,
    val icon: String,
    val latitude: Double,
    val longitude: Double,
    // Address fields
    val houseNumber: String? = null,
    val road: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postcode: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val isTransitStop: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun fromPlace(place: Place): SavedPlace {
            val timestamp = System.currentTimeMillis()

            return SavedPlace(
                id = UUID.randomUUID().toString(),
                placeId = 0,
                customName = null,
                customDescription = null,
                name = place.name,
                type = if (place.description.isNotEmpty()) place.description else "place",
                icon = place.icon,
                latitude = place.latLng.latitude,
                longitude = place.latLng.longitude,
                houseNumber = place.address?.houseNumber,
                road = place.address?.road,
                city = place.address?.city,
                state = place.address?.state,
                postcode = place.address?.postcode,
                country = place.address?.country,
                countryCode = place.address?.countryCode,
                isTransitStop = place.isTransitStop,
                createdAt = timestamp,
                updatedAt = timestamp
            )
        }
    }
}
