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
import earth.maps.cardinal.viewmodel.TurnByTurnNavigationViewModel
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
    }

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
