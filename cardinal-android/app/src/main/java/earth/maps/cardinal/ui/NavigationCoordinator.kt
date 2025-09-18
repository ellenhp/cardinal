package earth.maps.cardinal.ui

import android.net.Uri
import androidx.navigation.NavController
import com.google.gson.Gson
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.RoutingMode
import uniffi.ferrostar.Route

class NavigationCoordinator(
    private val mainNavController: NavController,
    private val bottomSheetNavController: NavController? = null
) {

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
        bottomSheetNavController?.navigate("place_card?place=$placeJson") {
            popUpTo("place_card") { inclusive = true }
        }
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
