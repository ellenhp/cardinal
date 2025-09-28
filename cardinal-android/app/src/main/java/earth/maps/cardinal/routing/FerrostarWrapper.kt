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
import com.stadiamaps.ferrostar.composeui.notification.DefaultForegroundNotificationBuilder
import com.stadiamaps.ferrostar.core.AndroidSystemLocationProvider
import com.stadiamaps.ferrostar.core.AndroidTtsObserver
import com.stadiamaps.ferrostar.core.FerrostarCore
import com.stadiamaps.ferrostar.core.http.OkHttpClientProvider
import com.stadiamaps.ferrostar.core.service.FerrostarForegroundServiceManager
import com.stadiamaps.ferrostar.core.service.ForegroundServiceManager
import earth.maps.cardinal.data.RoutingMode
import earth.maps.cardinal.data.room.RoutingProfileRepository
import okhttp3.OkHttpClient
import uniffi.ferrostar.CourseFiltering
import uniffi.ferrostar.NavigationControllerConfig
import uniffi.ferrostar.RouteAdapter
import uniffi.ferrostar.RouteDeviationTracking
import uniffi.ferrostar.WaypointAdvanceMode
import uniffi.ferrostar.stepAdvanceDistanceEntryAndExit
import uniffi.ferrostar.stepAdvanceDistanceToEndOfStep

class FerrostarWrapper(
    context: Context,
    private val mode: RoutingMode,
    private val localValhallaEndpoint: String,
    private val androidTtsObserver: AndroidTtsObserver,
    routingProfileRepository: RoutingProfileRepository,
    routingOptions: RoutingOptions? = null
) {

    private val foregroundServiceManager: ForegroundServiceManager =
        FerrostarForegroundServiceManager(
            context = context,
            DefaultForegroundNotificationBuilder(context)
        )
    private val locationProvider = AndroidSystemLocationProvider(context = context)
    private var previousRouteOptions: RoutingOptions? = null

    var core =
        FerrostarCore(
            routeAdapter = RouteAdapter.newValhallaHttp(
                endpointUrl = localValhallaEndpoint,
                profile = mode.value,
                optionsJson = routingOptions?.toValhallaOptionsJson()
                    ?: routingProfileRepository.createDefaultOptionsForMode(mode)
                        ?.toValhallaOptionsJson()
            ),
            httpClient = OkHttpClientProvider(OkHttpClient()),
            locationProvider = locationProvider,
            navigationControllerConfig = NavigationControllerConfig(
                waypointAdvance = WaypointAdvanceMode.WaypointWithinRange(100.0),
                stepAdvanceCondition = stepAdvanceDistanceEntryAndExit(30u, 5u, 32u),
                arrivalStepAdvanceCondition = stepAdvanceDistanceToEndOfStep(30u, 32u),
                routeDeviationTracking = RouteDeviationTracking.StaticThreshold(15U, 50.0),
                snappedLocationCourseFiltering = CourseFiltering.SNAP_TO_ROUTE
            ),
            foregroundServiceManager = foregroundServiceManager,
        )

    init {
        core.spokenInstructionObserver = androidTtsObserver
    }

    /**
     * Updates the routing options by recreating the core with new options.
     * This allows changing routing behavior without creating a new wrapper instance.
     */
    fun setOptions(
        newRoutingOptions: RoutingOptions? = null,
    ) {
        val routingOptions = newRoutingOptions ?: previousRouteOptions
        previousRouteOptions = routingOptions
        core = FerrostarCore(
            routeAdapter = RouteAdapter.newValhallaHttp(
                endpointUrl = localValhallaEndpoint,
                profile = mode.value,
                optionsJson = previousRouteOptions?.toValhallaOptionsJson()
            ),
            httpClient = OkHttpClientProvider(OkHttpClient()),
            locationProvider = locationProvider,
            navigationControllerConfig = NavigationControllerConfig(
                waypointAdvance = WaypointAdvanceMode.WaypointWithinRange(100.0),
                stepAdvanceCondition = stepAdvanceDistanceEntryAndExit(30u, 5u, 32u),
                arrivalStepAdvanceCondition = stepAdvanceDistanceToEndOfStep(30u, 32u),
                routeDeviationTracking = RouteDeviationTracking.StaticThreshold(15U, 50.0),
                snappedLocationCourseFiltering = CourseFiltering.SNAP_TO_ROUTE
            ),
            foregroundServiceManager = foregroundServiceManager
        )
        core.spokenInstructionObserver = androidTtsObserver
    }
}
