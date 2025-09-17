package earth.maps.cardinal.data

import androidx.room.TypeConverter

/**
 * TypeConverter for DownloadStatus enum to work with Room database
 */
class DownloadStatusConverter {
    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toDownloadStatus(status: String): DownloadStatus {
        return DownloadStatus.valueOf(status)
    }
}
