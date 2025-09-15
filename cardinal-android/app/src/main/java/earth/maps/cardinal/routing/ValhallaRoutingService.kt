package earth.maps.cardinal.routing

import android.util.Log
import earth.maps.cardinal.data.AppPreferenceRepository
import earth.maps.cardinal.data.LatLng
import earth.maps.cardinal.data.RoutingMode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.maplibre.geojson.utils.PolylineUtils

private const val TAG = "ValhallaRouting"

class ValhallaRoutingService(private val appPreferenceRepository: AppPreferenceRepository) :
    RoutingService {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging)
    }

    override suspend fun getRoute(
        origin: LatLng,
        destination: LatLng,
        mode: RoutingMode,
        options: Map<String, Any>
    ): RouteResult {
        try {
            val requestBody = buildJsonObject {
                put("locations", buildJsonArray {
                    add(buildJsonObject {
                        put("lon", origin.longitude)
                        put("lat", origin.latitude)
                        put("type", "break")
                    })
                    add(buildJsonObject {
                        put("lon", destination.longitude)
                        put("lat", destination.latitude)
                        put("type", "break")
                    })
                })
                put("costing", mode.value)

                // Add units from options or default to kilometers
                val units = options["units"] as? String ?: "kilometers"
                put("units", units)

                // Add costing options if provided
                if (options.isNotEmpty()) {
                    val costingOptions = buildJsonObject {
                        val profileOptions = options.filterKeys { it != "units" }
                        if (profileOptions.isNotEmpty()) {
                            put(mode.value, buildJsonObject {
                                profileOptions.forEach { (key, value) ->
                                    when (value) {
                                        is String -> put(key, value)
                                        is Number -> put(key, value)
                                        is Boolean -> put(key, value)
                                    }
                                }
                            })
                        }
                    }
                    put("costing_options", costingOptions)
                }
            }

            Log.d(TAG, "Request body: $requestBody")

            val config = appPreferenceRepository.valhallaApiConfig.value
            val url = if (config.apiKey != null) {
                "${config.baseUrl}?api_key=${config.apiKey}"
            } else {
                config.baseUrl
            }

            val response = client.post {
                url(url)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val result = response.body<JsonObject>()
            Log.d(TAG, "Response: $result")

            return parseRouteResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error during routing", e)
            throw e
        }
    }

    private fun parseRouteResult(element: JsonObject): RouteResult {
        return try {
            val trip = element["trip"]?.jsonObject!!
            val locations = trip["locations"]?.jsonArray!!
            val legs = trip["legs"]?.jsonArray!!

            val routeLegs = legs.mapNotNull { legElement ->
                parseRouteLeg(legElement.jsonObject)
            }

            val units = trip["units"]?.jsonPrimitive?.content ?: "kilometers"

            // Get total distance and duration from summary
            val summary = trip["summary"]?.jsonObject
            val distance = summary?.get("length")?.jsonPrimitive?.doubleOrNull ?: 0.0
            val duration = summary?.get("time")?.jsonPrimitive?.doubleOrNull ?: 0.0

            // Parse geometry from the trip shape if available
            val geometry = trip["shape"]?.jsonPrimitive?.content?.let { shape ->
                try {
                    // Decode the polyline using precision 6 (Valhalla uses 6-digit precision)
                    val decodedPoints = PolylineUtils.decode(shape, 6)
                    RouteGeometry(
                        type = "LineString",
                        coordinates = decodedPoints.map { point ->
                            listOf(point.longitude(), point.latitude())
                        }
                    )
                } catch (e: Exception) {
                    // If polyline decoding fails, create empty geometry
                    RouteGeometry(
                        type = "LineString",
                        coordinates = emptyList()
                    )
                }
            } ?: RouteGeometry(
                type = "LineString",
                coordinates = emptyList()
            )

            RouteResult(
                distance = distance,
                duration = duration,
                legs = routeLegs,
                units = units,
                geometry = geometry
            )
        } catch (e: Exception) {
            throw e
        }
    }

    private fun parseRouteLeg(legObject: JsonObject): RouteLeg? {
        return try {
            val summary = legObject["summary"]?.jsonObject
            val distance = summary?.get("length")?.jsonPrimitive?.doubleOrNull ?: 0.0
            val duration = summary?.get("time")?.jsonPrimitive?.doubleOrNull ?: 0.0

            val maneuvers = legObject["maneuvers"]?.jsonArray ?: JsonArray(emptyList())
            val steps = maneuvers.mapNotNull { maneuverElement ->
                parseRouteStep(maneuverElement.jsonObject)
            }

            RouteLeg(
                summary = "", // Valhalla doesn't provide a summary field for legs
                distance = distance,
                duration = duration,
                steps = steps
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing route leg", e)
            null
        }
    }

    private fun parseRouteStep(stepObject: JsonObject): RouteStep? {
        return try {
            val distance = stepObject["length"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            val duration = stepObject["time"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            val instruction = stepObject["instruction"]?.jsonPrimitive?.content ?: ""
            val name =
                stepObject["street_names"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.content ?: ""

            val maneuver =
                parseManeuver(stepObject["maneuver"]?.jsonObject ?: JsonObject(emptyMap()))

            RouteStep(
                distance = distance,
                duration = duration,
                instruction = instruction,
                name = name,
                maneuver = maneuver
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing route step", e)
            null
        }
    }

    private fun parseManeuver(maneuverObject: JsonObject): Maneuver {
        val location = maneuverObject["location"]?.jsonArray?.map {
            it.jsonPrimitive.doubleOrNull ?: 0.0
        } ?: listOf(0.0, 0.0)

        val bearingBefore = maneuverObject["begin_shape_index"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        val bearingAfter = maneuverObject["end_shape_index"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        val type = maneuverObject["type"]?.jsonPrimitive?.content ?: ""
        val modifier = maneuverObject["modifier"]?.jsonPrimitive?.content
        val instruction = maneuverObject["instruction"]?.jsonPrimitive?.content ?: ""

        return Maneuver(
            location = location,
            bearingBefore = bearingBefore,
            bearingAfter = bearingAfter,
            type = type,
            modifier = modifier,
            instruction = instruction
        )
    }
}
