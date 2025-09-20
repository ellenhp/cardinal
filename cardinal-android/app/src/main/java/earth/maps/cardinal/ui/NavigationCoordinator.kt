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

package earth.maps.cardinal.ui

import android.net.Uri
import androidx.navigation.NavController
import com.google.gson.Gson
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.RoutingMode
import earth.maps.cardinal.data.room.RoutingProfile
import uniffi.ferrostar.Route

class NavigationCoordinator(
    private val mainNavController: NavController,
    private val bottomSheetNavController: NavController
) {


    fun navigateRaw(route: String) {
        if (route.startsWith("turn_by_turn")) {
            mainNavController.navigate(route)
        } else {
            bottomSheetNavController.navigate(route)
        }
    }

    // Navigation to turn-by-turn with Ferrostar route
    fun navigateToTurnByTurnWithFerrostarRoute(ferrostarRoute: Route, routingMode: RoutingMode) {
        // Serialize the route and routing mode as navigation arguments
        val routeJson = Uri.encode(Gson().toJson(ferrostarRoute))
        val modeJson = Uri.encode(Gson().toJson(routingMode))
        val route = "turn_by_turn?ferrostarRoute=$routeJson&routingMode=$modeJson"
        mainNavController.navigate(route)
    }

    // Navigation within bottom sheet
    fun navigateToDirections(fromPlace: Place? = null, toPlace: Place? = null) {
        val route = when {
            fromPlace != null && toPlace != null -> {
                val fromJson = Uri.encode(Gson().toJson(fromPlace))
                val toJson = Uri.encode(Gson().toJson(toPlace))
                "directions?fromPlace=$fromJson&toPlace=$toJson"
            }

            toPlace != null -> {
                val toJson = Uri.encode(Gson().toJson(toPlace))
                "directions?toPlace=$toJson"
            }

            fromPlace != null -> {
                val fromJson = Uri.encode(Gson().toJson(fromPlace))
                "directions?fromPlace=$fromJson"
            }

            else -> Screen.Directions.route
        }
        bottomSheetNavController.navigate(route)
    }

    fun navigateToPlaceCard(place: Place) {
        val placeJson = Uri.encode(Gson().toJson(place))
        bottomSheetNavController.navigate("place_card?place=$placeJson") {
            popUpTo(Screen.Home.route) { inclusive = false }
        }
    }

    fun navigateToSettings() {
        bottomSheetNavController.navigate(Screen.Settings.route)
    }

    fun navigateToOfflineAreas() {
        bottomSheetNavController.navigate(Screen.OfflineAreas.route) {
            popUpTo(Screen.Home.route) { inclusive = false }
        }
    }

    // Back navigation that knows which controller to use
    fun navigateBack(): Boolean {
        return when {
            // If we're in turn-by-turn, use main controller
            mainNavController.currentDestination?.route == Screen.TurnByTurnNavigation.route -> {
                mainNavController.popBackStack()
            }
            // Otherwise try bottom sheet controller first
            bottomSheetNavController.popBackStack() -> true
            // Fall back to main controller
            else -> {
                mainNavController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route)
                }
                true
            }
        }
    }

    fun isInMainApp(): Boolean {
        return mainNavController.currentDestination?.route == Screen.Home.route
    }

    fun isInPlaceCard(): Boolean {
        return isInMainApp() && bottomSheetNavController.currentDestination?.route?.startsWith("place_card") == true
    }

    fun onMapInteraction() {
        if (isInPlaceCard()) {
            bottomSheetNavController.popBackStack()
        }
    }

    fun navigateToRoutingProfiles() {
        bottomSheetNavController.navigate(Screen.RoutingProfiles.route)
    }

    fun navigateToRoutingProfileEditor(profile: RoutingProfile? = null) {
        val id = profile?.id
        if (id == null) {
            bottomSheetNavController.navigate("profile_editor")

        } else {
            bottomSheetNavController.navigate("profile_editor?profileId=$id")
        }
    }

    fun navigateToPrivacySettings() {
        bottomSheetNavController.navigate(Screen.PrivacySettings.route)
    }

    fun navigateToAccessibilitySettings() {
        bottomSheetNavController.navigate(Screen.AccessibilitySettings.route)
    }

    fun navigateToAdvancedSettings() {
        bottomSheetNavController.navigate(Screen.AdvancedSettings.route)
    }

    fun navigateToTransitStopCard(stop: Place) {
        val stopJson = Uri.encode(Gson().toJson(stop))
        bottomSheetNavController.navigate("transit_card?stop=$stopJson") {
            popUpTo(Screen.Home.route) { inclusive = false }
        }
    }
}
