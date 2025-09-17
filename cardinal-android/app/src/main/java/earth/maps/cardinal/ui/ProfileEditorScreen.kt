package earth.maps.cardinal.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import earth.maps.cardinal.data.RoutingMode
import earth.maps.cardinal.routing.AutoOptions
import earth.maps.cardinal.routing.AutoRoutingOptions
import earth.maps.cardinal.routing.BicycleType
import earth.maps.cardinal.routing.CyclingRoutingOptions
import earth.maps.cardinal.routing.MotorScooterRoutingOptions
import earth.maps.cardinal.routing.MotorcycleRoutingOptions
import earth.maps.cardinal.routing.PedestrianRoutingOptions
import earth.maps.cardinal.routing.PedestrianType
import earth.maps.cardinal.routing.RoutingOptions
import earth.maps.cardinal.routing.TruckRoutingOptions
import earth.maps.cardinal.viewmodel.ProfileEditorViewModel
import kotlin.math.exp
import kotlin.math.ln

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorScreen(
    navController: NavController,
    profileId: String? = null,
    snackbarHostState: SnackbarHostState,
    viewModel: ProfileEditorViewModel = hiltViewModel()
) {
    val profileName by viewModel.profileName.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    val routingOptions by viewModel.routingOptions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isNewProfile by viewModel.isNewProfile.collectAsState()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()

    var showUnsavedChangesDialog by remember { mutableStateOf(false) }

    LaunchedEffect(profileId) {
        viewModel.loadProfile(profileId)
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Handle back navigation
    fun handleBackNavigation() {
        if (!isNewProfile && hasUnsavedChanges) {
            showUnsavedChangesDialog = true
        } else {
            navController.popBackStack()
        }
    }

    // Handle system back button
    if (!showUnsavedChangesDialog) {
        BackHandler {
            handleBackNavigation()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Custom app bar using Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { handleBackNavigation() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }

            Text(
                text = if (isNewProfile) "Create Profile" else "Edit Profile",
                style = MaterialTheme.typography.titleLarge
            )

            if (!isLoading) {
                IconButton(
                    onClick = {
                        viewModel.saveProfile {
                            navController.popBackStack()
                        }
                    }
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Save")
                }
            } else {
                // Placeholder to maintain layout
                IconButton(onClick = {}, enabled = false) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Transparent)
                }
            }
        }

        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp), // Reduced padding since we removed TopAppBar
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp) // Reduced padding since we removed TopAppBar
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Name
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { viewModel.updateProfileName(it) },
                    label = { Text("Profile Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Routing Mode Selector
                RoutingModeSelector(
                    selectedMode = selectedMode,
                    onModeSelected = { viewModel.updateRoutingMode(it) }
                )

                // Options Editor based on mode
                when (selectedMode) {
                    RoutingMode.AUTO -> AutoOptionsEditor(routingOptions as AutoRoutingOptions) {
                        viewModel.updateRoutingOptions(
                            it
                        )
                    }

                    RoutingMode.TRUCK -> TruckOptionsEditor(routingOptions as TruckRoutingOptions) {
                        viewModel.updateRoutingOptions(
                            it
                        )
                    }

                    RoutingMode.MOTOR_SCOOTER -> MotorScooterOptionsEditor(routingOptions as MotorScooterRoutingOptions) {
                        viewModel.updateRoutingOptions(
                            it
                        )
                    }

                    RoutingMode.MOTORCYCLE -> MotorcycleOptionsEditor(routingOptions as MotorcycleRoutingOptions) {
                        viewModel.updateRoutingOptions(
                            it
                        )
                    }

                    RoutingMode.BICYCLE -> CyclingOptionsEditor(routingOptions as CyclingRoutingOptions) {
                        viewModel.updateRoutingOptions(
                            it
                        )
                    }

                    RoutingMode.PEDESTRIAN -> PedestrianOptionsEditor(routingOptions as PedestrianRoutingOptions) {
                        viewModel.updateRoutingOptions(
                            it
                        )
                    }
                }
            }
        }
    }

    // Unsaved changes dialog
    if (showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            title = { Text("Unsaved Changes") },
            text = {
                Text("You have unsaved changes. What would you like to do?")
                BackHandler {
                    showUnsavedChangesDialog = false
                    navController.popBackStack()
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnsavedChangesDialog = false
                        viewModel.saveProfile {
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showUnsavedChangesDialog = false
                        navController.popBackStack()
                    }
                ) {
                    Text("Discard")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutingModeSelector(
    selectedMode: RoutingMode,
    onModeSelected: (RoutingMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedMode.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Routing Mode") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            RoutingMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label) },
                    onClick = {
                        onModeSelected(mode)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun <T> CommonAutoOptionsEditor(
    options: T,
    onUseHighwaysChanged: (Double?) -> Unit,
    onUseTollsChanged: (Double?) -> Unit,
    onUseLivingStreetsChanged: (Double?) -> Unit,
    onUseTracksChanged: (Double?) -> Unit,
    onExcludeUnpavedChanged: (Boolean?) -> Unit,
    onExcludeCashOnlyTollsChanged: (Boolean?) -> Unit,
    onManeuverPenaltyChanged: (Double?) -> Unit,
    onGateCostChanged: (Double?) -> Unit,
    onPrivateAccessPenaltyChanged: (Double?) -> Unit,
    onIgnoreClosuresChanged: (Boolean?) -> Unit,
    onIgnoreRestrictionsChanged: (Boolean?) -> Unit,
    onIgnoreOneWaysChanged: (Boolean?) -> Unit,
    onIgnoreAccessChanged: (Boolean?) -> Unit
) where T : AutoOptions, T : RoutingOptions {
    OptionsSection("Route Preferences") {
        SliderOption(
            "Use Highways",
            options.useHighways,
            0f..1f,
            valueFormatter = { it.format(2) },
            onValueChanged = onUseHighwaysChanged
        )
        SliderOption(
            "Use Tolls",
            options.useTolls,
            0f..1f,
            valueFormatter = { it.format(2) },
            onValueChanged = onUseTollsChanged
        )
        SliderOption(
            "Use Living Streets",
            options.useLivingStreets,
            0f..1f,
            valueFormatter = { it.format(2) },
            onValueChanged = onUseLivingStreetsChanged
        )
        SliderOption(
            "Use Tracks",
            options.useTracks,
            0f..1f,
            valueFormatter = { it.format(2) },
            onValueChanged = onUseTracksChanged
        )
        BooleanOption("Avoid Unpaved", options.excludeUnpaved, onExcludeUnpavedChanged)
        BooleanOption("Avoid Cash-only Tolls", options.excludeCashOnlyTolls, onExcludeCashOnlyTollsChanged)
    }

    OptionsSection("Penalties") {
        SliderOption(
            "Maneuver Penalty",
            options.maneuverPenalty,
            0f..43200f,
            useLogScale = true,
            valueFormatter = ::formatTime,
            onValueChanged = onManeuverPenaltyChanged
        )
        SliderOption(
            "Gate Cost",
            options.gateCost,
            0f..43200f,
            useLogScale = true,
            valueFormatter = ::formatTime,
            onValueChanged = onGateCostChanged
        )
        SliderOption(
            "Private Access Penalty",
            options.privateAccessPenalty,
            0f..43200f,
            useLogScale = true,
            valueFormatter = ::formatTime,
            onValueChanged = onPrivateAccessPenaltyChanged
        )
    }

    OptionsSection("Restrictions") {
        BooleanOption("Ignore Closures", options.ignoreClosures, onIgnoreClosuresChanged)
        BooleanOption("Ignore Restrictions", options.ignoreRestrictions, onIgnoreRestrictionsChanged)
        BooleanOption("Ignore One-ways", options.ignoreOneWays, onIgnoreOneWaysChanged)
        BooleanOption("Ignore Access", options.ignoreAccess, onIgnoreAccessChanged)
    }
}

@Composable
private fun AutoOptionsEditor(
    options: AutoRoutingOptions,
    onOptionsChanged: (AutoRoutingOptions) -> Unit
) {
    CommonAutoOptionsEditor(
        options = options,
        onUseHighwaysChanged = { value -> onOptionsChanged(options.copy(useHighways = value)) },
        onUseTollsChanged = { value -> onOptionsChanged(options.copy(useTolls = value)) },
        onUseLivingStreetsChanged = { value -> onOptionsChanged(options.copy(useLivingStreets = value)) },
        onUseTracksChanged = { value -> onOptionsChanged(options.copy(useTracks = value)) },
        onExcludeUnpavedChanged = { value -> onOptionsChanged(options.copy(excludeUnpaved = value)) },
        onExcludeCashOnlyTollsChanged = { value -> onOptionsChanged(options.copy(excludeCashOnlyTolls = value)) },
        onManeuverPenaltyChanged = { value -> onOptionsChanged(options.copy(maneuverPenalty = value)) },
        onGateCostChanged = { value -> onOptionsChanged(options.copy(gateCost = value)) },
        onPrivateAccessPenaltyChanged = { value -> onOptionsChanged(options.copy(privateAccessPenalty = value)) },
        onIgnoreClosuresChanged = { value -> onOptionsChanged(options.copy(ignoreClosures = value)) },
        onIgnoreRestrictionsChanged = { value -> onOptionsChanged(options.copy(ignoreRestrictions = value)) },
        onIgnoreOneWaysChanged = { value -> onOptionsChanged(options.copy(ignoreOneWays = value)) },
        onIgnoreAccessChanged = { value -> onOptionsChanged(options.copy(ignoreAccess = value)) }
    )
}

@Composable
private fun TruckOptionsEditor(
    options: TruckRoutingOptions,
    onOptionsChanged: (TruckRoutingOptions) -> Unit
) {
    CommonAutoOptionsEditor(
        options = options,
        onUseHighwaysChanged = { value -> onOptionsChanged(options.copy(useHighways = value)) },
        onUseTollsChanged = { value -> onOptionsChanged(options.copy(useTolls = value)) },
        onUseLivingStreetsChanged = { value -> onOptionsChanged(options.copy(useLivingStreets = value)) },
        onUseTracksChanged = { value -> onOptionsChanged(options.copy(useTracks = value)) },
        onExcludeUnpavedChanged = { value -> onOptionsChanged(options.copy(excludeUnpaved = value)) },
        onExcludeCashOnlyTollsChanged = { value -> onOptionsChanged(options.copy(excludeCashOnlyTolls = value)) },
        onManeuverPenaltyChanged = { value -> onOptionsChanged(options.copy(maneuverPenalty = value)) },
        onGateCostChanged = { value -> onOptionsChanged(options.copy(gateCost = value)) },
        onPrivateAccessPenaltyChanged = { value -> onOptionsChanged(options.copy(privateAccessPenalty = value)) },
        onIgnoreClosuresChanged = { value -> onOptionsChanged(options.copy(ignoreClosures = value)) },
        onIgnoreRestrictionsChanged = { value -> onOptionsChanged(options.copy(ignoreRestrictions = value)) },
        onIgnoreOneWaysChanged = { value -> onOptionsChanged(options.copy(ignoreOneWays = value)) },
        onIgnoreAccessChanged = { value -> onOptionsChanged(options.copy(ignoreAccess = value)) }
    )

    OptionsSection("Vehicle Dimensions") {
        SliderOption(
            "Length (m)",
            options.length,
            1f..50f,
            valueFormatter = { it.format(1) }) { value ->
            onOptionsChanged(options.copy(length = value))
        }
        SliderOption(
            "Width (m)",
            options.width,
            1f..5f,
            valueFormatter = { it.format(1) }) { value ->
            onOptionsChanged(options.copy(width = value))
        }
        SliderOption(
            "Height (m)",
            options.height,
            1f..10f,
            valueFormatter = { it.format(1) }) { value ->
            onOptionsChanged(options.copy(height = value))
        }
        SliderOption(
            "Weight (tons)",
            options.weight,
            0.1f..100f,
            valueFormatter = { it.format(1) }) { value ->
            onOptionsChanged(options.copy(weight = value))
        }
    }

    OptionsSection("Truck Restrictions") {
        BooleanOption("Hazmat", options.hazmat) { value ->
            onOptionsChanged(options.copy(hazmat = value))
        }
        SliderOption(
            "Axle Count",
            options.axleCount?.toDouble(),
            2f..20f,
            valueFormatter = { it.format(0) }) { value ->
            onOptionsChanged(options.copy(axleCount = value?.toInt()))
        }
        SliderOption(
            "Use Truck Route",
            options.useTruckRoute,
            0f..1f,
            valueFormatter = { it.format(2) }) { value ->
            onOptionsChanged(options.copy(useTruckRoute = value))
        }
    }
}

@Composable
private fun MotorScooterOptionsEditor(
    options: MotorScooterRoutingOptions,
    onOptionsChanged: (MotorScooterRoutingOptions) -> Unit
) {
    CommonAutoOptionsEditor(
        options = options,
        onUseHighwaysChanged = { value -> onOptionsChanged(options.copy(useHighways = value)) },
        onUseTollsChanged = { value -> onOptionsChanged(options.copy(useTolls = value)) },
        onUseLivingStreetsChanged = { value -> onOptionsChanged(options.copy(useLivingStreets = value)) },
        onUseTracksChanged = { value -> onOptionsChanged(options.copy(useTracks = value)) },
        onExcludeUnpavedChanged = { value -> onOptionsChanged(options.copy(excludeUnpaved = value)) },
        onExcludeCashOnlyTollsChanged = { value -> onOptionsChanged(options.copy(excludeCashOnlyTolls = value)) },
        onManeuverPenaltyChanged = { value -> onOptionsChanged(options.copy(maneuverPenalty = value)) },
        onGateCostChanged = { value -> onOptionsChanged(options.copy(gateCost = value)) },
        onPrivateAccessPenaltyChanged = { value -> onOptionsChanged(options.copy(privateAccessPenalty = value)) },
        onIgnoreClosuresChanged = { value -> onOptionsChanged(options.copy(ignoreClosures = value)) },
        onIgnoreRestrictionsChanged = { value -> onOptionsChanged(options.copy(ignoreRestrictions = value)) },
        onIgnoreOneWaysChanged = { value -> onOptionsChanged(options.copy(ignoreOneWays = value)) },
        onIgnoreAccessChanged = { value -> onOptionsChanged(options.copy(ignoreAccess = value)) }
    )

    OptionsSection("Motor Scooter Preferences") {
        SliderOption(
            "Use Primary Roads",
            options.usePrimary,
            0f..1f,
            valueFormatter = { it.format(2) }) { value ->
            onOptionsChanged(options.copy(usePrimary = value))
        }
        SliderOption(
            "Use Hills",
            options.useHills,
            0f..1f,
            valueFormatter = { it.format(2) }) { value ->
            onOptionsChanged(options.copy(useHills = value))
        }
    }
}

@Composable
private fun MotorcycleOptionsEditor(
    options: MotorcycleRoutingOptions,
    onOptionsChanged: (MotorcycleRoutingOptions) -> Unit
) {
    CommonAutoOptionsEditor(
        options = options,
        onUseHighwaysChanged = { value -> onOptionsChanged(options.copy(useHighways = value)) },
        onUseTollsChanged = { value -> onOptionsChanged(options.copy(useTolls = value)) },
        onUseLivingStreetsChanged = { value -> onOptionsChanged(options.copy(useLivingStreets = value)) },
        onUseTracksChanged = { value -> onOptionsChanged(options.copy(useTracks = value)) },
        onExcludeUnpavedChanged = { value -> onOptionsChanged(options.copy(excludeUnpaved = value)) },
        onExcludeCashOnlyTollsChanged = { value -> onOptionsChanged(options.copy(excludeCashOnlyTolls = value)) },
        onManeuverPenaltyChanged = { value -> onOptionsChanged(options.copy(maneuverPenalty = value)) },
        onGateCostChanged = { value -> onOptionsChanged(options.copy(gateCost = value)) },
        onPrivateAccessPenaltyChanged = { value -> onOptionsChanged(options.copy(privateAccessPenalty = value)) },
        onIgnoreClosuresChanged = { value -> onOptionsChanged(options.copy(ignoreClosures = value)) },
        onIgnoreRestrictionsChanged = { value -> onOptionsChanged(options.copy(ignoreRestrictions = value)) },
        onIgnoreOneWaysChanged = { value -> onOptionsChanged(options.copy(ignoreOneWays = value)) },
        onIgnoreAccessChanged = { value -> onOptionsChanged(options.copy(ignoreAccess = value)) }
    )

    OptionsSection("Motorcycle Preferences") {
        SliderOption(
            "Use Trails",
            options.useTrails,
            0f..1f,
            valueFormatter = { it.format(2) }) { value ->
            onOptionsChanged(options.copy(useTrails = value))
        }
    }
}

@Composable
private fun CyclingOptionsEditor(
    options: CyclingRoutingOptions,
    onOptionsChanged: (CyclingRoutingOptions) -> Unit
) {
    OptionsSection("Bike Type") {
        EnumDropdownOption(
            label = "Bicycle Type",
            selectedValue = options.bicycleType,
            values = BicycleType.entries.toTypedArray(),
            displayName = { it.displayName },
            onValueChanged = { bicycleType ->
                onOptionsChanged(options.copy(bicycleType = bicycleType))
            }
        )
    }

    OptionsSection("Route Preferences") {
        SliderOption(
            "Cycling Speed (km/h)",
            options.cyclingSpeed,
            0f..50f,
            valueFormatter = { it.format(1) }) { value ->
            onOptionsChanged(options.copy(cyclingSpeed = value))
        }
        SliderOption(
            "Use Roads",
            options.useRoads,
            0f..1f,
            valueFormatter = { it.format(2) }) { value ->
            onOptionsChanged(options.copy(useRoads = value))
        }
        SliderOption(
            "Use Hills",
            options.useHills,
            0f..1f,
            valueFormatter = { it.format(2) }) { value ->
            onOptionsChanged(options.copy(useHills = value))
        }
        SliderOption(
            "Avoid Bad Surfaces",
            options.avoidBadSurfaces,
            0f..1f,
            valueFormatter = { it.format(2) }) { value ->
            onOptionsChanged(options.copy(avoidBadSurfaces = value))
        }
    }
}

@Composable
private fun PedestrianOptionsEditor(
    options: PedestrianRoutingOptions,
    onOptionsChanged: (PedestrianRoutingOptions) -> Unit
) {
    OptionsSection("Walking Preferences") {
        SliderOption(
            "Walking Speed (km/h)",
            options.walkingSpeed,
            0.5f..25f,
            valueFormatter = { it.format(1) }) { value ->
            onOptionsChanged(options.copy(walkingSpeed = value))
        }
        EnumDropdownOption(
            label = "Pedestrian Type",
            selectedValue = options.type,
            values = PedestrianType.entries.toTypedArray(),
            displayName = { it.displayName },
            onValueChanged = { pedestrianType ->
                onOptionsChanged(options.copy(type = pedestrianType))
            }
        )
    }

    OptionsSection("Path Preferences") {
        SliderOption(
            "Walkway Factor",
            options.walkwayFactor,
            0f..1f,
            valueFormatter = { it.format(2) }) { value ->
            onOptionsChanged(options.copy(walkwayFactor = value))
        }
        SliderOption(
            "Sidewalk Factor",
            options.sidewalkFactor,
            0f..1f,
            valueFormatter = { it.format(2) }) { value ->
            onOptionsChanged(options.copy(sidewalkFactor = value))
        }
        SliderOption(
            "Use Lit Paths",
            options.useLit,
            0f..1f,
            valueFormatter = { it.format(2) }) { value ->
            onOptionsChanged(options.copy(useLit = value))
        }
    }
}

@Composable
private fun OptionsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun SliderOption(
    label: String,
    value: Double?,
    range: ClosedFloatingPointRange<Float>,
    valueFormatter: (Double) -> String,
    useLogScale: Boolean = false,
    onValueChanged: (Double?) -> Unit,
) {
    if (value != null) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$label: ${valueFormatter(value)}")
                TextButton(onClick = { onValueChanged(null) }) {
                    Text("Reset")
                }
            }
            val sliderValue = if (useLogScale) {
                valueToLogSlider(value.toFloat(), range.start, range.endInclusive)
            } else {
                valueToLinearSlider(value.toFloat(), range.start, range.endInclusive)
            }
            Slider(
                value = sliderValue,
                onValueChange = { sliderVal ->
                    val actualValue = if (useLogScale) {
                        logSliderToValue(sliderVal, range.start, range.endInclusive)
                    } else {
                        linearSliderToValue(sliderVal, range.start, range.endInclusive)
                    }
                    onValueChanged(actualValue.toDouble())
                },
                valueRange = 0f..1f
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label)
            TextButton(onClick = { onValueChanged(range.start.toDouble()) }) {
                Text("Set")
            }
        }
    }
}

// Convert linear value to logarithmic slider position (0-1)
private fun valueToLogSlider(value: Float, min: Float, max: Float): Float {
    if (value <= min) return 0f
    if (value >= max) return 1f

    val logMin = ln(maxOf(min, 0.01f)) // Avoid log(0)
    val logMax = ln(max)
    val logValue = ln(value)

    return (logValue - logMin) / (logMax - logMin)
}

// Convert logarithmic slider position (0-1) to linear value
private fun logSliderToValue(sliderValue: Float, min: Float, max: Float): Float {
    if (sliderValue <= 0f) return min
    if (sliderValue >= 1f) return max

    val logMin = ln(maxOf(min, 0.01f)) // Avoid log(0)
    val logMax = ln(max)

    return exp(logMin + sliderValue * (logMax - logMin))
}

// Convert linear value to linear slider position (0-1)
private fun valueToLinearSlider(value: Float, min: Float, max: Float): Float {
    if (value <= min) return 0f
    if (value >= max) return 1f

    return (value - min) / (max - min)
}

// Convert linear slider position (0-1) to linear value
private fun linearSliderToValue(sliderValue: Float, min: Float, max: Float): Float {
    if (sliderValue <= 0f) return min
    if (sliderValue >= 1f) return max

    return min + sliderValue * (max - min)
}

// Format time values in user-friendly units
private fun formatTime(seconds: Double): String {
    return when {
        seconds < 60 -> "${seconds.format(0)}s"
        seconds < 3600 -> "${(seconds / 60).format(1)}m"
        else -> "${(seconds / 3600).format(1)}h"
    }
}

@Composable
private fun BooleanOption(
    label: String,
    value: Boolean?,
    onValueChanged: (Boolean?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                TextButton(onClick = { onValueChanged(null) }) {
                    Text("Reset")
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Switch(
                checked = value ?: false,
                onCheckedChange = { onValueChanged(if (value == null) it else null) }
            )
        }
    }
}

@Composable
private fun NumberOption(
    label: String,
    value: Double?,
    onValueChanged: (Double?) -> Unit
) {
    if (value != null) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$label: $value")
                TextButton(onClick = { onValueChanged(null) }) {
                    Text("Reset")
                }
            }
            OutlinedTextField(
                value = value.toString(),
                onValueChange = { newValue ->
                    newValue.toDoubleOrNull()?.let { onValueChanged(it) }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label)
            TextButton(onClick = { onValueChanged(0.0) }) {
                Text("Set")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T : Enum<T>> EnumDropdownOption(
    label: String,
    selectedValue: T?,
    values: Array<T>,
    displayName: (T) -> String,
    onValueChanged: (T?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedValue?.let { displayName(it) } ?: "Select $label",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            values.forEach { value ->
                DropdownMenuItem(
                    text = { Text(displayName(value)) },
                    onClick = {
                        onValueChanged(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)
