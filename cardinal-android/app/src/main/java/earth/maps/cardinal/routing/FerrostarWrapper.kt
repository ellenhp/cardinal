package earth.maps.cardinal.routing

import android.content.Context
import com.stadiamaps.ferrostar.composeui.notification.DefaultForegroundNotificationBuilder
import com.stadiamaps.ferrostar.core.AndroidSystemLocationProvider
import com.stadiamaps.ferrostar.core.AndroidTtsObserver
import com.stadiamaps.ferrostar.core.FerrostarCore
import com.stadiamaps.ferrostar.core.http.OkHttpClientProvider
import com.stadiamaps.ferrostar.core.service.FerrostarForegroundServiceManager
import com.stadiamaps.ferrostar.core.service.ForegroundServiceManager
import earth.maps.cardinal.data.AppPreferenceRepository
import earth.maps.cardinal.data.RoutingMode
import earth.maps.cardinal.data.RoutingProfileRepository
import okhttp3.OkHttpClient
import uniffi.ferrostar.CourseFiltering
import uniffi.ferrostar.NavigationControllerConfig
import uniffi.ferrostar.RouteAdapter
import uniffi.ferrostar.RouteDeviationTracking
import uniffi.ferrostar.WaypointAdvanceMode
import uniffi.ferrostar.stepAdvanceDistanceEntryAndExit
import uniffi.ferrostar.stepAdvanceDistanceToEndOfStep

class FerrostarWrapper(
    context: Context,
    private val mode: RoutingMode,
    private val localValhallaEndpoint: String,
    private val androidTtsObserver: AndroidTtsObserver,
    appPreferences: AppPreferenceRepository,
    routingProfileRepository: RoutingProfileRepository,
    routingOptions: RoutingOptions? = null
) {

    private val foregroundServiceManager: ForegroundServiceManager =
        FerrostarForegroundServiceManager(
            context = context,
            DefaultForegroundNotificationBuilder(context)
        )
    private val locationProvider = AndroidSystemLocationProvider(context = context)

    var core =
        FerrostarCore(
            routeAdapter = RouteAdapter.newValhallaHttp(
                endpointUrl = localValhallaEndpoint,
                profile = mode.value,
                optionsJson = routingOptions?.toValhallaOptionsJson()
                    ?: routingProfileRepository.createDefaultOptionsForMode(mode).toValhallaOptionsJson()
            ),
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

    /**
     * Updates the routing options by recreating the core with new options.
     * This allows changing routing behavior without creating a new wrapper instance.
     */
    fun setOptions(routingOptions: RoutingOptions) {
        core = FerrostarCore(
            routeAdapter = RouteAdapter.newValhallaHttp(
                endpointUrl = localValhallaEndpoint,
                profile = mode.value,
                optionsJson = routingOptions.toValhallaOptionsJson()
            ),
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
        core.spokenInstructionObserver = androidTtsObserver
    }
}
