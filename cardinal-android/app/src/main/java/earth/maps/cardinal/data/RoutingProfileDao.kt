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

package earth.maps.cardinal.data

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
