package earth.maps.cardinal.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class OfflineAreaRepository @Inject constructor(
    private val offlineAreaDao: OfflineAreaDao
) {
    fun getAllOfflineAreas(): Flow<List<OfflineArea>> {
        return offlineAreaDao.getAllOfflineAreas()
    }

    suspend fun getOfflineAreaById(id: String): OfflineArea? {
        return offlineAreaDao.getOfflineAreaById(id)
    }

    suspend fun insertOfflineArea(offlineArea: OfflineArea) {
        offlineAreaDao.insertOfflineArea(offlineArea)
    }

    suspend fun updateOfflineArea(offlineArea: OfflineArea) {
        offlineAreaDao.updateOfflineArea(offlineArea)
    }

    suspend fun deleteOfflineArea(offlineArea: OfflineArea) {
        offlineAreaDao.deleteOfflineArea(offlineArea)
    }

    suspend fun deleteAllOfflineAreas() {
        offlineAreaDao.deleteAllOfflineAreas()
    }
}
