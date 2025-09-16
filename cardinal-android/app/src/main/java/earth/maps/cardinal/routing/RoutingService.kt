package earth.maps.cardinal.routing

import earth.maps.cardinal.data.LatLng
import earth.maps.cardinal.data.RoutingMode
import kotlinx.coroutines.flow.Flow

interface RoutingService {
    suspend fun getRoute(
        request: String
    ): String
}
