package earth.maps.cardinal.data

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import earth.maps.cardinal.routing.AutoRoutingOptions
import earth.maps.cardinal.routing.CyclingRoutingOptions
import earth.maps.cardinal.routing.MotorScooterRoutingOptions
import earth.maps.cardinal.routing.MotorcycleRoutingOptions
import earth.maps.cardinal.routing.PedestrianRoutingOptions
import earth.maps.cardinal.routing.RoutingOptions
import earth.maps.cardinal.routing.TruckRoutingOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutingProfileRepository @Inject constructor(
    private val database: AppDatabase
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val gson = Gson()
    private val dao = database.routingProfileDao()

    private val _allProfiles = MutableStateFlow<List<RoutingProfile>>(emptyList())
    val allProfiles: StateFlow<List<RoutingProfile>> = _allProfiles.asStateFlow()

    init {
        viewModelScope.launch {
            dao.getAllProfiles().collect { profiles ->
                _allProfiles.value = profiles
            }
        }
    }

    /**
     * Creates a new routing profile with the given options.
     */
    suspend fun createProfile(
        name: String,
        routingMode: RoutingMode,
        options: RoutingOptions,
        isDefault: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val optionsJson = gson.toJson(options)
            val profile = RoutingProfile(
                name = name,
                routingMode = routingMode.value,
                optionsJson = optionsJson,
                isDefault = isDefault
            )

            if (isDefault) {
                // Clear existing default for this mode
                dao.clearDefaultForMode(routingMode.value)
            }

            val profileId = dao.insert(profile)
            Result.success(profile.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates an existing routing profile.
     */
    suspend fun updateProfile(
        profileId: String,
        name: String? = null,
        options: RoutingOptions? = null,
        isDefault: Boolean? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existingProfile = dao.getProfileById(profileId)
                ?: return@withContext Result.failure(IllegalArgumentException("Profile not found"))

            val updatedProfile = existingProfile.copy(
                name = name ?: existingProfile.name,
                optionsJson = options?.let { gson.toJson(it) } ?: existingProfile.optionsJson,
                isDefault = isDefault ?: existingProfile.isDefault,
                updatedAt = System.currentTimeMillis()
            )

            if (isDefault == true) {
                // Clear existing default for this mode
                dao.clearDefaultForMode(existingProfile.routingMode)
            }

            dao.update(updatedProfile)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a routing profile.
     */
    suspend fun deleteProfile(profileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.deleteById(profileId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets a routing profile by ID.
     */
    suspend fun getProfileById(profileId: String): Result<RoutingProfile?> = withContext(Dispatchers.IO) {
        try {
            val profile = dao.getProfileById(profileId)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets a routing profile by ID and deserializes its options.
     */
    suspend fun getProfileWithOptions(profileId: String): Result<Pair<RoutingProfile, RoutingOptions>?> = withContext(Dispatchers.IO) {
        try {
            val profile = dao.getProfileById(profileId)
                ?: return@withContext Result.success(null)

            val options = deserializeOptions(profile.routingMode, profile.optionsJson)
            Result.success(Pair(profile, options))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets all profiles for a specific routing mode.
     */
    fun getProfilesForMode(routingMode: RoutingMode): Flow<List<RoutingProfile>> {
        return dao.getProfilesByMode(routingMode.value)
    }

    /**
     * Gets the default profile for a routing mode, or creates one if none exists.
     */
    suspend fun getOrCreateDefaultProfile(routingMode: RoutingMode): Result<Pair<RoutingProfile, RoutingOptions>> = withContext(Dispatchers.IO) {
        try {
            val existingDefault = dao.getDefaultProfileForMode(routingMode.value)
            if (existingDefault != null) {
                val options = deserializeOptions(existingDefault.routingMode, existingDefault.optionsJson)
                return@withContext Result.success(Pair(existingDefault, options))
            }

            // Create default profile
            val defaultOptions = createDefaultOptionsForMode(routingMode)
            val profileName = "Default ${routingMode.label}"

            createProfile(profileName, routingMode, defaultOptions, isDefault = true).fold(
                onSuccess = { profileId ->
                    val profile = dao.getProfileById(profileId)!!
                    Result.success(Pair(profile, defaultOptions))
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sets a profile as the default for its routing mode.
     */
    suspend fun setDefaultProfile(profileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val profile = dao.getProfileById(profileId)
                ?: return@withContext Result.failure(IllegalArgumentException("Profile not found"))

            dao.clearDefaultForMode(profile.routingMode)
            dao.setDefaultProfile(profileId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates default routing options for a given mode.
     */
    fun createDefaultOptionsForMode(routingMode: RoutingMode): RoutingOptions {
        return when (routingMode) {
            RoutingMode.AUTO -> AutoRoutingOptions()
            RoutingMode.TRUCK -> TruckRoutingOptions()
            RoutingMode.MOTOR_SCOOTER -> MotorScooterRoutingOptions()
            RoutingMode.MOTORCYCLE -> MotorcycleRoutingOptions()
            RoutingMode.BICYCLE -> CyclingRoutingOptions()
            RoutingMode.PEDESTRIAN -> PedestrianRoutingOptions()
        }
    }

    /**
     * Deserializes routing options from JSON based on the routing mode.
     */
    fun deserializeOptions(routingMode: String, optionsJson: String): RoutingOptions {
        return try {
            when (routingMode) {
                "auto" -> gson.fromJson(optionsJson, AutoRoutingOptions::class.java)
                "truck" -> gson.fromJson(optionsJson, TruckRoutingOptions::class.java)
                "motor_scooter" -> gson.fromJson(optionsJson, MotorScooterRoutingOptions::class.java)
                "motorcycle" -> gson.fromJson(optionsJson, MotorcycleRoutingOptions::class.java)
                "bicycle" -> gson.fromJson(optionsJson, CyclingRoutingOptions::class.java)
                "pedestrian" -> gson.fromJson(optionsJson, PedestrianRoutingOptions::class.java)
                else -> throw IllegalArgumentException("Unknown routing mode: $routingMode")
            }
        } catch (e: JsonSyntaxException) {
            // If deserialization fails, return default options for the mode
            val mode = RoutingMode.values().find { it.value == routingMode }
                ?: throw IllegalArgumentException("Unknown routing mode: $routingMode")
            createDefaultOptionsForMode(mode)
        }
    }

    /**
     * Gets the count of profiles for a specific mode.
     */
    suspend fun getProfileCountForMode(routingMode: RoutingMode): Int = withContext(Dispatchers.IO) {
        dao.getProfileCountForMode(routingMode.value)
    }
}
