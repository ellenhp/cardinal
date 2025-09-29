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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.string
import earth.maps.cardinal.data.AppPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilitySettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel<SettingsViewModel>(),
    onDismiss: () -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(title = {
                Text(
                    text = stringResource(string.accessibility_settings_title),
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
                    // Contrast Settings Item
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = dimensionResource(dimen.padding),
                                vertical = dimensionResource(dimen.padding_minor)
                            )
                    ) {
                        Text(
                            text = stringResource(string.contrast_settings_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(string.contrast_settings_help_text),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Contrast level selection
                        val currentContrastLevel by viewModel.contrastLevel.collectAsState()
                        var selectedContrastLevel by remember { mutableStateOf(currentContrastLevel) }

                        // Update selected state when preference changes from outside
                        LaunchedEffect(currentContrastLevel) {
                            selectedContrastLevel = currentContrastLevel
                        }

                        PreferenceOption(
                            selectedValue = selectedContrastLevel, options = listOf(
                                AppPreferences.CONTRAST_LEVEL_STANDARD to string.contrast_standard,
                                AppPreferences.CONTRAST_LEVEL_MEDIUM to string.contrast_medium,
                                AppPreferences.CONTRAST_LEVEL_HIGH to string.contrast_high
                            )
                        ) { newValue ->
                            selectedContrastLevel = newValue
                            viewModel.setContrastLevel(newValue)
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = DividerDefaults.Thickness,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Animation Speed Settings Item
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = dimensionResource(dimen.padding),
                                vertical = dimensionResource(dimen.padding_minor)
                            )
                    ) {
                        Text(
                            text = stringResource(string.animation_speed_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(string.animation_speed_help_text),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Animation speed selection
                        val currentAnimationSpeed by viewModel.animationSpeed.collectAsState()
                        var selectedAnimationSpeed by remember {
                            mutableIntStateOf(
                                currentAnimationSpeed
                            )
                        }

                        // Update selected state when preference changes from outside
                        LaunchedEffect(currentAnimationSpeed) {
                            selectedAnimationSpeed = currentAnimationSpeed
                        }

                        PreferenceOption(
                            selectedValue = selectedAnimationSpeed, options = listOf(
                                AppPreferences.ANIMATION_SPEED_SLOW to string.animation_speed_slow,
                                AppPreferences.ANIMATION_SPEED_NORMAL to string.animation_speed_normal,
                                AppPreferences.ANIMATION_SPEED_FAST to string.animation_speed_fast
                            )
                        ) { newValue ->
                            selectedAnimationSpeed = newValue
                            viewModel.setAnimationSpeed(newValue)
                        }
                    }
                }
            }
        }
    )
}
