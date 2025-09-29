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

package earth.maps.cardinal.tileserver

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import earth.maps.cardinal.MainActivity
import earth.maps.cardinal.R
import earth.maps.cardinal.data.BoundingBox
import earth.maps.cardinal.data.room.DownloadStatus
import earth.maps.cardinal.data.room.DownloadedTileDao
import earth.maps.cardinal.data.room.OfflineAreaDao
import earth.maps.cardinal.data.room.TileType
import earth.maps.cardinal.geocoding.TileProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class TileDownloadForegroundService : Service() {

    @Inject
    lateinit var tileDownloadManager: TileDownloadManager

    @Inject
    lateinit var offlineAreaDao: OfflineAreaDao

    @Inject
    lateinit var downloadedTileDao: DownloadedTileDao

    @Inject
    lateinit var tileProcessor: TileProcessor

    @Inject
    lateinit var permissionRequestManager: PermissionRequestManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var downloadJob: Job? = null

    private val _downloadProgress = MutableStateFlow(DownloadProgress())
    val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val binder = TileDownloadBinder()

    // Debouncing for notification updates
    private var notificationUpdateJob: Job? = null
    private var lastNotificationUpdate = 0L

    companion object {
        private const val TAG = "TileDownloadForegroundService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "tile_download_channel"
        private const val CHANNEL_NAME = "Map Downloads"

        // Debouncing constants
        private const val NOTIFICATION_DEBOUNCE_DELAY_MS = 500L
        private const val NOTIFICATION_MIN_INTERVAL_MS = 1000L

        const val ACTION_START_DOWNLOAD = "START_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "CANCEL_DOWNLOAD"
        const val ACTION_RESUME_DOWNLOADS = "RESUME_DOWNLOADS"
        const val ACTION_PAUSE_DOWNLOAD = "PAUSE_DOWNLOAD"
        const val ACTION_RESUME_DOWNLOAD = "RESUME_DOWNLOAD"
    }

    data class DownloadProgress(
        val areaId: String = "",
        val areaName: String = "",
        val currentStage: DownloadStage? = null,
        val stageProgress: Int = 0,
        val stageTotal: Int = 0,
        val isCompleted: Boolean = false,
        val hasError: Boolean = false
    ) {
        // Unified progress calculation with 3 stages (33.3% each)
        val unifiedProgress: Float get() = calculateUnifiedProgress()

        fun describe(): String {
            return when (currentStage) {
                DownloadStage.BASEMAP -> "Downloaded $stageProgress of $stageTotal map tiles"
                DownloadStage.VALHALLA -> "Downloaded $stageProgress of $stageTotal routing tiles"
                DownloadStage.PROCESSING -> "Processed $stageProgress of $stageTotal map tiles"
                null -> "Unknown download state"
            }
        }

        private fun calculateUnifiedProgress(): Float {
            val stageProgressFraction = stageProgress.toFloat() / stageTotal.toFloat()
            return when (currentStage) {
                DownloadStage.BASEMAP -> stageProgressFraction * 0.6f
                DownloadStage.VALHALLA -> 0.6f + stageProgressFraction * 0.2f
                DownloadStage.PROCESSING -> 0.8f + stageProgressFraction * 0.2f
                null -> 0f
            }
        }
    }

    enum class DownloadStage {
        BASEMAP,
        VALHALLA,
        PROCESSING
    }

    inner class TileDownloadBinder : Binder() {
        fun getService(): TileDownloadForegroundService = this@TileDownloadForegroundService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TileDownloadForegroundService created")
        createNotificationChannel()

        // Reset download state on service creation to prevent stale state issues
        _isDownloading.value = false
        _downloadProgress.value = DownloadProgress(currentStage = DownloadStage.BASEMAP)

        // Listen for permission results
        serviceScope.launch {
            permissionRequestManager.permissionResults.collect { result ->
                when (result) {
                    is PermissionResult.Granted -> {
                        Log.d(TAG, "Permission granted, restarting download queue")
                        processDownloadQueue()
                    }

                    is PermissionResult.Denied -> {
                        Log.d(TAG, "Permission denied, stopping service")
                        stopSelf()
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                serviceScope.launch {
                    processDownloadQueue()
                }
            }

            ACTION_CANCEL_DOWNLOAD -> {
                cancelDownload()
            }

            ACTION_RESUME_DOWNLOADS -> {
                serviceScope.launch {
                    processDownloadQueue()
                }
            }

            ACTION_PAUSE_DOWNLOAD -> {
                pauseDownload()
            }

            ACTION_RESUME_DOWNLOAD -> {
                resumeDownload()
            }
        }

        return START_STICKY // Restart service if killed
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Shows progress of map tile downloads"
            setShowBadge(false)
            enableVibration(false)
            enableLights(false)
            setSound(null, null)
        }

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        Log.d(TAG, "Notification channel created with importance DEFAULT")
    }

    private fun createNotification(progress: DownloadProgress): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_DEEP_LINK_DESTINATION, MainActivity.DEEP_LINK_OFFLINE_AREAS)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, TileDownloadForegroundService::class.java).apply {
            action = ACTION_CANCEL_DOWNLOAD
        }
        val cancelPendingIntent = PendingIntent.getService(
            this, 1, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, TileDownloadForegroundService::class.java).apply {
            action = ACTION_PAUSE_DOWNLOAD
        }
        val pausePendingIntent = PendingIntent.getService(
            this, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resumeIntent = Intent(this, TileDownloadForegroundService::class.java).apply {
            action = ACTION_RESUME_DOWNLOAD
        }
        val resumePendingIntent = PendingIntent.getService(
            this, 3, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isPaused = _isPaused.value
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (isPaused) "Download Paused - ${progress.areaName}" else "Downloading ${progress.areaName}")
            .setContentText(progress.describe()).setSmallIcon(R.drawable.cloud_download_24dp)
            .setContentIntent(pendingIntent).setOngoing(true)

        // Add actions based on current state
        if (isPaused) {
            builder.addAction(
                R.drawable.cloud_download_24dp, "Resume", resumePendingIntent
            )
        } else {
            builder.addAction(
                android.R.drawable.ic_media_pause, "Pause", pausePendingIntent
            )
        }

        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent
        )


        if (progress.unifiedProgress > 0) {
            builder.setProgress(1000, (progress.unifiedProgress * 1000f).toInt(), false)
        } else {
            // Initializing state
            builder.setProgress(0, 0, true) // Indeterminate progress
        }

        return builder.build()
    }

    private fun startDownloadJob(
        areaId: String, areaName: String, boundingBox: BoundingBox, minZoom: Int, maxZoom: Int
    ) {
        // Check if we're already downloading the same area
        if (_isDownloading.value && _downloadProgress.value.areaId == areaId) {
            Log.w(TAG, "Download already in progress for area $areaId, ignoring duplicate request")
            return
        }

        // If we're downloading a different area, abort and, TODO: cascade resumes after each download finishes
        if (_isDownloading.value) {
            Log.d(
                TAG,
                "Deferring download ($areaId)"
            )
            return
        }

        Log.d(TAG, "Starting download for area: $areaName (ID: $areaId)")

        _isDownloading.value = true
        _downloadProgress.value = DownloadProgress(
            areaId = areaId, areaName = areaName, currentStage = DownloadStage.BASEMAP
        )

        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "POST_NOTIFICATIONS permission not granted, requesting permission")
                serviceScope.launch {
                    permissionRequestManager.requestNotificationPermission()
                }
                // Don't start the download yet - wait for permission result
                _isDownloading.value = false
                return
            }
        }

        // Start foreground service with explicit type for Android 14+
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ requires explicit foreground service type
                startForeground(
                    NOTIFICATION_ID,
                    createNotification(_downloadProgress.value),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, createNotification(_downloadProgress.value))
            }
            Log.d(TAG, "Foreground service started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
            return
        }

        downloadJob = serviceScope.launch {
            try {
                // Calculate expected tiles for progress tracking
                val expectedBasemapTiles = tileDownloadManager.calculateTotalTiles(
                    boundingBox, minZoom, maxZoom
                )
                val expectedValhallaTiles = ValhallaTileUtils.tilesForBoundingBox(boundingBox).size

                // Check for existing tiles and resume from where we left off
                val existingBasemapTiles =
                    downloadedTileDao.getDownloadedTileCountForAreaAndType(areaId, TileType.BASEMAP)
                val existingValhallaTiles = downloadedTileDao.getDownloadedTileCountForAreaAndType(
                    areaId, TileType.VALHALLA
                )

                Log.d(
                    TAG,
                    "Resuming download: $existingBasemapTiles/$expectedBasemapTiles basemap, $existingValhallaTiles/$expectedValhallaTiles valhalla tiles"
                )

                // Start the actual download
                tileDownloadManager.downloadTilesInternal(
                    boundingBox, minZoom, maxZoom, areaId, areaName,
                )


            } catch (e: Exception) {
                Log.e(TAG, "Error during download", e)
                // Handle download failure
                handleDownloadCompletion(areaId, false, 0L)
            }
        }
    }

    private suspend fun handleDownloadCompletion(areaId: String, success: Boolean, fileSize: Long) {
        Log.d(TAG, "Download completed for area $areaId: success=$success, fileSize=$fileSize")

        // Update the offline area status
        val area = offlineAreaDao.getOfflineAreaById(areaId)
        if (area != null) {
            val updatedArea = area.copy(
                status = if (success) DownloadStatus.PROCESSING_GEOCODER else DownloadStatus.FAILED,
                fileSize = fileSize
            )
            offlineAreaDao.updateOfflineArea(updatedArea)
        }

        _downloadProgress.value = _downloadProgress.value.copy(
            isCompleted = true, hasError = !success
        )

        _isDownloading.value = false

        // Check if there are more downloads in the queue
        val areas = offlineAreaDao.getAllOfflineAreas().first()
        val remainingAreas = areas.filter {
            it.shouldAutomaticallyResume()
        }

        if (remainingAreas.isNotEmpty()) {
            Log.d(TAG, "More downloads in queue, continuing...")
            // Continue processing the queue
            processDownloadQueue()
        } else {
            Log.d(TAG, "No more downloads in queue, stopping service")
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun updateNotification() {
        try {
            // Check notification permission for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w(
                        TAG, "POST_NOTIFICATIONS permission not granted, cannot update notification"
                    )
                    return
                }
            }

            val notificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val notification = createNotification(_downloadProgress.value)
            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Notification updated successfully: ${notification.extras}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update notification", e)
        }
    }

    /**
     * Update notification with debouncing to prevent excessive updates during rapid progress changes.
     * @param immediate If true, bypasses debouncing for critical state changes
     */
    private fun updateNotificationDebounced(immediate: Boolean = false) {
        if (immediate) {
            // For critical state changes (pause/resume/error/completion), update immediately
            updateNotification()
            lastNotificationUpdate = System.currentTimeMillis()
            return
        }

        // Cancel any pending notification update
        notificationUpdateJob?.cancel()

        val currentTime = System.currentTimeMillis()
        val timeSinceLastUpdate = currentTime - lastNotificationUpdate

        if (timeSinceLastUpdate >= NOTIFICATION_MIN_INTERVAL_MS) {
            // Enough time has passed since last update, update immediately
            updateNotification()
            lastNotificationUpdate = currentTime
        } else {
            // Too soon since last update, schedule a debounced update
            notificationUpdateJob = serviceScope.launch {
                delay(NOTIFICATION_DEBOUNCE_DELAY_MS)
                if (_isDownloading.value) {
                    updateNotification()
                }
                lastNotificationUpdate = System.currentTimeMillis()
            }
        }
    }

    private fun stopDownloadJob() {
        try {
            downloadJob?.cancel()
        } finally {
            downloadJob = null
        }
    }

    fun cancelDownload() {
        Log.d(TAG, "Cancelling download")
        // Capture the value for the closure before we start changing state.
        val areaId = _downloadProgress.value.areaId
        stopDownloadJob()
        _isDownloading.value = false
        serviceScope.launch {
            val area = offlineAreaDao.getOfflineAreaById(areaId)?.copy(
                status = DownloadStatus.FAILED
            )
            area?.let { offlineAreaDao.updateOfflineArea(it) }
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    fun startDownload(boundingBox: BoundingBox, minZoom: Int, maxZoom: Int, areaName: String) {
        val areaId = UUID.randomUUID().toString()
        tileDownloadManager.downloadTiles(boundingBox, minZoom, maxZoom, areaId, areaName)
    }

    /**
     * Update download progress (unified version with processing)
     */
    fun updateProgress(
        areaId: String,
        areaName: String,
        currentStage: DownloadStage?,
        stageProgress: Int,
        stageTotal: Int,
        isCompleted: Boolean,
        hasError: Boolean
    ) {
        _downloadProgress.value = DownloadProgress(
            areaId = areaId,
            areaName = areaName,
            currentStage = currentStage,
            stageProgress = stageProgress,
            stageTotal = stageTotal,
            isCompleted = isCompleted,
            hasError = hasError
        )

        // Update notification if service is running
        if (_isDownloading.value) {
            // Use immediate update for completion/error states, debounced for progress updates
            val isImmediate = isCompleted || hasError
            updateNotificationDebounced(immediate = isImmediate)
        }
    }


    private suspend fun processDownloadQueue() {
        Log.d(TAG, "Processing download queue")

        try {
            val areas = offlineAreaDao.getAllOfflineAreas().first()
            val pendingAreas = areas.filter {
                it.shouldAutomaticallyResume()
            }

            Log.d(TAG, "Found ${pendingAreas.size} areas to process")

            if (pendingAreas.isEmpty()) {
                Log.d(TAG, "No pending downloads, stopping service")
                stopForeground(STOP_FOREGROUND_REMOVE)
                return
            }

            // Process areas one by one
            for (area in pendingAreas) {
                Log.d(TAG, "Processing download for area: ${area.name}")

                // Start the download
                startDownloadJob(
                    area.id, area.name, area.boundingBox(), area.minZoom, area.maxZoom
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing download queue", e)
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private suspend fun resumeIncompleteDownloads() {
        Log.d(TAG, "Checking for incomplete downloads to resume")

        try {
            val areas = offlineAreaDao.getAllOfflineAreas().first()
            val incompleteAreas = areas.filter {
                it.shouldAutomaticallyResume()
            }

            Log.d(TAG, "Found ${incompleteAreas.size} incomplete downloads")

            if (incompleteAreas.isNotEmpty()) {
                // For now, resume the first incomplete download
                // In a more advanced implementation, you could queue multiple downloads
                val areaToResume = incompleteAreas.first()
                Log.d(TAG, "Resuming download for area: ${areaToResume.name}")

                startDownloadJob(
                    areaToResume.id,
                    areaToResume.name,
                    areaToResume.boundingBox(),
                    areaToResume.minZoom,
                    areaToResume.maxZoom
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming incomplete downloads", e)
        }
    }

    fun pauseDownload() {
        Log.d(TAG, "Pausing download")

        stopDownloadJob()
        _isDownloading.value = false
        _isPaused.value = true

        // Update the current offline area to mark it as paused
        serviceScope.launch {
            val currentProgress = _downloadProgress.value
            if (currentProgress.areaId.isNotEmpty()) {
                val area = offlineAreaDao.getOfflineAreaById(currentProgress.areaId)
                if (area != null) {
                    val pausedArea = area.copy(paused = true)
                    offlineAreaDao.updateOfflineArea(pausedArea)
                    Log.d(TAG, "Marked area ${currentProgress.areaId} as paused in database")
                }
            }
        }

        // Update notification to show paused state
        updateNotificationDebounced(immediate = true)
    }

    fun deleteTilesForArea(areaId: String): Boolean {
        return tileDownloadManager.deleteTilesForArea(areaId)
    }

    fun resumeDownload() {
        Log.d(TAG, "Resuming download")
        _isPaused.value = false

        // Update the current offline area to mark it as not paused
        serviceScope.launch {
            val currentProgress = _downloadProgress.value
            if (currentProgress.areaId.isNotEmpty()) {
                val area = offlineAreaDao.getOfflineAreaById(currentProgress.areaId)
                if (area != null) {
                    val resumedArea = area.copy(paused = false)
                    offlineAreaDao.updateOfflineArea(resumedArea)
                    Log.d(TAG, "Marked area ${currentProgress.areaId} as resumed in database")
                }
                processDownloadQueue()
            }
        }

        // Update notification to show resumed state
        updateNotificationDebounced(immediate = true)

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "TileDownloadForegroundService destroyed")
        stopDownloadJob()

        // Cancel any pending notification updates
        notificationUpdateJob?.cancel()
        notificationUpdateJob = null
    }
}
