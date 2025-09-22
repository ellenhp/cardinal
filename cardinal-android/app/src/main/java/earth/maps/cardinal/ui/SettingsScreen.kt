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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.drawable
import earth.maps.cardinal.R.string
import earth.maps.cardinal.viewmodel.SettingsViewModel


@Composable
fun <T> PreferenceOption(
    selectedValue: T, options: List<Pair<T, Int>>, onOptionSelected: (T) -> Unit
) {
    Column {
        options.forEach { (value, labelResId) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onOptionSelected(value)
                    }
                    .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedValue == value, onClick = {
                        onOptionSelected(value)
                    })
                Text(
                    text = stringResource(labelResId),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimensionResource(dimen.padding_minor))
    ) {
        // Version and call to action.
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(dimen.padding)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(string.app_name_long),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    modifier = Modifier.padding(start = dimensionResource(dimen.padding)),
                    text = viewModel.getVersionName() ?: "v?.?.?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic
                )
                Spacer(modifier = Modifier.fillMaxWidth())

                IconButton(onClick = {
                    navController.popBackStack()
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(string.close)
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(dimen.padding)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(string.call_to_action),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(
                            0xAA,
                            0x11,
                            0x11
                        )
                    ),
                    onClick = {
                        viewModel.onCallToActionClicked()
                    }) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        tint = Color.White,
                        contentDescription = stringResource(string.open_cardinal_maps_github_repository_in_browser)
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Routing Profiles Settings Item
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { NavigationUtils.navigate(navController, Screen.RoutingProfiles) }
                .padding(
                    horizontal = dimensionResource(dimen.padding),
                    vertical = dimensionResource(dimen.padding_minor)
                )) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(string.routing_profiles),
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(
                    painter = painterResource(drawable.commute_icon), contentDescription = null
                )
            }
            Text(
                text = stringResource(string.create_and_manage_custom_routing_profiles),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Privacy Settings Item
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { NavigationUtils.navigate(navController, Screen.OfflineSettings) }
                .padding(
                    horizontal = dimensionResource(dimen.padding),
                    vertical = dimensionResource(dimen.padding_minor)
                )) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(string.privacy_settings_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(
                    painter = painterResource(drawable.ic_offline), contentDescription = null
                )
            }
            Text(
                text = stringResource(string.privacy_settings_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Accessibility Settings Item
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { NavigationUtils.navigate(navController, Screen.AccessibilitySettings) }
                .padding(
                    horizontal = dimensionResource(dimen.padding),
                    vertical = dimensionResource(dimen.padding_minor)
                )) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(string.accessibility_settings_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(
                    painter = painterResource(drawable.ic_accessiblity_settings),
                    contentDescription = null
                )
            }
            Text(
                text = stringResource(string.accessibility_settings_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Advanced Settings Item
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { NavigationUtils.navigate(navController, Screen.AdvancedSettings) }
                .padding(
                    horizontal = dimensionResource(dimen.padding),
                    vertical = dimensionResource(dimen.padding_minor)
                )) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(string.advanced_settings_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(
                    painter = painterResource(drawable.ic_settings), contentDescription = null
                )
            }
            Text(
                text = stringResource(string.advanced_settings_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Add some bottom padding to ensure proper spacing
        Box(modifier = Modifier.padding(bottom = 8.dp))
    }
}
