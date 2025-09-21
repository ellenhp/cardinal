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
