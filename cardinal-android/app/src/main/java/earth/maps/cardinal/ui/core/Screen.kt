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

package earth.maps.cardinal.ui.core

import android.net.Uri
import androidx.navigation.NavController
import com.google.gson.Gson
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.RoutingMode

/**
 * Type-safe navigation destinations with typed parameters.
 */
sealed class Screen(val route: String) {
    companion object {
        const val HOME_SEARCH = "home"
        const val NEARBY_POI = "nearby_poi"
        const val NEARBY_TRANSIT = "nearby_transit"
        const val PLACE_CARD = "place_card?place={place}"
        const val MANAGE_PLACES = "manage_places?listId={listId}&parents={parents}"
        const val OFFLINE_AREAS = "offline_areas"
        const val SETTINGS = "settings"
        const val OFFLINE_SETTINGS = "offline_settings"
        const val ACCESSIBILITY_SETTINGS = "accessibility_settings"
        const val ADVANCED_SETTINGS = "advanced_settings"
        const val ROUTING_PROFILES = "routing_profile_settings"
        const val PROFILE_EDITOR = "edit_routing_profile?profileId={profileId}"
        const val DIRECTIONS = "directions?fromPlace={fromPlace}&toPlace={toPlace}"
        const val TRANSIT_ITINERARY_DETAIL = "transit_itinerary_detail?itinerary={itinerary}"
        const val TURN_BY_TURN = "turn_by_turn?routeId={routeId}&routingMode={routingMode}"
    }

    object HomeSearch : Screen(HOME_SEARCH)

    object NearbyPoi : Screen(NEARBY_POI)

    object NearbyTransit : Screen(NEARBY_TRANSIT)

    data class PlaceCard(val place: Place) : Screen(PLACE_CARD)

    data class ManagePlaces(val listId: String? = null, val parents: List<String> = emptyList()) :
        Screen(MANAGE_PLACES)

    object OfflineAreas : Screen(OFFLINE_AREAS)

    object Settings : Screen(SETTINGS)

    object OfflineSettings : Screen(OFFLINE_SETTINGS)

    object AccessibilitySettings : Screen(ACCESSIBILITY_SETTINGS)

    object AdvancedSettings : Screen(ADVANCED_SETTINGS)

    object RoutingProfiles : Screen(ROUTING_PROFILES)

    data class ProfileEditor(val profileId: String?) :
        Screen(PROFILE_EDITOR)

    data class Directions(val fromPlace: Place?, val toPlace: Place?) :
        Screen(DIRECTIONS)

    data class TransitItineraryDetail(val itinerary: earth.maps.cardinal.transit.Itinerary) :
        Screen(TRANSIT_ITINERARY_DETAIL)

    data class TurnByTurnNavigation(val routeId: String, val routingMode: RoutingMode) :
        Screen(TURN_BY_TURN)
}

/**
 * Navigation utilities for type-safe navigation.
 */
object NavigationUtils {
    private val gson = Gson()

    /**
     * Convert a Screen to a route string that can be used with NavController.
     */
    fun toRoute(screen: Screen): String {
        return when (screen) {
            is Screen.HomeSearch -> screen.route
            is Screen.NearbyPoi -> screen.route
            is Screen.NearbyTransit -> screen.route
            is Screen.PlaceCard -> {
                val placeJson = Uri.encode(gson.toJson(screen.place))
                "place_card?place=$placeJson"
            }

            is Screen.ManagePlaces -> {
                val listId = screen.listId?.let { Uri.encode(it) } ?: ""
                val parents = screen.parents.let { Uri.encode(gson.toJson(it)) } ?: ""
                "manage_places?listId=$listId&parents=$parents"
            }

            is Screen.OfflineAreas -> screen.route
            is Screen.Settings -> screen.route
            is Screen.OfflineSettings -> screen.route
            is Screen.AccessibilitySettings -> screen.route
            is Screen.AdvancedSettings -> screen.route
            is Screen.RoutingProfiles -> screen.route
            is Screen.ProfileEditor -> {
                val profileId = screen.profileId
                "edit_routing_profile?profileId=${
                    Uri.encode(profileId)
                }"
            }

            is Screen.Directions -> {
                val fromPlaceJson = screen.fromPlace?.let { Uri.encode(gson.toJson(it)) } ?: ""
                val toPlaceJson = screen.toPlace?.let { Uri.encode(gson.toJson(it)) } ?: ""
                "directions?fromPlace=$fromPlaceJson&toPlace=$toPlaceJson"
            }

            is Screen.TransitItineraryDetail -> {
                val itineraryJson = Uri.encode(gson.toJson(screen.itinerary))
                "transit_itinerary_detail?itinerary=$itineraryJson"
            }

            is Screen.TurnByTurnNavigation -> "turn_by_turn?routeId=${Uri.encode(screen.routeId)}&routingMode=${screen.routingMode.value}"
        }
    }

    /**
     * Navigate to a screen using NavController.
     */
    fun navigate(
        navController: NavController,
        screen: Screen,
        avoidCycles: Boolean = true,
        popUpToHome: Boolean = false,
    ) {
        navController.navigate(toRoute(screen)) {
            if (popUpToHome) {
                popUpTo(Screen.HOME_SEARCH) {
                    inclusive = screen.route == Screen.HOME_SEARCH
                }
            } else if (avoidCycles) {
                popUpTo(screen.route) {
                    inclusive = true
                }
            }
        }
    }
}
