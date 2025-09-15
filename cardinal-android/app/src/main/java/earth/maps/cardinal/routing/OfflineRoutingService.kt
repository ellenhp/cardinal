package earth.maps.cardinal.routing

import ValhallaConfigBuilder
import android.content.Context
import com.valhalla.api.models.CostingModel
import com.valhalla.api.models.RouteRequest
import com.valhalla.api.models.RouteResponse
import com.valhalla.api.models.RoutingWaypoint
import com.valhalla.valhalla.Valhalla
import com.valhalla.valhalla.ValhallaResponse
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.RoutingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class OfflineRoutingService(private val context: Context) : RoutingService {

    private val config =
        ValhallaConfigBuilder().withTileDir("${context.filesDir}/valhalla_tiles").build()
    private val valhalla = Valhalla(context, config)
    
    override suspend fun getRoute(
        origin: Place,
        destination: Place,
        mode: RoutingMode,
        options: Map<String, Any>
    ): Flow<RouteResult> = flow {
        try {
            val valhallaResponse = valhalla.route(
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
            ) as ValhallaResponse.Json

            // Parse the response using generated data classes
            val routeResult = parseRouteResponse(valhallaResponse.jsonResponse)
            emit(routeResult)
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
            
            val legs = parseLegs(trip.legs)
            
            // Parse geometry from the first leg if available
            val geometry = if (legs.isNotEmpty()) {
                // TODO: Decode the polyline.
                RouteGeometry(
                    type = "LineString",
                    coordinates = emptyList()
                )
            } else {
                null
            }
            
            return RouteResult(
                distance = distance,
                duration = duration,
                legs = legs,
                geometry = geometry,
                units = units
            )
        } catch (e: Exception) {
            // Return a default RouteResult if parsing fails
            return RouteResult(
                distance = 0.0,
                duration = 0.0,
                legs = emptyList(),
                geometry = null,
                units = "kilometers"
            )
        }
    }
    
    private fun parseLegs(legs: List<com.valhalla.api.models.RouteLeg>): List<RouteLeg> {
        return legs.map { leg ->
            RouteLeg(
                summary = "Route leg",
                distance = leg.summary.length,
                duration = leg.summary.time,
                steps = parseSteps(leg.maneuvers)
            )
        }
    }
    
    private fun parseSteps(maneuvers: List<com.valhalla.api.models.RouteManeuver>): List<RouteStep> {
        return maneuvers.map { maneuver ->
            RouteStep(
                distance = maneuver.length,
                duration = maneuver.time,
                instruction = maneuver.instruction,
                name = maneuver.streetNames?.firstOrNull() ?: "",
                geometry = null, // Would need to decode polyline
                maneuver = Maneuver(
                    location = emptyList(), // Would need to extract from maneuver
                    bearingBefore = 0.0,
                    bearingAfter = 0.0,
                    type = maneuver.type.toString(),
                    modifier = null,
                    instruction = maneuver.instruction
                )
            )
        }
    }

    companion object {
        const val TAG = "OfflineRoutingService"
    }
}
