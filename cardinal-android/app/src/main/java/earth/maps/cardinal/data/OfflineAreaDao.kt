package earth.maps.cardinal.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineAreaDao {
    @Query("SELECT * FROM offline_areas")
    fun getAllOfflineAreas(): Flow<List<OfflineArea>>

    @Query("SELECT * FROM offline_areas WHERE id = :id")
    suspend fun getOfflineAreaById(id: String): OfflineArea?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineArea(offlineArea: OfflineArea)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineAreas(offlineAreas: List<OfflineArea>)

    @Update
    suspend fun updateOfflineArea(offlineArea: OfflineArea)

    @Delete
    suspend fun deleteOfflineArea(offlineArea: OfflineArea)

    @Query("DELETE FROM offline_areas")
    suspend fun deleteAllOfflineAreas()
}
