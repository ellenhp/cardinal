package earth.maps.cardinal.data

import earth.maps.cardinal.R.drawable

enum class RoutingMode(val value: String, val label: String, val icon: Int) {
    AUTO("auto", "Driving", drawable.mode_car),
    TRUCK("truck", "Truck", drawable.mode_truck),
    MOTOR_SCOOTER("motor_scooter", "Motor Scooter", drawable.mode_moped),
    MOTORCYCLE("motorcycle", "Motorcycle", drawable.mode_motorcycle),
    BICYCLE("bicycle", "Cycling", drawable.mode_bike),
    PEDESTRIAN("pedestrian", "Walking", drawable.mode_walk);
}
