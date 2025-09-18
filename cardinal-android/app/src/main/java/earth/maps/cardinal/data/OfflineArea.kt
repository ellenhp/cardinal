package earth.maps.cardinal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import earth.maps.cardinal.ui.BoundingBox

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
    val status: DownloadStatus,
    val paused: Boolean = false,
) {
    fun isIncomplete(): Boolean {
        return status != DownloadStatus.COMPLETED && status != DownloadStatus.FAILED
    }

    fun shouldAutomaticallyResume(): Boolean {
        return isIncomplete() && !paused
    }

    fun boundingBox(): BoundingBox {
        return BoundingBox(
            north,
            south,
            east,
            west
        )

    }
}

/**
 * Enum representing the status of an offline area download.
 */
enum class DownloadStatus {
    PENDING,
    DOWNLOADING_BASEMAP,
    DOWNLOADING_VALHALLA,
    PROCESSING_GEOCODER,
    COMPLETED,
    FAILED
}
