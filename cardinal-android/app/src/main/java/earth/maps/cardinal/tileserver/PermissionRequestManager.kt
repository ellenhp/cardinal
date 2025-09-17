package earth.maps.cardinal.tileserver

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionRequestManager @Inject constructor() {

    private val _permissionRequests = MutableSharedFlow<PermissionRequest>()
    val permissionRequests: SharedFlow<PermissionRequest> = _permissionRequests.asSharedFlow()

    private val _permissionResults = MutableSharedFlow<PermissionResult>()
    val permissionResults: SharedFlow<PermissionResult> = _permissionResults.asSharedFlow()

    companion object {
        private const val TAG = "PermissionRequestManager"
    }

    /**
     * Request notification permission for tile download service
     */
    suspend fun requestNotificationPermission() {
        Log.d(TAG, "Requesting notification permission for tile downloads")
        _permissionRequests.emit(PermissionRequest.NotificationPermission)
    }

    /**
     * Notify that permission was granted
     */
    suspend fun onPermissionGranted(request: PermissionRequest) {
        Log.d(TAG, "Permission granted for request: $request")
        _permissionResults.emit(PermissionResult.Granted(request))
    }

    /**
     * Notify that permission was denied
     */
    suspend fun onPermissionDenied(request: PermissionRequest) {
        Log.d(TAG, "Permission denied for request: $request")
        _permissionResults.emit(PermissionResult.Denied(request))
    }

    /**
     * Check if notification permission is granted
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not required for older Android versions
        }
    }
}

/**
 * Represents a permission request
 */
sealed class PermissionRequest {
    data object NotificationPermission : PermissionRequest()
}

/**
 * Represents the result of a permission request
 */
sealed class PermissionResult {
    data class Granted(val request: PermissionRequest) : PermissionResult()
    data class Denied(val request: PermissionRequest) : PermissionResult()
}
