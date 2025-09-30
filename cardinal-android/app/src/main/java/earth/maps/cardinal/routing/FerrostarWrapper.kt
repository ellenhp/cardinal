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
