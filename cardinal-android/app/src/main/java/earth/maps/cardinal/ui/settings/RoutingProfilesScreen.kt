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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import earth.maps.cardinal.R.dimen
import earth.maps.cardinal.R.drawable
import earth.maps.cardinal.R.string
import earth.maps.cardinal.data.RoutingMode
import earth.maps.cardinal.data.room.RoutingProfile
import earth.maps.cardinal.ui.core.NavigationUtils
import earth.maps.cardinal.ui.core.Screen
import earth.maps.cardinal.ui.core.TOOLBAR_HEIGHT_DP
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutingProfilesScreen(
    viewModel: RoutingProfilesViewModel = hiltViewModel(), navController: NavController
) {
    val allProfiles by viewModel.allProfiles.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(string.routing_profiles),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        content = { padding ->
            Box(modifier = Modifier.padding(padding)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(dimensionResource(dimen.padding))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = dimensionResource(dimen.padding))
                    ) {
                        if (allProfiles.isEmpty()) {
                            Text(
                                text = stringResource(string.no_routing_profiles_configured_yet)
                            )
                        }
                        // Group profiles by routing mode
                        RoutingMode.entries.forEach { mode ->
                            val modeProfiles = allProfiles.filter { it.routingMode == mode.value }

                            if (modeProfiles.isNotEmpty()) {
                                RoutingModeSection(
                                    routingMode = mode,
                                    profiles = modeProfiles,
                                    navController = navController,
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
                        .padding(bottom = TOOLBAR_HEIGHT_DP)
                ) {
                    // Floating Action Button positioned at bottom right
                    FloatingActionButton(
                        onClick = {
                            NavigationUtils.navigate(
                                navController,
                                Screen.ProfileEditor(null)
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(dimensionResource(dimen.padding))
                    ) {
                        Icon(
                            painter = painterResource(drawable.ic_add),
                            contentDescription = stringResource(string.add_profile)
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun RoutingModeSection(
    routingMode: RoutingMode,
    profiles: List<RoutingProfile>,
    navController: NavController,
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
                ProfileCard(profile = profile, onClick = {
                    NavigationUtils.navigate(
                        navController, Screen.ProfileEditor(profile.id)
                    )
                }, onSetDefault = { onSetDefault(profile) }, onDelete = { onDelete(profile) })
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
                            painter = painterResource(drawable.ic_star),
                            contentDescription = "Default",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                Text(
                    text = "Last modified: ${
                        SimpleDateFormat(
                            "MMM dd, yyyy", Locale.getDefault()
                        ).format(Date(profile.updatedAt))
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                IconButton(onClick = onSetDefault) {
                    Icon(
                        painter = painterResource(drawable.ic_star),
                        contentDescription = if (profile.isDefault) stringResource(string.remove_as_default) else stringResource(
                            string.set_as_default
                        )
                    )
                }
                IconButton(onClick = onClick) {
                    Icon(

                        painter = painterResource(drawable.ic_edit),
                        contentDescription = stringResource(string.content_description_edit_routing_profile)
                    )
                }
                IconButton(onClick = { showDeleteDialog.value = true }) {
                    Icon(
                        painter = painterResource(drawable.ic_delete),
                        contentDescription = stringResource(string.content_description_delete_routing_profile)
                    )
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
                    Text(stringResource(string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog.value = false }) {
                    Text(stringResource(string.cancel))
                }
            })
    }
}
