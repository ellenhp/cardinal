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

package earth.maps.cardinal.data.room

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
