package earth.maps.cardinal.routing

import android.content.Context
import com.stadiamaps.ferrostar.composeui.notification.DefaultForegroundNotificationBuilder
import com.stadiamaps.ferrostar.core.AndroidSystemLocationProvider
import com.stadiamaps.ferrostar.core.CustomRouteProvider
import com.stadiamaps.ferrostar.core.FerrostarCore
import com.stadiamaps.ferrostar.core.http.OkHttpClientProvider
import com.stadiamaps.ferrostar.core.service.FerrostarForegroundServiceManager
import com.stadiamaps.ferrostar.core.service.ForegroundServiceManager
import earth.maps.cardinal.data.LatLng
import earth.maps.cardinal.data.RoutingMode
import okhttp3.OkHttpClient
import uniffi.ferrostar.BoundingBox
import uniffi.ferrostar.CourseFiltering
import uniffi.ferrostar.GeographicCoordinate
import uniffi.ferrostar.NavigationControllerConfig
import uniffi.ferrostar.Route
import uniffi.ferrostar.RouteDeviationTracking
import uniffi.ferrostar.RouteStep
import uniffi.ferrostar.UserLocation
import uniffi.ferrostar.Waypoint
import uniffi.ferrostar.WaypointAdvanceMode
import uniffi.ferrostar.stepAdvanceDistanceEntryAndExit
import uniffi.ferrostar.stepAdvanceDistanceToEndOfStep

class FerrostarWrapper(
    context: Context,
    private val mode: RoutingMode,
    routingService: RoutingService
) {

    private val foregroundServiceManager: ForegroundServiceManager =
        FerrostarForegroundServiceManager(
            context = context,
            DefaultForegroundNotificationBuilder(context)
        )
    private val locationProvider = AndroidSystemLocationProvider(context = context)

    private val routeProvider = object : CustomRouteProvider {
        override suspend fun getRoutes(
            userLocation: UserLocation,
            waypoints: List<Waypoint>
        ): List<Route> {
            val start = waypoints[0]
            val end = waypoints[1]
            val startLatLng = LatLng(start.coordinate.lat, start.coordinate.lng)
            val endLatLng = LatLng(end.coordinate.lat, end.coordinate.lng)
            val route = routingService.getRoute(startLatLng, endLatLng, mode)
            val geographicCoordinates =
                route.geometry.coordinates.map { GeographicCoordinate(it[1], it[0]) }
            
            // Calculate bounding box from coordinates
            val lats = route.geometry.coordinates.map { it[1] }
            val lngs = route.geometry.coordinates.map { it[0] }
            val bbox = BoundingBox(
                sw = GeographicCoordinate(lats.minOrNull() ?: 0.0, lngs.minOrNull() ?: 0.0),
                ne = GeographicCoordinate(lats.maxOrNull() ?: 0.0, lngs.maxOrNull() ?: 0.0)
            )
            
            val steps = route.legs.flatMap { leg ->
                leg.steps.map { step ->
                    RouteStep(
                        geometry = emptyList(), // Geometry would need to be extracted from step polyline if available
                        distance = step.distance,
                        duration = step.duration,
                        roadName = step.name,
                        exits = emptyList(), // No exit information available in current data structure
                        instruction = step.instruction,
                        visualInstructions = emptyList(), // Would need to be generated from instruction
                        spokenInstructions = emptyList(), // Would need to be generated from instruction
                        annotations = null, // No annotation data available
                        incidents = emptyList(), // No incident data available
                    )
                }
            }
            return listOf(
                Route(
                    geographicCoordinates,
                    bbox = bbox,
                    distance = route.distance,
                    waypoints = waypoints,
                    steps = steps
                )
            )
        }
    }

    val core =
        FerrostarCore(
            customRouteProvider = routeProvider,
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
}
