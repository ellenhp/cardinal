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

package earth.maps.cardinal.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import earth.maps.cardinal.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel<SettingsViewModel>(),
    onDismiss: () -> Unit,
    onNavigateToOfflineAreas: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimensionResource(dimen.padding))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = dimensionResource(dimen.padding)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(string.privacy_settings_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )

            IconButton(onClick = onDismiss) {
                Icon(
                    painter = painterResource(drawable.ic_close),
                    contentDescription = stringResource(string.close)
                )
            }
        }

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
                    isAllowTransitInOfflineModeEnabled = currentAllowTransitInOfflineMode
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAllowTransitInOfflineModeEnabled) stringResource(string.enabled) else stringResource(
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
