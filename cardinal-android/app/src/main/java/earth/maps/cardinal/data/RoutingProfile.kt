package earth.maps.cardinal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Entity representing a user-defined routing profile.
 * Contains a routing mode and associated options stored as JSON.
 */
@Entity(tableName = "routing_profiles")
data class RoutingProfile(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val routingMode: String, // Separate column for easy querying
    val optionsJson: String, // JSON serialized routing options
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
