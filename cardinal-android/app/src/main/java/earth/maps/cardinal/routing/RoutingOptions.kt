package earth.maps.cardinal.routing

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder

/**
 * Base class for routing options that can be serialized to JSON for Valhalla API.
 */
abstract class RoutingOptions {
    abstract val costingType: String

    /**
     * Convert this options object to JSON string for Valhalla API.
     */
    fun toValhallaOptionsJson(): String {
        val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
        val wrapper = object {
            val costing_options = mapOf(costingType to this@RoutingOptions)
        }
        return gson.toJson(wrapper)
    }
}

/**
 * Interface for routing options that include automobile-specific parameters.
 */
interface AutoOptions {
    val maneuverPenalty: Double?
    val gateCost: Double?
    val gatePenalty: Double?
    val privateAccessPenalty: Double?
    val destinationOnlyPenalty: Double?
    val tollBoothCost: Double?
    val tollBoothPenalty: Double?
    val ferryCost: Double?
    val useFerry: Double?
    val useHighways: Double?
    val useTolls: Double?
    val useLivingStreets: Double?
    val useTracks: Double?
    val servicePenalty: Double?
    val serviceFactor: Double?
    val countryCrossingCost: Double?
    val countryCrossingPenalty: Double?
    val shortest: Boolean?
    val useDistance: Double?
    val disableHierarchyPruning: Boolean?
    val topSpeed: Double?
    val fixedSpeed: Double?
    val ignoreClosures: Boolean?
    val closureFactor: Double?
    val ignoreRestrictions: Boolean?
    val ignoreOneWays: Boolean?
    val ignoreNonVehicularRestrictions: Boolean?
    val ignoreAccess: Boolean?
    val ignoreConstruction: Boolean?
    val height: Double?
    val width: Double?
    val excludeUnpaved: Boolean?
    val excludeCashOnlyTolls: Boolean?
    val includeHov2: Boolean?
    val includeHov3: Boolean?
    val includeHot: Boolean?
}

/**
 * Routing options for automobile mode.
 */
data class AutoRoutingOptions(
    override val costingType: String = "auto",

    // Maneuver and access penalties
    override val maneuverPenalty: Double? = null,
    override val gateCost: Double? = null,
    override val gatePenalty: Double? = null,
    override val privateAccessPenalty: Double? = null,
    override val destinationOnlyPenalty: Double? = null,

    // Toll and ferry preferences
    override val tollBoothCost: Double? = null,
    override val tollBoothPenalty: Double? = null,
    override val ferryCost: Double? = null,
    override val useFerry: Double? = null,

    // Road type preferences (0-1 range)
    override val useHighways: Double? = null,
    override val useTolls: Double? = null,
    override val useLivingStreets: Double? = null,
    override val useTracks: Double? = null,

    // Service road preferences
    override val servicePenalty: Double? = null,
    override val serviceFactor: Double? = null,

    // Country crossing
    override val countryCrossingCost: Double? = null,
    override val countryCrossingPenalty: Double? = null,

    // Algorithm options
    override val shortest: Boolean? = null,
    override val useDistance: Double? = null,
    override val disableHierarchyPruning: Boolean? = null,

    // Speed options
    override val topSpeed: Double? = null,
    override val fixedSpeed: Double? = null,

    // Restriction options
    override val ignoreClosures: Boolean? = null,
    override val closureFactor: Double? = null,
    override val ignoreRestrictions: Boolean? = null,
    override val ignoreOneWays: Boolean? = null,
    override val ignoreNonVehicularRestrictions: Boolean? = null,
    override val ignoreAccess: Boolean? = null,
    override val ignoreConstruction: Boolean? = null,

    // Vehicle dimensions
    override val height: Double? = null,
    override val width: Double? = null,

    // HOV options
    override val excludeUnpaved: Boolean? = null,
    override val excludeCashOnlyTolls: Boolean? = null,
    override val includeHov2: Boolean? = null,
    override val includeHov3: Boolean? = null,
    override val includeHot: Boolean? = null
) : RoutingOptions(), AutoOptions

/**
 * Routing options for truck mode (extends auto with truck-specific parameters).
 */
