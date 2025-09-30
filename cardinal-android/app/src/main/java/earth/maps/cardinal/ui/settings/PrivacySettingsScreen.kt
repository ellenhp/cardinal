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

package earth.maps.cardinal.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.drawable
import earth.maps.cardinal.R.string

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel<SettingsViewModel>(),
    onDismiss: () -> Unit,
    onNavigateToOfflineAreas: () -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(remember { SnackbarHostState() }) },
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(title = {
                Text(
                    text = stringResource(string.privacy_settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            })
        },
        content = { padding ->
            Box(modifier = Modifier.padding(padding)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Offline Areas Settings Item
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToOfflineAreas() }
                            .padding(
                                horizontal = dimensionResource(dimen.padding),
                                vertical = dimensionResource(dimen.padding_minor)
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(string.offline_areas_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Icon(
                                painter = painterResource(drawable.cloud_download_24dp),
                                contentDescription = null
                            )
                        }
                        Text(
                            text = stringResource(string.offline_areas_help_text),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = DividerDefaults.Thickness,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Offline Mode Settings Item
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = dimensionResource(dimen.padding),
                                vertical = dimensionResource(dimen.padding_minor)
                            )
                    ) {
                        Text(
                            text = stringResource(string.offline_mode_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(string.offline_mode_help_text),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Offline mode toggle
                        val currentOfflineMode by viewModel.offlineMode.collectAsState()
                        var isOfflineModeEnabled by remember { mutableStateOf(currentOfflineMode) }

                        // Update selected state when preference changes from outside
                        LaunchedEffect(currentOfflineMode) {
                            isOfflineModeEnabled = currentOfflineMode
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isOfflineModeEnabled) stringResource(string.enabled) else stringResource(
                                    string.disabled
                                ), style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = isOfflineModeEnabled, onCheckedChange = { newValue ->
                                    isOfflineModeEnabled = newValue
                                    viewModel.setOfflineMode(newValue)
                                })
                        }

                        // Allow transit in offline mode toggle
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text(
                                text = stringResource(string.allow_transit_in_offline_mode_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(string.allow_transit_in_offline_mode_help_text),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val currentAllowTransitInOfflineMode by viewModel.allowTransitInOfflineMode.collectAsState()
                            var isAllowTransitInOfflineModeEnabled by remember {
                                mutableStateOf(
                                    currentAllowTransitInOfflineMode
                                )
                            }

                            // Update selected state when preference changes from outside
                            LaunchedEffect(currentAllowTransitInOfflineMode) {
                                isAllowTransitInOfflineModeEnabled =
                                    currentAllowTransitInOfflineMode
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isAllowTransitInOfflineModeEnabled) stringResource(
                                        string.enabled
                                    ) else stringResource(
                                        string.disabled
                                    ), style = MaterialTheme.typography.bodyMedium
                                )
                                Switch(
                                    checked = isAllowTransitInOfflineModeEnabled,
                                    onCheckedChange = { newValue ->
                                        isAllowTransitInOfflineModeEnabled = newValue
                                        viewModel.setAllowTransitInOfflineMode(newValue)
                                    })
                            }
                        }
                    }
                }

            }
        }
    )
}
