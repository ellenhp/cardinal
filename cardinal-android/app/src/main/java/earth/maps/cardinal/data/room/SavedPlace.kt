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
    val isPinned: Boolean = false,
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
    val transitStopId: String? = null,
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
                isPinned = false,
                name = place.name,
                type = place.description.ifEmpty { "place" },
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
                transitStopId = place.transitStopId,
                createdAt = timestamp,
                updatedAt = timestamp,
            )
        }
    }
}
