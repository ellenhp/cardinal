package earth.maps.cardinal.routing

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName

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
    val privateAccessPenalty: Double?
    val useHighways: Double?
    val useTolls: Double?
    val useLivingStreets: Double?
    val useTracks: Double?
    val excludeUnpaved: Boolean?
    val excludeCashOnlyTolls: Boolean?
    val ignoreClosures: Boolean?
    val ignoreRestrictions: Boolean?
    val ignoreOneWays: Boolean?
    val ignoreAccess: Boolean?
}

/**
 * Routing options for automobile mode.
 */
data class AutoRoutingOptions(
    override val costingType: String = "auto",

    // Maneuver and access penalties
    override val maneuverPenalty: Double? = null,
    override val gateCost: Double? = null,
    override val privateAccessPenalty: Double? = null,

    // Road type preferences (0-1 range)
    override val useHighways: Double? = null,
    override val useTolls: Double? = null,
    override val useLivingStreets: Double? = null,
    override val useTracks: Double? = null,

    // Restriction options
    override val ignoreClosures: Boolean? = null,
    override val ignoreRestrictions: Boolean? = null,
    override val ignoreOneWays: Boolean? = null,
    override val ignoreAccess: Boolean? = null,

    // HOV options
    override val excludeUnpaved: Boolean? = null,
    override val excludeCashOnlyTolls: Boolean? = null
) : RoutingOptions(), AutoOptions

/**
 * Routing options for truck mode (extends auto with truck-specific parameters).
 */
data class TruckRoutingOptions(
    override val costingType: String = "truck",

    // Basic auto options
    override val maneuverPenalty: Double? = null,
    override val gateCost: Double? = null,
    override val privateAccessPenalty: Double? = null,
    override val useHighways: Double? = null,
    override val useTolls: Double? = null,
    override val useLivingStreets: Double? = null,
    override val useTracks: Double? = null,
    override val ignoreClosures: Boolean? = null,
    override val ignoreRestrictions: Boolean? = null,
    override val ignoreOneWays: Boolean? = null,
    override val ignoreAccess: Boolean? = null,
    override val excludeUnpaved: Boolean? = null,
    override val excludeCashOnlyTolls: Boolean? = null,

    // Truck-specific options
    val length: Double? = null, // meters
    val width: Double? = null,
    val height: Double? = null,
    val weight: Double? = null, // metric tons
    val axleCount: Int? = null,
    val hazmat: Boolean? = null,
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
    override val privateAccessPenalty: Double? = null,
    override val useHighways: Double? = null,
    override val useTolls: Double? = null,
    override val useLivingStreets: Double? = null,
    override val useTracks: Double? = null,
    override val ignoreClosures: Boolean? = null,
    override val ignoreRestrictions: Boolean? = null,
    override val ignoreOneWays: Boolean? = null,
    override val ignoreAccess: Boolean? = null,
    override val excludeUnpaved: Boolean? = null,
    override val excludeCashOnlyTolls: Boolean? = null,

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
    override val privateAccessPenalty: Double? = null,
    override val useHighways: Double? = null,
    override val useTolls: Double? = null,
    override val useLivingStreets: Double? = null,
    override val useTracks: Double? = null,
    override val ignoreClosures: Boolean? = null,
    override val ignoreRestrictions: Boolean? = null,
    override val ignoreOneWays: Boolean? = null,
    override val ignoreAccess: Boolean? = null,
    override val excludeUnpaved: Boolean? = null,
    override val excludeCashOnlyTolls: Boolean? = null,

    // Motorcycle specific
    val useTrails: Double? = null
) : RoutingOptions(), AutoOptions

/**
 * Routing options for cycling mode.
 */
data class CyclingRoutingOptions(
    override val costingType: String = "bicycle",

    // Bicycle type
    @SerializedName("bicycle_type")
    val bicycleType: BicycleType? = null,

    // Speed and fitness
    val cyclingSpeed: Double? = null, // km/h

    // Road preferences (0-1 range)
    val useRoads: Double? = null,
    val useHills: Double? = null,

    // Surface preferences
    val avoidBadSurfaces: Double? = null
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

    // Road preferences (0-1 range)
    val useLit: Double? = null,

    // Accessibility options
    val type: PedestrianType? = null
) : RoutingOptions()
