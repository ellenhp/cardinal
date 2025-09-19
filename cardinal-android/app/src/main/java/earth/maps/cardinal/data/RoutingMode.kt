/*
 *    Copyright 2025 The Cardinal Authors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

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
