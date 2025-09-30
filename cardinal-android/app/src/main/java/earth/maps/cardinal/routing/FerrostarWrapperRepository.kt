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

package earth.maps.cardinal.routing

import android.content.Context
import com.stadiamaps.ferrostar.core.AndroidTtsObserver
import dagger.hilt.android.qualifiers.ApplicationContext
import earth.maps.cardinal.data.RoutingMode
import earth.maps.cardinal.data.room.RoutingProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FerrostarWrapperRepository @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
    private val routingProfileRepository: RoutingProfileRepository
) {
    lateinit var walking: FerrostarWrapper
    lateinit var cycling: FerrostarWrapper
    lateinit var driving: FerrostarWrapper
    lateinit var truck: FerrostarWrapper
    lateinit var motorScooter: FerrostarWrapper
    lateinit var motorcycle: FerrostarWrapper

    val androidTtsObserver = AndroidTtsObserver(context)

    fun setValhallaEndpoint(endpoint: String) {
        walking = FerrostarWrapper(
            context,
            RoutingMode.PEDESTRIAN,
            endpoint,
            androidTtsObserver,
            routingProfileRepository
        )
        cycling = FerrostarWrapper(
            context,
            RoutingMode.BICYCLE,
            endpoint,
            androidTtsObserver,
            routingProfileRepository
        )
        driving = FerrostarWrapper(
            context,
            RoutingMode.AUTO,
            endpoint,
            androidTtsObserver,
            routingProfileRepository
        )
        truck = FerrostarWrapper(
            context,
            RoutingMode.TRUCK,
            endpoint,
            androidTtsObserver,
            routingProfileRepository
        )
        motorScooter = FerrostarWrapper(
            context,
            RoutingMode.MOTOR_SCOOTER,
            endpoint,
            androidTtsObserver,
            routingProfileRepository
        )
        motorcycle = FerrostarWrapper(
            context,
            RoutingMode.MOTORCYCLE,
            endpoint,
            androidTtsObserver,
            routingProfileRepository
        )
    }

    /**
     * Updates the routing options for the specified mode by modifying the existing wrapper.
     */
    fun setOptionsForMode(mode: RoutingMode, routingOptions: RoutingOptions) {
        when (mode) {
            RoutingMode.PEDESTRIAN -> walking.setOptions(routingOptions)
            RoutingMode.BICYCLE -> cycling.setOptions(routingOptions)
            RoutingMode.AUTO -> driving.setOptions(routingOptions)
            RoutingMode.TRUCK -> truck.setOptions(routingOptions)
            RoutingMode.MOTOR_SCOOTER -> motorScooter.setOptions(routingOptions)
            RoutingMode.MOTORCYCLE -> motorcycle.setOptions(routingOptions)
            else -> {}
        }
    }

    /**
     * Resets the routing options for the specified mode to defaults by recreating the wrapper.
     */
    fun resetOptionsToDefaultsForMode(mode: RoutingMode) {
        val defaultOptions = routingProfileRepository.createDefaultOptionsForMode(mode)
        when (mode) {
            RoutingMode.PEDESTRIAN -> walking.setOptions(defaultOptions)
            RoutingMode.BICYCLE -> cycling.setOptions(defaultOptions)
            RoutingMode.AUTO -> driving.setOptions(defaultOptions)
            RoutingMode.TRUCK -> truck.setOptions(defaultOptions)
            RoutingMode.MOTOR_SCOOTER -> motorScooter.setOptions(defaultOptions)
            RoutingMode.MOTORCYCLE -> motorcycle.setOptions(defaultOptions)
            else -> {}
        }
    }
}
