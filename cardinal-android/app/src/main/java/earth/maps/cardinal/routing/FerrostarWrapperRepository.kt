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

package earth.maps.cardinal.routing

import android.content.Context
import com.stadiamaps.ferrostar.core.AndroidTtsObserver
import dagger.hilt.android.qualifiers.ApplicationContext
import earth.maps.cardinal.data.AppPreferenceRepository
import earth.maps.cardinal.data.RoutingMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FerrostarWrapperRepository @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
    private val appPreferences: AppPreferenceRepository,
    private val routingProfileRepository: earth.maps.cardinal.data.RoutingProfileRepository
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
            appPreferences,
            routingProfileRepository
        )
        cycling = FerrostarWrapper(
            context,
            RoutingMode.BICYCLE,
            endpoint,
            androidTtsObserver,
            appPreferences,
            routingProfileRepository
        )
        driving = FerrostarWrapper(
            context,
            RoutingMode.AUTO,
            endpoint,
            androidTtsObserver,
            appPreferences,
            routingProfileRepository
        )
        truck = FerrostarWrapper(
            context,
            RoutingMode.TRUCK,
            endpoint,
            androidTtsObserver,
            appPreferences,
            routingProfileRepository
        )
        motorScooter = FerrostarWrapper(
            context,
            RoutingMode.MOTOR_SCOOTER,
            endpoint,
            androidTtsObserver,
            appPreferences,
            routingProfileRepository
        )
        motorcycle = FerrostarWrapper(
            context,
            RoutingMode.MOTORCYCLE,
            endpoint,
            androidTtsObserver,
            appPreferences,
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
        }
    }
}
