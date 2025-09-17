package earth.maps.cardinal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import earth.maps.cardinal.R
import earth.maps.cardinal.R.drawable
import earth.maps.cardinal.R.string
import earth.maps.cardinal.data.AppPreferenceRepository
import earth.maps.cardinal.data.AppPreferences

@Composable
private fun <T> PreferenceOption(
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

@Composable
private fun ExpandableSection(
    title: String, isExpanded: Boolean, onToggle: () -> Unit, content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = if (isExpanded) {
                    stringResource(string.content_description_collapse)
                } else {
                    stringResource(string.content_description_expand)
                }
            )
        }

        if (isExpanded) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onDismiss: () -> Unit,
    appPreferenceRepository: AppPreferenceRepository,
    navController: NavController
) {
    var isPrivacyExpanded by remember { mutableStateOf(false) }
    var isAccessibilityExpanded by remember { mutableStateOf(false) }
    var isAdvancedExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimensionResource(R.dimen.padding_minor))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(string.cardinal_maps_settings_heading),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(string.close)
                )
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
                .clickable { navController.navigate(Screen.RoutingProfiles.route) }
                .padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Routing Profiles",
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(
                    painter = painterResource(drawable.commute_icon),
                    contentDescription = null
                )
            }
            Text(
                text = "Create and manage custom routing profiles for different transportation modes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Privacy Section
        ExpandableSection(
            title = stringResource(string.privacy_settings_title),
            isExpanded = isPrivacyExpanded,
            onToggle = { isPrivacyExpanded = !isPrivacyExpanded }) {
            // Offline Areas Settings Item
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(Screen.OfflineAreas.route) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)) {
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
                    .padding(horizontal = 16.dp, vertical = 12.dp)
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
                val currentOfflineMode by appPreferenceRepository.offlineMode.collectAsState()
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
                            appPreferenceRepository.setOfflineMode(newValue)
                        })
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Accessibility Section
        ExpandableSection(
            title = stringResource(string.accessibility_settings_title),
            isExpanded = isAccessibilityExpanded,
            onToggle = { isAccessibilityExpanded = !isAccessibilityExpanded }) {
            // Contrast Settings Item
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
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
                val currentContrastLevel by appPreferenceRepository.contrastLevel.collectAsState()
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
                    appPreferenceRepository.setContrastLevel(newValue)
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
                    .padding(horizontal = 16.dp, vertical = 12.dp)
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
                val currentAnimationSpeed by appPreferenceRepository.animationSpeed.collectAsState()
                var selectedAnimationSpeed by remember { mutableIntStateOf(currentAnimationSpeed) }

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
                    appPreferenceRepository.setAnimationSpeed(newValue)
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Advanced Section
        ExpandableSection(
            title = stringResource(string.advanced_settings_title),
            isExpanded = isAdvancedExpanded,
            onToggle = { isAdvancedExpanded = !isAdvancedExpanded }) {
            // Pelias Base URL
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(string.pelias_base_url_title),
                    style = MaterialTheme.typography.titleMedium
                )

                val currentPeliasConfig by appPreferenceRepository.peliasApiConfig.collectAsState()
                var peliasBaseUrl by remember { mutableStateOf(currentPeliasConfig.baseUrl) }

                // Update state when config changes from outside
                LaunchedEffect(currentPeliasConfig) {
                    peliasBaseUrl = currentPeliasConfig.baseUrl
                }

                OutlinedTextField(
                    value = peliasBaseUrl,
                    onValueChange = { newValue ->
                        peliasBaseUrl = newValue
                        appPreferenceRepository.setPeliasBaseUrl(newValue)
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
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(string.pelias_api_key_title),
                    style = MaterialTheme.typography.titleMedium
                )

                val currentPeliasConfig by appPreferenceRepository.peliasApiConfig.collectAsState()
                var peliasApiKey by remember { mutableStateOf(currentPeliasConfig.apiKey ?: "") }

                // Update state when config changes from outside
                LaunchedEffect(currentPeliasConfig) {
                    peliasApiKey = currentPeliasConfig.apiKey ?: ""
                }

                OutlinedTextField(
                    value = peliasApiKey,
                    onValueChange = { newValue ->
                        peliasApiKey = newValue
                        appPreferenceRepository.setPeliasApiKey(if (newValue.isNotEmpty()) newValue else null)
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
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(string.valhalla_base_url_title),
                    style = MaterialTheme.typography.titleMedium
                )

                val currentValhallaConfig by appPreferenceRepository.valhallaApiConfig.collectAsState()
                var valhallaBaseUrl by remember { mutableStateOf(currentValhallaConfig.baseUrl) }

                // Update state when config changes from outside
                LaunchedEffect(currentValhallaConfig) {
                    valhallaBaseUrl = currentValhallaConfig.baseUrl
                }

                OutlinedTextField(
                    value = valhallaBaseUrl,
                    onValueChange = { newValue ->
                        valhallaBaseUrl = newValue
                        appPreferenceRepository.setValhallaBaseUrl(newValue)
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
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(string.valhalla_api_key_title),
                    style = MaterialTheme.typography.titleMedium
                )

                val currentValhallaConfig by appPreferenceRepository.valhallaApiConfig.collectAsState()
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
                        appPreferenceRepository.setValhallaApiKey(if (newValue.isNotEmpty()) newValue else null)
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
        }

        // Add some bottom padding to ensure proper spacing
        Box(modifier = Modifier.padding(bottom = 8.dp))
    }
}
