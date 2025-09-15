package earth.maps.cardinal.ui

import earth.maps.cardinal.data.OfflineArea

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object PlaceCard : Screen("place_card?place={place}")
    object OfflineAreas : Screen("offline_areas")
    object Settings : Screen("settings")
    object Directions : Screen("directions?fromPlace={fromPlace}&toPlace={toPlace}")
    object TurnByTurnNavigation : Screen("turn_by_turn")
}
