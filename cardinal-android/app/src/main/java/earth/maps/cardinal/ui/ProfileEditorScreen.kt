package earth.maps.cardinal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import earth.maps.cardinal.data.RoutingMode
import earth.maps.cardinal.routing.AutoRoutingOptions
import earth.maps.cardinal.routing.CyclingRoutingOptions
import earth.maps.cardinal.routing.MotorScooterRoutingOptions
import earth.maps.cardinal.routing.MotorcycleRoutingOptions
import earth.maps.cardinal.routing.PedestrianRoutingOptions
import earth.maps.cardinal.routing.TruckRoutingOptions
import earth.maps.cardinal.viewmodel.ProfileEditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorScreen(
    navController: NavController,
    profileId: String? = null,
    viewModel: ProfileEditorViewModel = hiltViewModel()
) {
    val profileName by viewModel.profileName.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    val routingOptions by viewModel.routingOptions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isNewProfile by viewModel.isNewProfile.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(profileId) {
        viewModel.loadProfile(profileId)
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNewProfile) "Create Profile" else "Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
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
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
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
                    RoutingMode.AUTO -> AutoOptionsEditor(routingOptions as AutoRoutingOptions) { viewModel.updateRoutingOptions(it) }
                    RoutingMode.TRUCK -> TruckOptionsEditor(routingOptions as TruckRoutingOptions) { viewModel.updateRoutingOptions(it) }
                    RoutingMode.MOTOR_SCOOTER -> MotorScooterOptionsEditor(routingOptions as MotorScooterRoutingOptions) { viewModel.updateRoutingOptions(it) }
                    RoutingMode.MOTORCYCLE -> MotorcycleOptionsEditor(routingOptions as MotorcycleRoutingOptions) { viewModel.updateRoutingOptions(it) }
                    RoutingMode.BICYCLE -> CyclingOptionsEditor(routingOptions as CyclingRoutingOptions) { viewModel.updateRoutingOptions(it) }
                    RoutingMode.PEDESTRIAN -> PedestrianOptionsEditor(routingOptions as PedestrianRoutingOptions) { viewModel.updateRoutingOptions(it) }
                }
            }
        }
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
            RoutingMode.values().forEach { mode ->
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
private fun AutoOptionsEditor(
    options: AutoRoutingOptions,
    onOptionsChanged: (AutoRoutingOptions) -> Unit
) {
    OptionsSection("Route Preferences") {
        SliderOption("Use Highways", options.useHighways, 0f..1f) { value ->
            onOptionsChanged(options.copy(useHighways = value))
        }
        SliderOption("Use Tolls", options.useTolls, 0f..1f) { value ->
            onOptionsChanged(options.copy(useTolls = value))
        }
        SliderOption("Use Living Streets", options.useLivingStreets, 0f..1f) { value ->
            onOptionsChanged(options.copy(useLivingStreets = value))
        }
        SliderOption("Use Tracks", options.useTracks, 0f..1f) { value ->
            onOptionsChanged(options.copy(useTracks = value))
        }
        BooleanOption("Avoid Unpaved", options.excludeUnpaved) { value ->
            onOptionsChanged(options.copy(excludeUnpaved = value))
        }
        BooleanOption("Avoid Cash-only Tolls", options.excludeCashOnlyTolls) { value ->
            onOptionsChanged(options.copy(excludeCashOnlyTolls = value))
        }
    }

    OptionsSection("Penalties") {
        NumberOption("Maneuver Penalty", options.maneuverPenalty) { value ->
            onOptionsChanged(options.copy(maneuverPenalty = value))
        }
        NumberOption("Gate Cost", options.gateCost) { value ->
            onOptionsChanged(options.copy(gateCost = value))
        }
        NumberOption("Private Access Penalty", options.privateAccessPenalty) { value ->
            onOptionsChanged(options.copy(privateAccessPenalty = value))
        }
    }

    OptionsSection("Restrictions") {
        BooleanOption("Ignore Closures", options.ignoreClosures) { value ->
            onOptionsChanged(options.copy(ignoreClosures = value))
        }
        BooleanOption("Ignore Restrictions", options.ignoreRestrictions) { value ->
            onOptionsChanged(options.copy(ignoreRestrictions = value))
        }
        BooleanOption("Ignore One-ways", options.ignoreOneWays) { value ->
            onOptionsChanged(options.copy(ignoreOneWays = value))
        }
        BooleanOption("Ignore Access", options.ignoreAccess) { value ->
            onOptionsChanged(options.copy(ignoreAccess = value))
        }
    }
}

@Composable
private fun TruckOptionsEditor(
    options: TruckRoutingOptions,
    onOptionsChanged: (TruckRoutingOptions) -> Unit
) {
    OptionsSection("Route Preferences") {
        SliderOption("Use Highways", options.useHighways, 0f..1f) { value ->
            onOptionsChanged(options.copy(useHighways = value))
        }
        SliderOption("Use Tolls", options.useTolls, 0f..1f) { value ->
            onOptionsChanged(options.copy(useTolls = value))
        }
        SliderOption("Use Living Streets", options.useLivingStreets, 0f..1f) { value ->
            onOptionsChanged(options.copy(useLivingStreets = value))
        }
        SliderOption("Use Tracks", options.useTracks, 0f..1f) { value ->
            onOptionsChanged(options.copy(useTracks = value))
        }
        BooleanOption("Avoid Unpaved", options.excludeUnpaved) { value ->
            onOptionsChanged(options.copy(excludeUnpaved = value))
        }
        BooleanOption("Avoid Cash-only Tolls", options.excludeCashOnlyTolls) { value ->
            onOptionsChanged(options.copy(excludeCashOnlyTolls = value))
        }
    }

    OptionsSection("Penalties") {
        NumberOption("Maneuver Penalty", options.maneuverPenalty) { value ->
            onOptionsChanged(options.copy(maneuverPenalty = value))
        }
        NumberOption("Gate Cost", options.gateCost) { value ->
            onOptionsChanged(options.copy(gateCost = value))
        }
        NumberOption("Private Access Penalty", options.privateAccessPenalty) { value ->
            onOptionsChanged(options.copy(privateAccessPenalty = value))
        }
    }

    OptionsSection("Restrictions") {
        BooleanOption("Ignore Closures", options.ignoreClosures) { value ->
            onOptionsChanged(options.copy(ignoreClosures = value))
        }
        BooleanOption("Ignore Restrictions", options.ignoreRestrictions) { value ->
            onOptionsChanged(options.copy(ignoreRestrictions = value))
        }
        BooleanOption("Ignore One-ways", options.ignoreOneWays) { value ->
            onOptionsChanged(options.copy(ignoreOneWays = value))
        }
        BooleanOption("Ignore Access", options.ignoreAccess) { value ->
            onOptionsChanged(options.copy(ignoreAccess = value))
        }
    }

    OptionsSection("Vehicle Dimensions") {
        NumberOption("Length (m)", options.length) { value ->
            onOptionsChanged(options.copy(length = value))
        }
        NumberOption("Width (m)", options.width) { value ->
            onOptionsChanged(options.copy(width = value))
        }
        NumberOption("Height (m)", options.height) { value ->
            onOptionsChanged(options.copy(height = value))
        }
        NumberOption("Weight (tons)", options.weight) { value ->
            onOptionsChanged(options.copy(weight = value))
        }
    }

    OptionsSection("Truck Restrictions") {
        BooleanOption("Hazmat", options.hazmat) { value ->
            onOptionsChanged(options.copy(hazmat = value))
        }
        NumberOption("Axle Count", options.axleCount?.toDouble()) { value ->
            onOptionsChanged(options.copy(axleCount = value?.toInt()))
        }
        SliderOption("Use Truck Route", options.useTruckRoute, 0f..1f) { value ->
            onOptionsChanged(options.copy(useTruckRoute = value))
        }
    }
}

@Composable
private fun MotorScooterOptionsEditor(
    options: MotorScooterRoutingOptions,
    onOptionsChanged: (MotorScooterRoutingOptions) -> Unit
) {
    OptionsSection("Route Preferences") {
        SliderOption("Use Highways", options.useHighways, 0f..1f) { value ->
            onOptionsChanged(options.copy(useHighways = value))
        }
        SliderOption("Use Tolls", options.useTolls, 0f..1f) { value ->
            onOptionsChanged(options.copy(useTolls = value))
        }
        SliderOption("Use Living Streets", options.useLivingStreets, 0f..1f) { value ->
            onOptionsChanged(options.copy(useLivingStreets = value))
        }
        SliderOption("Use Tracks", options.useTracks, 0f..1f) { value ->
            onOptionsChanged(options.copy(useTracks = value))
        }
        BooleanOption("Avoid Unpaved", options.excludeUnpaved) { value ->
            onOptionsChanged(options.copy(excludeUnpaved = value))
        }
        BooleanOption("Avoid Cash-only Tolls", options.excludeCashOnlyTolls) { value ->
            onOptionsChanged(options.copy(excludeCashOnlyTolls = value))
        }
    }

    OptionsSection("Penalties") {
        NumberOption("Maneuver Penalty", options.maneuverPenalty) { value ->
            onOptionsChanged(options.copy(maneuverPenalty = value))
        }
        NumberOption("Gate Cost", options.gateCost) { value ->
            onOptionsChanged(options.copy(gateCost = value))
        }
        NumberOption("Private Access Penalty", options.privateAccessPenalty) { value ->
            onOptionsChanged(options.copy(privateAccessPenalty = value))
        }
    }

    OptionsSection("Restrictions") {
        BooleanOption("Ignore Closures", options.ignoreClosures) { value ->
            onOptionsChanged(options.copy(ignoreClosures = value))
        }
        BooleanOption("Ignore Restrictions", options.ignoreRestrictions) { value ->
            onOptionsChanged(options.copy(ignoreRestrictions = value))
        }
        BooleanOption("Ignore One-ways", options.ignoreOneWays) { value ->
            onOptionsChanged(options.copy(ignoreOneWays = value))
        }
        BooleanOption("Ignore Access", options.ignoreAccess) { value ->
            onOptionsChanged(options.copy(ignoreAccess = value))
        }
    }

    OptionsSection("Motor Scooter Preferences") {
        SliderOption("Use Primary Roads", options.usePrimary, 0f..1f) { value ->
            onOptionsChanged(options.copy(usePrimary = value))
        }
        SliderOption("Use Hills", options.useHills, 0f..1f) { value ->
            onOptionsChanged(options.copy(useHills = value))
        }
    }
}

@Composable
private fun MotorcycleOptionsEditor(
    options: MotorcycleRoutingOptions,
    onOptionsChanged: (MotorcycleRoutingOptions) -> Unit
) {
    OptionsSection("Route Preferences") {
        SliderOption("Use Highways", options.useHighways, 0f..1f) { value ->
            onOptionsChanged(options.copy(useHighways = value))
        }
        SliderOption("Use Tolls", options.useTolls, 0f..1f) { value ->
            onOptionsChanged(options.copy(useTolls = value))
        }
        SliderOption("Use Living Streets", options.useLivingStreets, 0f..1f) { value ->
            onOptionsChanged(options.copy(useLivingStreets = value))
        }
        SliderOption("Use Tracks", options.useTracks, 0f..1f) { value ->
            onOptionsChanged(options.copy(useTracks = value))
        }
        BooleanOption("Avoid Unpaved", options.excludeUnpaved) { value ->
            onOptionsChanged(options.copy(excludeUnpaved = value))
        }
        BooleanOption("Avoid Cash-only Tolls", options.excludeCashOnlyTolls) { value ->
            onOptionsChanged(options.copy(excludeCashOnlyTolls = value))
        }
    }

    OptionsSection("Penalties") {
        NumberOption("Maneuver Penalty", options.maneuverPenalty) { value ->
            onOptionsChanged(options.copy(maneuverPenalty = value))
        }
        NumberOption("Gate Cost", options.gateCost) { value ->
            onOptionsChanged(options.copy(gateCost = value))
        }
        NumberOption("Private Access Penalty", options.privateAccessPenalty) { value ->
            onOptionsChanged(options.copy(privateAccessPenalty = value))
        }
    }

    OptionsSection("Restrictions") {
        BooleanOption("Ignore Closures", options.ignoreClosures) { value ->
            onOptionsChanged(options.copy(ignoreClosures = value))
        }
        BooleanOption("Ignore Restrictions", options.ignoreRestrictions) { value ->
            onOptionsChanged(options.copy(ignoreRestrictions = value))
        }
        BooleanOption("Ignore One-ways", options.ignoreOneWays) { value ->
            onOptionsChanged(options.copy(ignoreOneWays = value))
        }
        BooleanOption("Ignore Access", options.ignoreAccess) { value ->
            onOptionsChanged(options.copy(ignoreAccess = value))
        }
    }

    OptionsSection("Motorcycle Preferences") {
        SliderOption("Use Trails", options.useTrails, 0f..1f) { value ->
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
        Text("Type: ${options.bicycleType ?: "road"}")
    }

    OptionsSection("Route Preferences") {
        NumberOption("Cycling Speed (km/h)", options.cyclingSpeed) { value ->
            onOptionsChanged(options.copy(cyclingSpeed = value))
        }
        SliderOption("Use Roads", options.useRoads, 0f..1f) { value ->
            onOptionsChanged(options.copy(useRoads = value))
        }
        SliderOption("Use Hills", options.useHills, 0f..1f) { value ->
            onOptionsChanged(options.copy(useHills = value))
        }
        SliderOption("Avoid Bad Surfaces", options.avoidBadSurfaces, 0f..1f) { value ->
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
        NumberOption("Walking Speed (km/h)", options.walkingSpeed) { value ->
            onOptionsChanged(options.copy(walkingSpeed = value))
        }
        Text("Type: ${options.type ?: "foot"}")
    }

    OptionsSection("Path Preferences") {
        SliderOption("Walkway Factor", options.walkwayFactor, 0f..1f) { value ->
            onOptionsChanged(options.copy(walkwayFactor = value))
        }
        SliderOption("Sidewalk Factor", options.sidewalkFactor, 0f..1f) { value ->
            onOptionsChanged(options.copy(sidewalkFactor = value))
        }
        SliderOption("Use Lit Paths", options.useLit, 0f..1f) { value ->
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
    onValueChanged: (Double?) -> Unit
) {
    if (value != null) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$label: ${value.format(2)}")
                TextButton(onClick = { onValueChanged(null) }) {
                    Text("Reset")
                }
            }
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChanged(it.toDouble()) },
                valueRange = range
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

private fun Double.format(digits: Int) = "%.${digits}f".format(this)
