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

package earth.maps.cardinal.ui.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import earth.maps.cardinal.data.Place
import earth.maps.cardinal.data.room.OfflineArea
import earth.maps.cardinal.transit.Itinerary
import kotlinx.coroutines.CoroutineScope
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.camera.rememberCameraState
import uniffi.ferrostar.Route

@Stable
class AppContentState(
    val mapPins: SnapshotStateList<Place>,
    val cameraState: CameraState,
    val coroutineScope: CoroutineScope,
    val density: Density,
    fabHeight: Dp = 0.dp,
    selectedOfflineArea: OfflineArea? = null,
    showToolbar: Boolean = true,
    currentRoute: Route? = null,
    currentTransitItinerary: Itinerary? = null,
    screenHeightDp: Dp = 0.dp,
    screenWidthDp: Dp = 0.dp,
    peekHeight: Dp = 0.dp,
) {
    var fabHeight by mutableStateOf(fabHeight)
    var selectedOfflineArea by mutableStateOf(selectedOfflineArea)
    var showToolbar by mutableStateOf(showToolbar)
    var currentRoute by mutableStateOf(currentRoute)
    var currentTransitItinerary by mutableStateOf(currentTransitItinerary)
    var screenHeightDp by mutableStateOf(screenHeightDp)
    var screenWidthDp by mutableStateOf(screenWidthDp)
    var peekHeight by mutableStateOf(peekHeight)
}

@Composable
fun rememberAppContentState(
    cameraState: CameraState = rememberCameraState(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    density: Density = LocalDensity.current,
): AppContentState {
    val mapPins = remember { mutableStateListOf<Place>() }

    return remember(cameraState, coroutineScope, density) {
        AppContentState(
            mapPins = mapPins,
            cameraState = cameraState,
            coroutineScope = coroutineScope,
            density = density,
        )
    }
}
