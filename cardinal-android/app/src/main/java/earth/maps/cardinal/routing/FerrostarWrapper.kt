package earth.maps.cardinal.routing

import android.content.Context
import com.stadiamaps.ferrostar.composeui.notification.DefaultForegroundNotificationBuilder
import com.stadiamaps.ferrostar.core.AndroidSystemLocationProvider
import com.stadiamaps.ferrostar.core.AndroidTtsObserver
import com.stadiamaps.ferrostar.core.FerrostarCore
import com.stadiamaps.ferrostar.core.http.OkHttpClientProvider
import com.stadiamaps.ferrostar.core.service.FerrostarForegroundServiceManager
import com.stadiamaps.ferrostar.core.service.ForegroundServiceManager
import earth.maps.cardinal.data.RoutingMode
import okhttp3.OkHttpClient
import uniffi.ferrostar.CourseFiltering
import uniffi.ferrostar.NavigationControllerConfig
import uniffi.ferrostar.RouteDeviationTracking
import uniffi.ferrostar.WaypointAdvanceMode
import uniffi.ferrostar.stepAdvanceDistanceEntryAndExit
import uniffi.ferrostar.stepAdvanceDistanceToEndOfStep
import java.net.URL

class FerrostarWrapper(
    context: Context,
    mode: RoutingMode,
    localValhallaEndpoint: String,
    androidTtsObserver: AndroidTtsObserver,
) {

    private val foregroundServiceManager: ForegroundServiceManager =
        FerrostarForegroundServiceManager(
            context = context,
            DefaultForegroundNotificationBuilder(context)
        )
    private val locationProvider = AndroidSystemLocationProvider(context = context)

    val core =
        FerrostarCore(
            valhallaEndpointURL = URL(localValhallaEndpoint),
            profile = mode.value,
            httpClient = OkHttpClientProvider(OkHttpClient()),
            locationProvider = locationProvider,
            navigationControllerConfig = NavigationControllerConfig(
                waypointAdvance = WaypointAdvanceMode.WaypointWithinRange(100.0),
                stepAdvanceCondition = stepAdvanceDistanceEntryAndExit(30u, 5u, 32u),
                arrivalStepAdvanceCondition = stepAdvanceDistanceToEndOfStep(30u, 32u),
                routeDeviationTracking = RouteDeviationTracking.StaticThreshold(15U, 50.0),
                snappedLocationCourseFiltering = CourseFiltering.SNAP_TO_ROUTE
            ),
            foregroundServiceManager = foregroundServiceManager,
        )

    init {
        core.spokenInstructionObserver = androidTtsObserver
    }
}
