package earth.maps.cardinal.routing

import earth.maps.cardinal.data.AppPreferenceRepository
import earth.maps.cardinal.data.LatLng
import earth.maps.cardinal.data.RoutingMode
import javax.inject.Inject

class MultiplexedRoutingService
@Inject constructor(
    private val appPreferenceRepository: AppPreferenceRepository,
    private val onlineRoutingService: ValhallaRoutingService,
    private val offlineRoutingService: OfflineRoutingService
) : RoutingService {

    override suspend fun getRoute(
        origin: LatLng,
        destination: LatLng,
        mode: RoutingMode,
        options: Map<String, Any>
    ): RouteResult {
        return if (appPreferenceRepository.offlineMode.value) {
            offlineRoutingService.getRoute(origin, destination, mode, options)
        } else {
            onlineRoutingService.getRoute(origin, destination, mode, options)
        }
    }
}
