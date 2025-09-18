package earth.maps.cardinal.ui

import android.net.Uri
import androidx.navigation.NavController
import com.google.gson.Gson
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.RoutingMode
import earth.maps.cardinal.routing.RouteResult
import uniffi.ferrostar.Route

class NavigationCoordinator(
    private val mainNavController: NavController,
    private val bottomSheetNavController: NavController? = null
) {

    // Navigation to full-screen experiences
    fun navigateToTurnByTurn(routeResult: RouteResult? = null) {
        val route = if (routeResult != null) {
            // Pass route data as navigation argument if needed in the future
            "turn_by_turn?route=${Uri.encode(Gson().toJson(routeResult))}"
        } else {
            Screen.TurnByTurnNavigation.route
        }
        mainNavController.navigate(route)
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
        bottomSheetNavController?.navigate(route)
    }

    fun navigateToPlaceCard(place: Place) {
        val placeJson = Uri.encode(Gson().toJson(place))
        if (isInPlaceCard()) {
            bottomSheetNavController?.popBackStack()
        }
        bottomSheetNavController?.navigate("place_card?place=$placeJson")
    }

    fun navigateToHome() {
        bottomSheetNavController?.navigate("home") {
            popUpTo("home") { inclusive = true }
        }
    }

    fun navigateToSettings() {
        bottomSheetNavController?.navigate(Screen.Settings.route)
    }

    fun navigateToOfflineAreas() {
        bottomSheetNavController?.navigate(Screen.OfflineAreas.route)
    }

    // Back navigation that knows which controller to use
    fun navigateBack(): Boolean {
        return when {
            // If we're in turn-by-turn, use main controller
            mainNavController.currentDestination?.route == Screen.TurnByTurnNavigation.route -> {
                mainNavController.popBackStack()
            }
            // Otherwise try bottom sheet controller first
            bottomSheetNavController?.popBackStack() == true -> true
            // Fall back to main controller
            else -> mainNavController.popBackStack()
        }
    }

    // Check current location
    fun isInTurnByTurn(): Boolean {
        return mainNavController.currentDestination?.route == Screen.TurnByTurnNavigation.route
    }

    fun isInMainApp(): Boolean {
        return mainNavController.currentDestination?.route == "main"
    }

    fun isInPlaceCard() : Boolean {
        return isInMainApp() && bottomSheetNavController?.currentDestination?.route?.startsWith("place_card") == true
    }

    fun onMapInteraction() {
        if (isInPlaceCard()) {
            bottomSheetNavController?.popBackStack()
        }
    }

    // Get current bottom sheet route
    fun getCurrentBottomSheetRoute(): String? {
        return bottomSheetNavController?.currentDestination?.route
    }
}
