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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A repository for caching routes with UUID keys to avoid passing large JSON objects
 * through navigation arguments.
 */
@Singleton
class RouteRepository @Inject constructor() {
    private val routeCache = mutableMapOf<String, Route>()

    /**
     * Stores a route in the cache and returns its unique identifier.
     */
    fun storeRoute(route: Route): String {
        val id = UUID.randomUUID().toString()
        routeCache[id] = route
        return id
    }

    /**
     * Retrieves a route by its identifier.
     */
    fun getRoute(id: String): Route? = routeCache[id]

    /**
     * Removes a route from the cache by its identifier.
     */
    fun removeRoute(id: String) {
        routeCache.remove(id)
    }

    /**
     * Clears all routes from the cache.
     */
    fun clear() {
        routeCache.clear()
    }
}
