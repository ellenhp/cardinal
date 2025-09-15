package earth.maps.cardinal.routing

import ValhallaConfigBuilder
import android.content.Context
import com.valhalla.api.models.CostingModel
import com.valhalla.api.models.RouteRequest
import com.valhalla.api.models.RouteResponse
import com.valhalla.api.models.RoutingWaypoint
import com.valhalla.valhalla.Valhalla
import com.valhalla.valhalla.ValhallaResponse
import earth.maps.cardinal.data.LatLng
import earth.maps.cardinal.data.RoutingMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.maplibre.geojson.utils.PolylineUtils

class OfflineRoutingService(context: Context) : RoutingService {

    private val config =
        ValhallaConfigBuilder().withTileDir("${context.filesDir}/valhalla_tiles").build()
    private val valhalla = Valhalla(context, config)

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override suspend fun getRoute(
        origin: LatLng,
        destination: LatLng,
        mode: RoutingMode,
        options: Map<String, Any>
    ): RouteResult {
        try {
            val valhallaResponse = coroutineScope.async {
                valhalla.route(
                    RouteRequest(
                        locations = listOf(
                            RoutingWaypoint(lat = origin.latitude, lon = origin.longitude),
                            RoutingWaypoint(lat = destination.latitude, lon = destination.longitude)
                        ),
                        costing = when (mode) {
                            RoutingMode.AUTO -> CostingModel.auto
                            RoutingMode.PEDESTRIAN -> CostingModel.pedestrian
                            RoutingMode.BICYCLE -> CostingModel.bicycle
                        },
                    )
                )
            }.await() as ValhallaResponse.Json
            // Parse the response using generated data classes
            val routeResult = parseRouteResponse(valhallaResponse.jsonResponse)
            return routeResult
        } catch (e: Exception) {
            // TODO: Deal with exceptions somehow.
            throw e
        }
    }

    private fun parseRouteResponse(response: RouteResponse): RouteResult {
        try {
            val trip = response.trip

            val distance = trip.summary.length
            val duration = trip.summary.time
            val units = trip.units.value



            // Parse geometry from the first leg if available
            val geometry = RouteGeometry(
                type = "LineString",
                coordinates = if (trip.legs.isNotEmpty()) {
                    // Decode the polyline using precision 6 (Valhalla uses 6-digit precision)
                    val decodedPoints = PolylineUtils.decode(trip.legs[0].shape, 6)
                    decodedPoints.map { point ->
                        listOf(point.longitude(), point.latitude())
                    }
                } else {
                    emptyList()
                }
            )

            val legs = parseLegs(trip.legs, routeGeometry = geometry)

            return RouteResult(
                distance = distance,
                duration = duration,
                legs = legs,
                geometry = geometry,
                units = units
            )
        } catch (e: Exception) {
            throw e
        }
    }

    private fun parseLegs(legs: List<com.valhalla.api.models.RouteLeg>, routeGeometry: RouteGeometry): List<RouteLeg> {
        return legs.map { leg ->
            RouteLeg(
                summary = "Route leg",
                distance = leg.summary.length,
                duration = leg.summary.time,
                steps = parseSteps(leg.maneuvers, routeGeometry)
            )
        }
    }

    private fun parseSteps(
        maneuvers: List<com.valhalla.api.models.RouteManeuver>,
        routeGeometry: RouteGeometry
    ): List<RouteStep> {
        return maneuvers.map { maneuver ->
            val stepGeometry =
                routeGeometry.coordinates.subList(maneuver.beginShapeIndex, maneuver.endShapeIndex)
            RouteStep(
                distance = maneuver.length,
                duration = maneuver.time,
                instruction = maneuver.instruction,
                name = maneuver.streetNames?.firstOrNull() ?: "",
                geometry = RouteGeometry(coordinates = stepGeometry),
            )
        }
    }

    companion object {
        const val TAG = "OfflineRoutingService"
    }
}