data class TruckRoutingOptions(
    override val costingType: String = "truck",

    // Basic auto options
    override val maneuverPenalty: Double? = null,
    override val gateCost: Double? = null,
    override val gatePenalty: Double? = null,
    override val privateAccessPenalty: Double? = null,
    override val destinationOnlyPenalty: Double? = null,
    override val tollBoothCost: Double? = null,
    override val tollBoothPenalty: Double? = null,
    override val ferryCost: Double? = null,
    override val useFerry: Double? = null,
    override val useHighways: Double? = null,
    override val useTolls: Double? = null,
    override val useLivingStreets: Double? = null,
    override val useTracks: Double? = null,
    override val servicePenalty: Double? = null,
    override val serviceFactor: Double? = null,
    override val countryCrossingCost: Double? = null,
    override val countryCrossingPenalty: Double? = null,
    override val shortest: Boolean? = null,
    override val useDistance: Double? = null,
    override val disableHierarchyPruning: Boolean? = null,
    override val topSpeed: Double? = null,
    override val fixedSpeed: Double? = null,
    override val ignoreClosures: Boolean? = null,
    override val closureFactor: Double? = null,
    override val ignoreRestrictions: Boolean? = null,
    override val ignoreOneWays: Boolean? = null,
    override val ignoreNonVehicularRestrictions: Boolean? = null,
    override val ignoreAccess: Boolean? = null,
    override val ignoreConstruction: Boolean? = null,
    override val height: Double? = null,
    override val width: Double? = null,
    override val excludeUnpaved: Boolean? = null,
    override val excludeCashOnlyTolls: Boolean? = null,
    override val includeHov2: Boolean? = null,
    override val includeHov3: Boolean? = null,
    override val includeHot: Boolean? = null,

    // Truck-specific options
    val length: Double? = null, // meters
    val weight: Double? = null, // metric tons
    val axleLoad: Double? = null, // metric tons
    val axleCount: Int? = null,
    val hazmat: Boolean? = null,
    val hgvNoAccessPenalty: Double? = null,
    val lowClassPenalty: Double? = null,
    val useTruckRoute: Double? = null // 0-1 range
) : RoutingOptions(), AutoOptions

/**
 * Routing options for motor scooter mode.
 */
data class MotorScooterRoutingOptions(
    override val costingType: String = "motor_scooter",

    // Basic auto options
    override val maneuverPenalty: Double? = null,
    override val gateCost: Double? = null,
    override val gatePenalty: Double? = null,
    override val privateAccessPenalty: Double? = null,
    override val destinationOnlyPenalty: Double? = null,
    override val tollBoothCost: Double? = null,
    override val tollBoothPenalty: Double? = null,
    override val ferryCost: Double? = null,
    override val useFerry: Double? = null,
    override val useHighways: Double? = null,
    override val useTolls: Double? = null,
    override val useLivingStreets: Double? = null,
    override val useTracks: Double? = null,
    override val servicePenalty: Double? = null,
    override val serviceFactor: Double? = null,
    override val countryCrossingCost: Double? = null,
    override val countryCrossingPenalty: Double? = null,
    override val shortest: Boolean? = null,
    override val useDistance: Double? = null,
    override val disableHierarchyPruning: Boolean? = null,
    override val topSpeed: Double? = null,
    override val fixedSpeed: Double? = null,
    override val ignoreClosures: Boolean? = null,
    override val closureFactor: Double? = null,
    override val ignoreRestrictions: Boolean? = null,
    override val ignoreOneWays: Boolean? = null,
    override val ignoreNonVehicularRestrictions: Boolean? = null,
    override val ignoreAccess: Boolean? = null,
    override val ignoreConstruction: Boolean? = null,
    override val height: Double? = null,
    override val width: Double? = null,
    override val excludeUnpaved: Boolean? = null,
    override val excludeCashOnlyTolls: Boolean? = null,
    override val includeHov2: Boolean? = null,
    override val includeHov3: Boolean? = null,
    override val includeHot: Boolean? = null,

    // Motor scooter specific
    val usePrimary: Double? = null,
    val useHills: Double? = null
) : RoutingOptions(), AutoOptions

/**
 * Routing options for motorcycle mode.
 */
