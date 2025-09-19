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

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object PlaceCard : Screen("place_card?place={place}")
    object OfflineAreas : Screen("offline_areas")
    object Settings : Screen("settings")
    object PrivacySettings : Screen("privacy_settings")
    object AccessibilitySettings : Screen("accessibility_settings")
    object AdvancedSettings : Screen("advanced_settings")
    object RoutingProfiles : Screen("routing_profiles")
    object ProfileEditor : Screen("profile_editor?profileId={profileId}")
    object Directions : Screen("directions?fromPlace={fromPlace}&toPlace={toPlace}")
    object TurnByTurnNavigation : Screen("turn_by_turn")
}
