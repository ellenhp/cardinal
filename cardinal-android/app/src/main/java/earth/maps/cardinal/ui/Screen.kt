package earth.maps.cardinal.ui

import earth.maps.cardinal.data.OfflineArea

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object PlaceCard : Screen("place_card?place={place}")
    object OfflineAreas : Screen("offline_areas")
    object Settings : Screen("settings")
    
    // Helper function to create route with arguments
    fun createRouteWithArea(area: OfflineArea): String {
        return "offline_areas?area=${area.id}"
    }
}
