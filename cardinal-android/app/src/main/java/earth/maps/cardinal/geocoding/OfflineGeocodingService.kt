package earth.maps.cardinal.geocoding

import android.content.Context
import earth.maps.cardinal.data.Address
import earth.maps.cardinal.data.GeocodeResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import uniffi.cardinal_geocoder.newAirmailIndex
import java.io.File

class OfflineGeocodingService(private val context: Context) : GeocodingService, TileProcessor {
    private val geocoderDir = File(context.filesDir, "geocoder").apply { mkdirs() }
    private val airmailIndex = newAirmailIndex("en", geocoderDir.absolutePath)

    override suspend fun geocode(query: String): Flow<List<GeocodeResult>> = flow {
        try {
            val results = airmailIndex.searchPhrase(query)
            val geocodeResults = results.map { poi ->
                val tagMap: HashMap<String, String> = HashMap(poi.tags.size)
                for (tag in poi.tags) {
                    tagMap[tag.key] = tag.value
                }

                // Get display name from name tag or create from address components
                val displayName = tagMap["name"] ?: buildAddressString(tagMap)

                // Populate address from available tags
                val address = buildAddress(tagMap)

                GeocodeResult(
                    displayName = displayName,
                    latitude = poi.lat,
                    longitude = poi.lng,
                    address = address
                )
            }
            emit(geocodeResults)
        } catch (_: Exception) {
            // If there's an error, return empty list
            emit(emptyList())
        }
    }

    override suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double
    ): Flow<List<GeocodeResult>> = flow {
        emit(emptyList())
    }

    override suspend fun beginTileProcessing() {
        airmailIndex.beginIngestion()
    }

    override suspend fun endTileProcessing() {
        airmailIndex.commitIngestion()
    }

    override suspend fun processTile(tileData: ByteArray, zoom: Int, x: Int, y: Int) {
        if (zoom != 14) {
            return
        }
        try {
            // Ingest the tile into the geocoder index
            airmailIndex.ingestTile(tileData)
        } catch (e: Exception) {
            // Log the error but don't throw as this shouldn't break the tile download process
            android.util.Log.e("OfflineGeocodingService", "Error processing tile $zoom/$x/$y", e)
        }
    }

    private fun buildAddressString(tags: Map<String, String>): String {
        val houseNumber = tags["addr:housenumber"]
        val road = tags["addr:street"]
        val city = tags["addr:city"]

        return when {
            houseNumber != null && road != null -> "$houseNumber $road"
            road != null -> road
            city != null -> city
            else -> "Unnamed Location"
        }
    }

    private fun buildAddress(tags: Map<String, String>): Address {
        return Address(
            houseNumber = tags["addr:housenumber"],
            road = tags["addr:street"],
            city = tags["addr:city"],
            state = tags["addr:state"],
            postcode = tags["addr:postcode"],
            country = tags["addr:country"]
        )
    }
}
