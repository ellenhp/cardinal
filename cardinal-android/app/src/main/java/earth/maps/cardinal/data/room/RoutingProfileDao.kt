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

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutingProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: RoutingProfile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<RoutingProfile>)

    @Update
    suspend fun update(profile: RoutingProfile)

    @Delete
    suspend fun delete(profile: RoutingProfile)

    @Query("SELECT * FROM routing_profiles ORDER BY updatedAt DESC")
    fun getAllProfiles(): Flow<List<RoutingProfile>>

    @Query("SELECT * FROM routing_profiles WHERE id = :profileId")
    suspend fun getProfileById(profileId: String): RoutingProfile?

    @Query("SELECT * FROM routing_profiles WHERE routingMode = :routingMode ORDER BY name ASC")
    fun getProfilesByMode(routingMode: String): Flow<List<RoutingProfile>>

    @Query("SELECT * FROM routing_profiles WHERE routingMode = :routingMode AND isDefault = 1 LIMIT 1")
    suspend fun getDefaultProfileForMode(routingMode: String): RoutingProfile?

    @Query("UPDATE routing_profiles SET isDefault = 0 WHERE routingMode = :routingMode")
    suspend fun clearDefaultForMode(routingMode: String)

    @Query("UPDATE routing_profiles SET isDefault = 1 WHERE id = :profileId")
    suspend fun setDefaultProfile(profileId: String)

    @Query("DELETE FROM routing_profiles WHERE id = :profileId")
    suspend fun deleteById(profileId: String)

    @Query("SELECT COUNT(*) FROM routing_profiles WHERE routingMode = :routingMode")
    suspend fun getProfileCountForMode(routingMode: String): Int

    @Transaction
    suspend fun setProfileAsDefault(profileId: String) {
        val profile =
            getProfileById(profileId) ?: throw IllegalArgumentException("Profile not found")
        clearDefaultForMode(profile.routingMode)
        setDefaultProfile(profileId)
    }

    @Transaction
    suspend fun createProfileWithDefault(profile: RoutingProfile): Long {
        if (profile.isDefault) {
            clearDefaultForMode(profile.routingMode)
        }
        return insert(profile)
    }

    @Transaction
    suspend fun updateProfileWithDefault(profile: RoutingProfile) {
        if (profile.isDefault) {
            clearDefaultForMode(profile.routingMode)
        }
        update(profile)
    }
}
