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

import uniffi.ferrostar.Route
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A repository for caching routes with UUID keys to avoid passing large JSON objects
 * through navigation arguments.
 */
@Singleton
class RouteRepository @Inject constructor() {
    private val routeCache = ConcurrentHashMap<String, Route>()
    private val routeIds = ConcurrentLinkedQueue<String>()

    /**
     * Stores a route in the cache and returns its unique identifier.
     * Maintains a maximum of MAX_ROUTES routes, removing the oldest when necessary.
     */
    fun storeRoute(route: Route): String {
        val id = UUID.randomUUID().toString()
        routeCache[id] = route
        routeIds.offer(id)

        // If we exceed the maximum number of routes, remove the oldest one
        if (routeIds.size > MAX_ROUTES) {
            val oldestId = routeIds.poll()
            oldestId?.let { routeCache.remove(it) }
        }

        return id
    }

    /**
     * Retrieves a route by its identifier.
     */
    fun getRoute(id: String): Route? = routeCache[id]

    companion object {
        // In theory we only need one, but it's cheap to store two just in case something weird happens.
        // Nothing surprises me anymore w.r.t. concurrency and the android lifecycle.
        private const val MAX_ROUTES = 2
    }

}
