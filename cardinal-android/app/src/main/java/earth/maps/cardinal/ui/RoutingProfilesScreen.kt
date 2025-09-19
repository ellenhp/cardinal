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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import earth.maps.cardinal.R
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.string
import earth.maps.cardinal.data.RoutingMode
import earth.maps.cardinal.data.RoutingProfile
import earth.maps.cardinal.viewmodel.RoutingProfilesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutingProfilesScreen(
    navigationCoordinator: NavigationCoordinator,
    viewModel: RoutingProfilesViewModel = hiltViewModel()
) {
    val allProfiles by viewModel.allProfiles.collectAsState()

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
                text = stringResource(string.routing_profiles),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = { navigationCoordinator.navigateBack() }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(string.close)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = dimensionResource(dimen.padding))
        ) {
            if (allProfiles.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_routing_profiles_configured_yet)
                )
            }
            // Group profiles by routing mode
            RoutingMode.entries.forEach { mode ->
                val modeProfiles = allProfiles.filter { it.routingMode == mode.value }

                if (modeProfiles.isNotEmpty()) {
                    RoutingModeSection(
                        routingMode = mode,
                        profiles = modeProfiles,
                        navigationCoordinator = navigationCoordinator,
                        onSetDefault = { profile ->
                            viewModel.setDefaultProfile(profile.id)
                        },
                        onDelete = { profile ->
                            viewModel.deleteProfile(profile.id)
                        })
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.safeContent.asPaddingValues())
    ) {
        // Floating Action Button positioned at bottom right
        FloatingActionButton(
            onClick = { navigationCoordinator.navigateToRoutingProfileEditor() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(dimensionResource(dimen.padding))
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_profile))
        }
    }
}

@Composable
private fun RoutingModeSection(
    routingMode: RoutingMode,
    profiles: List<RoutingProfile>,
    navigationCoordinator: NavigationCoordinator,
    onSetDefault: (RoutingProfile) -> Unit,
    onDelete: (RoutingProfile) -> Unit
) {
    Column(modifier = Modifier.padding(dimensionResource(dimen.padding))) {
        Text(
            text = routingMode.label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (profiles.isEmpty()) {
            Text(
                text = "No profiles yet. Tap + to create one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            profiles.forEach { profile ->
                ProfileCard(
                    profile = profile,
                    onClick = { navigationCoordinator.navigateToRoutingProfileEditor(profile) },
                    onSetDefault = { onSetDefault(profile) },
                    onDelete = { onDelete(profile) })
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = dimensionResource(dimen.padding)))
    }
}

@Composable
private fun ProfileCard(
    profile: RoutingProfile, onClick: () -> Unit, onSetDefault: () -> Unit, onDelete: () -> Unit
) {
    val showDeleteDialog = remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(dimen.padding)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (profile.isDefault) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Default",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                Text(
                    text = "Last modified: ${
                        java.text.SimpleDateFormat(
                            "MMM dd, yyyy", java.util.Locale.getDefault()
                        ).format(java.util.Date(profile.updatedAt))
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                IconButton(onClick = onSetDefault) {
                    Icon(
                        imageVector = if (profile.isDefault) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = if (profile.isDefault) "Remove as default" else "Set as default"
                    )
                }
                IconButton(onClick = onClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = { showDeleteDialog.value = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }

    if (showDeleteDialog.value) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog.value = false },
            title = { Text("Delete Profile") },
            text = { Text("Are you sure you want to delete this routing profile?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog.value = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog.value = false }) {
                    Text("Cancel")
                }
            })
    }
}