data class MotorcycleRoutingOptions(
    override val costingType: String = "motorcycle",

    // Basic auto options
    override val maneuverPenalty: Double? = null,
    override val gateCost: Double? = null,
    override val gatePenalty: Double? = null,
    override val privateAccessPenalty: Double? = null,
    override val destinationOnlyPenalty: Double? = null,
    override val tollBoothCost: Double? = null,
    override val tollBoothPenalty: Double? = null,
    override val ferryCost: Double? = null,
    override val useFerry: Double? = null,
    override val useHighways: Double? = null,
    override val useTolls: Double? = null,
    override val useLivingStreets: Double? = null,
    override val useTracks: Double? = null,
    override val servicePenalty: Double? = null,
    override val serviceFactor: Double? = null,
    override val countryCrossingCost: Double? = null,
    override val countryCrossingPenalty: Double? = null,
    override val shortest: Boolean? = null,
    override val useDistance: Double? = null,
    override val disableHierarchyPruning: Boolean? = null,
    override val topSpeed: Double? = null,
    override val fixedSpeed: Double? = null,
    override val ignoreClosures: Boolean? = null,
    override val closureFactor: Double? = null,
    override val ignoreRestrictions: Boolean? = null,
    override val ignoreOneWays: Boolean? = null,
    override val ignoreNonVehicularRestrictions: Boolean? = null,
    override val ignoreAccess: Boolean? = null,
    override val ignoreConstruction: Boolean? = null,
    override val height: Double? = null,
    override val width: Double? = null,
    override val excludeUnpaved: Boolean? = null,
    override val excludeCashOnlyTolls: Boolean? = null,
    override val includeHov2: Boolean? = null,
    override val includeHov3: Boolean? = null,
    override val includeHot: Boolean? = null,

    // Motorcycle specific
    val useTrails: Double? = null
) : RoutingOptions(), AutoOptions

/**
 * Routing options for cycling mode.
 */
data class CyclingRoutingOptions(
    override val costingType: String = "bicycle",

    // Bicycle type
    val bicycleType: String? = null, // "road", "hybrid", "cross", "mountain"

    // Speed and fitness
    val cyclingSpeed: Double? = null, // km/h

    // Road preferences (0-1 range)
    val useRoads: Double? = null,
    val useHills: Double? = null,
    val useFerry: Double? = null,
    val useLivingStreets: Double? = null,

    // Surface preferences
    val avoidBadSurfaces: Double? = null,

    // Cost penalties (inherited from auto)
    val maneuverPenalty: Double? = null,
    val gateCost: Double? = null,
    val gatePenalty: Double? = null,
    val destinationOnlyPenalty: Double? = null,
    val servicePenalty: Double? = null,
    val countryCrossingCost: Double? = null,
    val countryCrossingPenalty: Double? = null,

    // Algorithm options
    val shortest: Boolean? = null,
    val disableHierarchyPruning: Boolean? = null
) : RoutingOptions()

/**
 * Routing options for pedestrian mode.
 */
data class PedestrianRoutingOptions(
    override val costingType: String = "pedestrian",

    // Walking speed
    val walkingSpeed: Double? = null, // km/h

    // Path preferences (factors)
    val walkwayFactor: Double? = null,
    val sidewalkFactor: Double? = null,
    val alleyFactor: Double? = null,
    val drivewayFactor: Double? = null,

    // Penalties
    val stepPenalty: Double? = null,
    val elevatorPenalty: Double? = null,

    // Road preferences (0-1 range)
    val useFerry: Double? = null,
    val useLivingStreets: Double? = null,
    val useTracks: Double? = null,
    val useHills: Double? = null,
    val useLit: Double? = null,

    // Service road preferences
    val servicePenalty: Double? = null,
    val serviceFactor: Double? = null,
    val destinationOnlyPenalty: Double? = null,

    // Hiking difficulty
    val maxHikingDifficulty: Int? = null,

    // Accessibility options
    val type: String? = null, // "foot", "wheelchair", "blind"

    // Distance limits
    val maxDistance: Double? = null,
    val transitStartEndMaxDistance: Double? = null,
    val transitTransferMaxDistance: Double? = null,

    // Algorithm options
    val shortest: Boolean? = null,
    val modeFactor: Double? = null
) : RoutingOptions()
