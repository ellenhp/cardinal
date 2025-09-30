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

import androidx.room.Entity
import androidx.room.PrimaryKey
import earth.maps.cardinal.data.BoundingBox

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
