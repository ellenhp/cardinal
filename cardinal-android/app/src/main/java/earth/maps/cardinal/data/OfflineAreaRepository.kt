package earth.maps.cardinal.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class OfflineAreaRepository @Inject constructor(
    private val offlineAreaDao: OfflineAreaDao
) {
    fun getAllOfflineAreas(): Flow<List<OfflineArea>> {
        return offlineAreaDao.getAllOfflineAreas()
    }
    suspend fun deleteOfflineArea(offlineArea: OfflineArea) {
        offlineAreaDao.deleteOfflineArea(offlineArea)
    }
}
