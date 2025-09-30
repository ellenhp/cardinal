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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.string
import earth.maps.cardinal.ui.core.TOOLBAR_HEIGHT_DP

@Composable
private fun ContinuousLocationTrackingSetting(viewModel: SettingsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(dimen.padding),
                vertical = dimensionResource(dimen.padding_minor)
            )
    ) {
        Text(
            text = stringResource(string.continuous_location_tracking_disabled_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(string.continuous_location_tracking_disabled_help_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val continuousLocationTracking by viewModel.continuousLocationTracking.collectAsState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (continuousLocationTracking) stringResource(string.enabled) else stringResource(
                    string.disabled
                ), style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = continuousLocationTracking,
                onCheckedChange = { newValue ->
                    viewModel.setContinuousLocationTrackingEnabled(newValue)
                }
            )
        }
    }
}

@Composable
private fun ShowZoomFabsSetting(viewModel: SettingsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(dimen.padding),
                vertical = dimensionResource(dimen.padding_minor)
            )
    ) {
        Text(
            text = stringResource(string.show_zoom_fabs_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(string.show_zoom_fabs_help_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val showZoomFabs by viewModel.showZoomFabs.collectAsState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showZoomFabs) stringResource(string.enabled) else stringResource(
                    string.disabled
                ), style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = showZoomFabs,
                onCheckedChange = { newValue ->
                    viewModel.setShowZoomFabsEnabled(newValue)
                }
            )
        }
    }
}

@Composable
private fun TimeFormatSetting(viewModel: SettingsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(dimen.padding),
                vertical = dimensionResource(dimen.padding_minor)
            )
    ) {
        Text(
            text = stringResource(string.time_format_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(string.time_format_help_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val use24HourFormat by viewModel.use24HourFormat.collectAsState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val formatText = if (use24HourFormat) {
                "24\u2011hour"
            } else {
                "12\u2011hour"
            }
            Text(
                text = formatText,
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = use24HourFormat,
                onCheckedChange = { newValue ->
                    viewModel.setUse24HourFormat(newValue)
                }
            )
        }
    }
}

@Composable
private fun DistanceUnitSetting(viewModel: SettingsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(dimen.padding),
                vertical = dimensionResource(dimen.padding_minor)
            )
    ) {

        Text(
            text = stringResource(string.distance_unit_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(string.distance_unit_help_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val distanceUnit by viewModel.distanceUnit.collectAsState()
        val isMetric = distanceUnit == 0

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val unitText = if (isMetric) {
                stringResource(string.metric)
            } else {
                stringResource(string.imperial)
            }
            Text(
                text = unitText,
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = isMetric,
                onCheckedChange = { newValue ->
                    val newUnit = if (newValue) 0 else 1
                    viewModel.setDistanceUnit(newUnit)
                }
            )
        }
    }
}

@Composable
private fun PeliasBaseUrlSetting(viewModel: SettingsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(dimen.padding),
                vertical = dimensionResource(dimen.padding_minor)
            )
    ) {
        Text(
            text = stringResource(string.pelias_base_url_title),
            style = MaterialTheme.typography.titleMedium
        )

        val currentPeliasConfig by viewModel.peliasApiConfig.collectAsState()
        var peliasBaseUrl by remember { mutableStateOf(currentPeliasConfig.baseUrl) }

        // Update state when config changes from outside
        LaunchedEffect(currentPeliasConfig) {
            peliasBaseUrl = currentPeliasConfig.baseUrl
        }

        OutlinedTextField(
            value = peliasBaseUrl,
            onValueChange = { newValue ->
                peliasBaseUrl = newValue
                viewModel.setPeliasBaseUrl(newValue)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
    }
}

@Composable
private fun PeliasApiKeySetting(viewModel: SettingsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(dimen.padding),
                vertical = dimensionResource(dimen.padding_minor)
            )
    ) {
        Text(
            text = stringResource(string.pelias_api_key_title),
            style = MaterialTheme.typography.titleMedium
        )

        val currentPeliasConfig by viewModel.peliasApiConfig.collectAsState()
        var peliasApiKey by remember {
            mutableStateOf(
                currentPeliasConfig.apiKey ?: ""
            )
        }

        // Update state when config changes from outside
        LaunchedEffect(currentPeliasConfig) {
            peliasApiKey = currentPeliasConfig.apiKey ?: ""
        }

        OutlinedTextField(
            value = peliasApiKey,
            onValueChange = { newValue ->
                peliasApiKey = newValue
                viewModel.setPeliasApiKey(if (newValue.isNotEmpty()) newValue else null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
    }
}

@Composable
private fun ValhallaBaseUrlSetting(viewModel: SettingsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(dimen.padding),
                vertical = dimensionResource(dimen.padding_minor)
            )
    ) {
        Text(
            text = stringResource(string.valhalla_base_url_title),
            style = MaterialTheme.typography.titleMedium
        )

        val currentValhallaConfig by viewModel.valhallaApiConfig.collectAsState()
        var valhallaBaseUrl by remember { mutableStateOf(currentValhallaConfig.baseUrl) }

        // Update state when config changes from outside
        LaunchedEffect(currentValhallaConfig) {
            valhallaBaseUrl = currentValhallaConfig.baseUrl
        }

        OutlinedTextField(
            value = valhallaBaseUrl,
            onValueChange = { newValue ->
                valhallaBaseUrl = newValue
                viewModel.setValhallaBaseUrl(newValue)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
    }
}

@Composable
private fun ValhallaApiKeySetting(viewModel: SettingsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(dimen.padding),
                vertical = dimensionResource(dimen.padding_minor)
            )
    ) {
        Text(
            text = stringResource(string.valhalla_api_key_title),
            style = MaterialTheme.typography.titleMedium
        )

        val currentValhallaConfig by viewModel.valhallaApiConfig.collectAsState()
        var valhallaApiKey by remember {
            mutableStateOf(
                currentValhallaConfig.apiKey ?: ""
            )
        }

        // Update state when config changes from outside
        LaunchedEffect(currentValhallaConfig) {
            valhallaApiKey = currentValhallaConfig.apiKey ?: ""
        }

        OutlinedTextField(
            value = valhallaApiKey,
            onValueChange = { newValue ->
                valhallaApiKey = newValue
                viewModel.setValhallaApiKey(if (newValue.isNotEmpty()) newValue else null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel<SettingsViewModel>()
) {
    val snackBarHostState = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(title = {
                Text(
                    text = stringResource(string.advanced_settings_title),
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

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = DividerDefaults.Thickness,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    ContinuousLocationTrackingSetting(viewModel)

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = DividerDefaults.Thickness,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    ShowZoomFabsSetting(viewModel)

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = DividerDefaults.Thickness,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    TimeFormatSetting(viewModel)

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = DividerDefaults.Thickness,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    DistanceUnitSetting(viewModel)

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = DividerDefaults.Thickness,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // NEW SETTINGS GO HERE.

                    PeliasBaseUrlSetting(viewModel)

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = DividerDefaults.Thickness,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    PeliasApiKeySetting(viewModel)

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = DividerDefaults.Thickness,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    ValhallaBaseUrlSetting(viewModel)

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = DividerDefaults.Thickness,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    ValhallaApiKeySetting(viewModel)

                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(TOOLBAR_HEIGHT_DP)
                    )
                }
            }
        }
    )
}
