package earth.maps.cardinal.routing

import android.content.Context
import android.util.Log
import earth.maps.cardinal.data.Place
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class OfflineRoutingService(private val context: Context) : RoutingService {
    override suspend fun getRoute(
        origin: Place,
        destination: Place,
        profile: String,
        options: Map<String, Any>
    ): Flow<RouteResult> = flow {
        // For now, emit an empty result as a placeholder
        // This will be implemented when the offline routing engine is ready
        Log.d(TAG, "Offline routing not yet implemented")
        emit(RouteResult(
            distance = 0.0,
            duration = 0.0,
            legs = emptyList(),
            units = "kilometers"
        ))
    }

    companion object {
        const val TAG = "OfflineRoutingService"
    }
}
