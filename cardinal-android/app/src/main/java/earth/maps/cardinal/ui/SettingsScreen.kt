package earth.maps.cardinal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import earth.maps.cardinal.R
import earth.maps.cardinal.R.drawable
import earth.maps.cardinal.data.AppPreferenceRepository
import earth.maps.cardinal.data.AppPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onDismiss: () -> Unit,
    appPreferenceRepository: AppPreferenceRepository,
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
                text = stringResource(R.string.cardinal_maps_settings_heading),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close)
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Offline Areas Settings Item
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { navController.navigate(Screen.OfflineAreas.route) }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.offline_areas_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(
                    painter = painterResource(drawable.cloud_download_24dp),
                    contentDescription = null
                )
            }
            Text(
                text = stringResource(R.string.offline_areas_help_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Contrast Settings Item
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.contrast_settings_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.contrast_settings_help_text),
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

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedContrastLevel = AppPreferences.CONTRAST_LEVEL_STANDARD
                            appPreferenceRepository.setContrastLevel(AppPreferences.CONTRAST_LEVEL_STANDARD)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedContrastLevel == AppPreferences.CONTRAST_LEVEL_STANDARD,
                        onClick = {
                            selectedContrastLevel = AppPreferences.CONTRAST_LEVEL_STANDARD
                            appPreferenceRepository.setContrastLevel(AppPreferences.CONTRAST_LEVEL_STANDARD)
                        }
                    )
                    Text(
                        text = stringResource(R.string.contrast_standard),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedContrastLevel = AppPreferences.CONTRAST_LEVEL_MEDIUM
                            appPreferenceRepository.setContrastLevel(AppPreferences.CONTRAST_LEVEL_MEDIUM)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedContrastLevel == AppPreferences.CONTRAST_LEVEL_MEDIUM,
                        onClick = {
                            selectedContrastLevel = AppPreferences.CONTRAST_LEVEL_MEDIUM
                            appPreferenceRepository.setContrastLevel(AppPreferences.CONTRAST_LEVEL_MEDIUM)
                        }
                    )
                    Text(
                        text = stringResource(R.string.contrast_medium),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedContrastLevel = AppPreferences.CONTRAST_LEVEL_HIGH
                            appPreferenceRepository.setContrastLevel(AppPreferences.CONTRAST_LEVEL_HIGH)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedContrastLevel == AppPreferences.CONTRAST_LEVEL_HIGH,
                        onClick = {
                            selectedContrastLevel = AppPreferences.CONTRAST_LEVEL_HIGH
                            appPreferenceRepository.setContrastLevel(AppPreferences.CONTRAST_LEVEL_HIGH)
                        }
                    )
                    Text(
                        text = stringResource(R.string.contrast_high),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
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
                text = stringResource(R.string.animation_speed_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.animation_speed_help_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Animation speed selection
            val currentAnimationSpeed by appPreferenceRepository.animationSpeed.collectAsState()
            var selectedAnimationSpeed by remember { mutableStateOf(currentAnimationSpeed) }

            // Update selected state when preference changes from outside
            LaunchedEffect(currentAnimationSpeed) {
                selectedAnimationSpeed = currentAnimationSpeed
            }

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedAnimationSpeed = AppPreferences.ANIMATION_SPEED_SLOW
                            appPreferenceRepository.setAnimationSpeed(AppPreferences.ANIMATION_SPEED_SLOW)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedAnimationSpeed == AppPreferences.ANIMATION_SPEED_SLOW,
                        onClick = {
                            selectedAnimationSpeed = AppPreferences.ANIMATION_SPEED_SLOW
                            appPreferenceRepository.setAnimationSpeed(AppPreferences.ANIMATION_SPEED_SLOW)
                        }
                    )
                    Text(
                        text = stringResource(R.string.animation_speed_slow),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedAnimationSpeed = AppPreferences.ANIMATION_SPEED_NORMAL
                            appPreferenceRepository.setAnimationSpeed(AppPreferences.ANIMATION_SPEED_NORMAL)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedAnimationSpeed == AppPreferences.ANIMATION_SPEED_NORMAL,
                        onClick = {
                            selectedAnimationSpeed = AppPreferences.ANIMATION_SPEED_NORMAL
                            appPreferenceRepository.setAnimationSpeed(AppPreferences.ANIMATION_SPEED_NORMAL)
                        }
                    )
                    Text(
                        text = stringResource(R.string.animation_speed_normal),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedAnimationSpeed = AppPreferences.ANIMATION_SPEED_FAST
                            appPreferenceRepository.setAnimationSpeed(AppPreferences.ANIMATION_SPEED_FAST)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedAnimationSpeed == AppPreferences.ANIMATION_SPEED_FAST,
                        onClick = {
                            selectedAnimationSpeed = AppPreferences.ANIMATION_SPEED_FAST
                            appPreferenceRepository.setAnimationSpeed(AppPreferences.ANIMATION_SPEED_FAST)
                        }
                    )
                    Text(
                        text = stringResource(R.string.animation_speed_fast),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Add some bottom padding to ensure proper spacing
        Box(modifier = Modifier.padding(bottom = 8.dp))
    }
}
