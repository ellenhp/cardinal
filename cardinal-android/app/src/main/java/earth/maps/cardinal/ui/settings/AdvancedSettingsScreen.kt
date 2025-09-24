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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.drawable
import earth.maps.cardinal.R.string
import earth.maps.cardinal.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel<SettingsViewModel>(),
    onDismiss: () -> Unit
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
                text = stringResource(string.advanced_settings_title),
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

        // Pelias Base URL
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

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Pelias API Key
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
            var peliasApiKey by remember { mutableStateOf(currentPeliasConfig.apiKey ?: "") }

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

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Valhalla Base URL
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

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Valhalla API Key
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
}
