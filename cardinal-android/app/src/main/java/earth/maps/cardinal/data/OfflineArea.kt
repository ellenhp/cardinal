package earth.maps.cardinal.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data class representing an offline area that has been downloaded for offline use.
 */
@Entity(tableName = "offline_areas")
data class OfflineArea(
    @PrimaryKey val id: String,
    val name: String,
    val north: Double,
    val south: Double,
    val east: Double,
    val west: Double,
    val minZoom: Int,
    val maxZoom: Int,
    val downloadDate: Long,
    val fileSize: Long,
    val status: DownloadStatus
)

/**
 * Enum representing the status of an offline area download.
 */
enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED
}
