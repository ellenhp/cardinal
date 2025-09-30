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

package earth.maps.cardinal.ui.directions

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.hilt.navigation.compose.hiltViewModel
import com.stadiamaps.ferrostar.core.DefaultNavigationViewModel
import com.stadiamaps.ferrostar.maplibreui.views.DynamicallyOrientingNavigationView
import earth.maps.cardinal.data.RoutingMode
import uniffi.ferrostar.Route

@Composable
fun KeepScreenOn() {
    val currentView = LocalView.current
    DisposableEffect(Unit) {
        currentView.keepScreenOn = true
        onDispose {
            currentView.keepScreenOn = false
        }
    }
}

@Composable
fun TurnByTurnNavigationScreen(
    port: Int,
    mode: RoutingMode,
    route: Route?
) {
    // Inject the ViewModel using Hilt
    val turnByTurnViewModel: TurnByTurnNavigationViewModel = hiltViewModel()
    val ferrostarWrapperRepository = turnByTurnViewModel.ferrostarWrapperRepository

    // Get the appropriate FerrostarWrapper based on routing mode
    val ferrostarWrapper = when (mode) {
        RoutingMode.AUTO -> ferrostarWrapperRepository.driving
        RoutingMode.PEDESTRIAN -> ferrostarWrapperRepository.walking
        RoutingMode.BICYCLE -> ferrostarWrapperRepository.cycling
        RoutingMode.TRUCK -> ferrostarWrapperRepository.truck
        RoutingMode.MOTOR_SCOOTER -> ferrostarWrapperRepository.motorScooter
        RoutingMode.MOTORCYCLE -> ferrostarWrapperRepository.motorcycle
        else -> null
    }
    if (ferrostarWrapper == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Unsupported routing mode",
                style = MaterialTheme.typography.headlineSmall
            )
        }
        return
    } else {
        val ferrostarCore = remember(ferrostarWrapper) {
            ferrostarWrapper.core
        }

        // Start navigation when a route is provided
        LaunchedEffect(route) {
            route?.let {
                ferrostarCore.startNavigation(route = it)
            }
        }

        // TODO: Make this configurable.
        KeepScreenOn()

        // Create and remember the navigation view model
        val viewModel = remember { DefaultNavigationViewModel(ferrostarCore = ferrostarCore) }

        // Determine the style URL based on theme
        val styleVariant = if (isSystemInDarkTheme()) "dark" else "light"
        val styleUrl = "http://127.0.0.1:$port/style_$styleVariant.json"

        // Only display the navigation view if we have a route
        if (route != null) {
            DynamicallyOrientingNavigationView(
                styleUrl = styleUrl, modifier = Modifier, viewModel = viewModel
            )
        } else {
            // Show a placeholder or loading state when no route is available
            Box(
                modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No route available for navigation",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}
