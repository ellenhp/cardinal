package earth.maps.cardinal.routing

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import earth.maps.cardinal.data.RoutingMode
import javax.inject.Inject

class FerrostarWrapperRepository @Inject constructor(
    @ApplicationContext
    context: Context,
    multiplexedRoutingService: MultiplexedRoutingService
) {
    val walking = FerrostarWrapper(context, RoutingMode.PEDESTRIAN, multiplexedRoutingService)
    val cycling = FerrostarWrapper(context, RoutingMode.BICYCLE, multiplexedRoutingService)
    val driving = FerrostarWrapper(context, RoutingMode.AUTO, multiplexedRoutingService)
}