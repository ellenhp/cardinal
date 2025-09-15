package earth.maps.cardinal.routing

data class RouteResult(
    val distance: Double, // in meters or as specified by units
    val duration: Double, // in seconds
    val legs: List<RouteLeg>,
    val geometry: RouteGeometry,
    val units: String = "kilometers" // or "miles"
)

data class RouteLeg(
    val summary: String,
    val distance: Double,
    val duration: Double,
    val steps: List<RouteStep>
)

data class RouteStep(
    val distance: Double,
    val duration: Double,
    val instruction: String,
    val name: String,
    val geometry: RouteGeometry? = null,
    val maneuver: Maneuver
)

data class RouteGeometry(
    val type: String = "LineString",
    val coordinates: List<List<Double>> // [longitude, latitude] pairs
)

data class Maneuver(
    val location: List<Double>, // [longitude, latitude]
    val bearingBefore: Double,
    val bearingAfter: Double,
    val type: String,
    val modifier: String? = null,
    val instruction: String
)
