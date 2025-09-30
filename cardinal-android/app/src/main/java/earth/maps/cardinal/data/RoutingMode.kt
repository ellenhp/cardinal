/*
 *     Cardinal Maps
 *     Copyright (C) 2025 Cardinal Maps Authors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package earth.maps.cardinal.data

import earth.maps.cardinal.R.drawable

enum class RoutingMode(val value: String, val label: String, val icon: Int) {
    AUTO("auto", "Driving", drawable.mode_car),
    TRUCK("truck", "Truck", drawable.mode_truck),
    MOTOR_SCOOTER("motor_scooter", "Motor Scooter", drawable.mode_moped),
    MOTORCYCLE("motorcycle", "Motorcycle", drawable.mode_motorcycle),
    BICYCLE("bicycle", "Cycling", drawable.mode_bike),
    PEDESTRIAN("pedestrian", "Walking", drawable.mode_walk),
    PUBLIC_TRANSPORT("transit", "Transit", drawable.ic_bus_railway);
}
