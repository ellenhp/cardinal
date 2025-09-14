package earth.maps.cardinal.routing

import earth.maps.cardinal.data.AppPreferenceRepository
import earth.maps.cardinal.data.Place
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MultiplexedRoutingService(
    private val appPreferenceRepository: AppPreferenceRepository,
    private val onlineRoutingService: ValhallaRoutingService,
    private val offlineRoutingService: OfflineRoutingService
) : RoutingService {
    
    override suspend fun getRoute(
        origin: Place,
        destination: Place,
        profile: String,
        options: Map<String, Any>
    ): Flow<RouteResult> {
        return if (appPreferenceRepository.offlineMode.first()) {
            offlineRoutingService.getRoute(origin, destination, profile, options)
        } else {
            onlineRoutingService.getRoute(origin, destination, profile, options)
        }
    }
}
