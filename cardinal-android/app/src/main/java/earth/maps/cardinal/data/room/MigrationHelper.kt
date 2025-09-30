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

import android.database.Cursor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for migrating data from the old schema to the new schema.
 */
@Singleton
class MigrationHelper @Inject constructor(
    private val database: AppDatabase
) {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * Migrates existing PlaceEntity data to the new SavedPlace schema.
     * This should be called after the database has been upgraded to version 8.
     */
    suspend fun migratePlacesToSavedPlaces(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Check if we have already migrated
            val rootListExists = database.savedListDao().getRootList() != null
            if (rootListExists) {
                // Already migrated
                return@withContext Result.success(Unit)
            }

            // Create the root "Saved Places" list
            val rootListId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()
            val rootList = SavedList(
                id = rootListId,
                name = "Saved Places",
                description = "All your saved places",
                isRoot = true,
                isCollapsed = false,
                createdAt = timestamp,
                updatedAt = timestamp
            )
            database.savedListDao().insertList(rootList)

            // Get all existing places from the old table
            val placeCursor = database.query("SELECT * FROM places", null)

            var position = 0
            placeCursor.use { cursor ->
                while (cursor.moveToNext()) {
                    // Convert PlaceEntity to SavedPlace
                    val savedPlace = cursorToSavedPlace(cursor)

                    // Insert the saved place
                    database.savedPlaceDao().insertPlace(savedPlace)

                    // Add the place to the root list
                    val listItem = ListItem(
                        listId = rootListId,
                        itemId = savedPlace.id,
                        itemType = ItemType.PLACE,
                        position = position,
                        addedAt = timestamp
                    )
                    database.listItemDao().insertItem(listItem)

                    position++
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Converts a Cursor from the old places table to a SavedPlace.
     */
    private fun cursorToSavedPlace(cursor: Cursor): SavedPlace {
        val id = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        return SavedPlace(
            id = id,
            placeId = cursor.getIntOrNull(cursor.getColumnIndex("id")),
            customName = null,
            customDescription = null,
            name = cursor.getString(cursor.getColumnIndex("name")),
            type = cursor.getString(cursor.getColumnIndex("type")),
            icon = cursor.getString(cursor.getColumnIndex("icon")),
            latitude = cursor.getDouble(cursor.getColumnIndex("latitude")),
            longitude = cursor.getDouble(cursor.getColumnIndex("longitude")),
            houseNumber = cursor.getStringOrNull(cursor.getColumnIndex("houseNumber")),
            road = cursor.getStringOrNull(cursor.getColumnIndex("road")),
            city = cursor.getStringOrNull(cursor.getColumnIndex("city")),
            state = cursor.getStringOrNull(cursor.getColumnIndex("state")),
            postcode = cursor.getStringOrNull(cursor.getColumnIndex("postcode")),
            country = cursor.getStringOrNull(cursor.getColumnIndex("country")),
            countryCode = cursor.getStringOrNull(cursor.getColumnIndex("countryCode")),
            isTransitStop = cursor.getInt(cursor.getColumnIndex("isTransitStop")) == 1,
            createdAt = timestamp,
            updatedAt = timestamp
        )
    }

    /**
     * Extension function to get string value from cursor or null.
     */
    private fun Cursor.getStringOrNull(columnIndex: Int): String? {
        return if (isNull(columnIndex)) null else getString(columnIndex)
    }

    /**
     * Extension function to get int value from cursor or null.
     */
    private fun Cursor.getIntOrNull(columnIndex: Int): Int? {
        return if (isNull(columnIndex)) null else getInt(columnIndex)
    }

    /**
     * Extension function to get double value from cursor.
     */
    private fun Cursor.getDouble(columnIndex: Int): Double {
        return getDouble(columnIndex)
    }
}
