package earth.maps.cardinal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import earth.maps.cardinal.R
import earth.maps.cardinal.data.RoutingMode
import earth.maps.cardinal.data.RoutingProfile
import earth.maps.cardinal.viewmodel.RoutingProfilesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutingProfilesScreen(
    navController: NavController,
    viewModel: RoutingProfilesViewModel = hiltViewModel()
) {
    val allProfiles by viewModel.allProfiles.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.routing_profiles_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.ProfileEditor.route) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_profile))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutingModeSection(
    routingMode: RoutingMode,
    profiles: List<RoutingProfile>,
    navController: NavController,
    onSetDefault: (RoutingProfile) -> Unit,
    onDelete: (RoutingProfile) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
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
                    onClick = { navController.navigate("profile_editor?profileId=${profile.id}") },
                    onSetDefault = { onSetDefault(profile) },
                    onDelete = { onDelete(profile) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
    }
}

@Composable
private fun ProfileCard(
    profile: RoutingProfile,
    onClick: () -> Unit,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit
) {
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
                .padding(16.dp),
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
                            "MMM dd, yyyy",
                            java.util.Locale.getDefault()
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
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}
