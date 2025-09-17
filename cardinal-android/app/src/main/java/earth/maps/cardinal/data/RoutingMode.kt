package earth.maps.cardinal.data

enum class RoutingMode(val value: String, val label: String) {
    AUTO("auto", "Driving"),
    TRUCK("truck", "Truck"),
    MOTOR_SCOOTER("motor_scooter", "Motor Scooter"),
    MOTORCYCLE("motorcycle", "Motorcycle"),
    BICYCLE("bicycle", "Cycling"),
    PEDESTRIAN("pedestrian", "Walking");
}
