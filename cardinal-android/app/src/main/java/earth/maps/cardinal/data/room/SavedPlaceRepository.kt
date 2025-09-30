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

import earth.maps.cardinal.data.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedPlaceRepository @Inject constructor(
    database: AppDatabase,
    private val savedListRepository: SavedListRepository,
) {
    private val placeDao = database.savedPlaceDao()
    private val listItemDao = database.listItemDao()

    private val _allPlaces = MutableStateFlow<List<SavedPlace>>(emptyList())
    val allPlaces: StateFlow<List<SavedPlace>> = _allPlaces.asStateFlow()

    /**
     * Saves a place.
     */
    suspend fun savePlace(
        place: Place,
        list: SavedList? = null,
        customName: String? = null,
        customDescription: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val savedPlace = SavedPlace.fromPlace(place).copy(
                customName = customName, customDescription = customDescription
            )

            placeDao.insertPlace(savedPlace)

            val list = list ?: savedListRepository.getRootList().getOrNull()

            list?.let { list ->
                savedListRepository.addItemToList(
                    list.id,
                    itemId = savedPlace.id,
                    itemType = ItemType.PLACE
                )
            }

            Result.success(savedPlace.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates a saved place.
     */
    suspend fun updatePlace(
        placeId: String,
        customName: String? = null,
        customDescription: String? = null,
        isPinned: Boolean? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existingPlace = placeDao.getPlace(placeId) ?: return@withContext Result.failure(
                IllegalArgumentException("Place not found")
            )

            val updatedPlace = existingPlace.copy(
                customName = customName ?: existingPlace.customName,
                customDescription = customDescription ?: existingPlace.customDescription,
                isPinned = isPinned ?: existingPlace.isPinned,
                updatedAt = System.currentTimeMillis()
            )

            placeDao.updatePlace(updatedPlace)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a saved place.
     */
    suspend fun deletePlace(placeId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val place = placeDao.getPlace(placeId) ?: return@withContext Result.failure(
                IllegalArgumentException("Place not found")
            )

            listItemDao.orphanItem(placeId, ItemType.PLACE)
            placeDao.deletePlace(place)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets a saved place by ID.
     */
    suspend fun getPlaceById(placeId: String): Result<SavedPlace?> = withContext(Dispatchers.IO) {
        try {
            val place = placeDao.getPlace(placeId)
            Result.success(place)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets all places in a list.
     */
    fun getPlacesInList(listId: String): Flow<List<SavedPlace>> {
        return placeDao.getPlacesInList(listId)
    }

    /**
     * Converts a SavedPlace back to a Place.
     */
    fun toPlace(savedPlace: SavedPlace): Place {
        return Place(
            id = savedPlace.id,
            name = savedPlace.customName ?: savedPlace.name,
            description = savedPlace.customDescription ?: savedPlace.type,
            icon = savedPlace.icon,
            latLng = earth.maps.cardinal.data.LatLng(
                latitude = savedPlace.latitude, longitude = savedPlace.longitude
            ),
            address = if (savedPlace.houseNumber != null || savedPlace.road != null || savedPlace.city != null || savedPlace.state != null || savedPlace.postcode != null || savedPlace.country != null || savedPlace.countryCode != null) {
                earth.maps.cardinal.data.Address(
                    houseNumber = savedPlace.houseNumber,
                    road = savedPlace.road,
                    city = savedPlace.city,
                    state = savedPlace.state,
                    postcode = savedPlace.postcode,
                    country = savedPlace.country,
                    countryCode = savedPlace.countryCode
                )
            } else {
                null
            },
            isTransitStop = savedPlace.isTransitStop,
            transitStopId = savedPlace.transitStopId,
        )
    }
}
