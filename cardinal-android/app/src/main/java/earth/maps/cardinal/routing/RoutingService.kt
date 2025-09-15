package earth.maps.cardinal.routing

import earth.maps.cardinal.data.LatLng
import earth.maps.cardinal.data.RoutingMode
import kotlinx.coroutines.flow.Flow

interface RoutingService {
    /**
     * Get directions from one place to another
     * @param origin The starting place
     * @param destination The destination place
     * @param profile The routing profile (e.g., "auto", "pedestrian", "bicycle")
     * @param options Additional routing options
     * @return Route result
     */
    suspend fun getRoute(
        origin: LatLng,
        destination: LatLng,
        mode: RoutingMode,
        options: Map<String, Any> = emptyMap()
    ): RouteResult
}
