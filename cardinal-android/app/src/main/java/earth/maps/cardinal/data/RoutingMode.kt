package earth.maps.cardinal.data

enum class RoutingMode(val value: String, val label: String) {
    AUTO("auto", "Driving"),
    PEDESTRIAN("pedestrian", "Walking"),
    BICYCLE("bicycle", "Cycling");
}
