package earth.maps.cardinal.routing

import earth.maps.cardinal.data.AppPreferenceRepository
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.RoutingMode
import kotlinx.coroutines.flow.Flow

class MultiplexedRoutingService(
    private val appPreferenceRepository: AppPreferenceRepository,
    private val onlineRoutingService: ValhallaRoutingService,
    private val offlineRoutingService: OfflineRoutingService
) : RoutingService {

    override suspend fun getRoute(
        origin: Place,
        destination: Place,
        mode: RoutingMode,
        options: Map<String, Any>
    ): Flow<RouteResult> {
        return if (appPreferenceRepository.offlineMode.value) {
            offlineRoutingService.getRoute(origin, destination, mode, options)
        } else {
            onlineRoutingService.getRoute(origin, destination, mode, options)
        }
    }
}
