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

package earth.maps.cardinal.geocoding

import android.content.Context
import android.util.Log
import earth.maps.cardinal.R
import earth.maps.cardinal.data.Address
import earth.maps.cardinal.data.GeocodeResult
import earth.maps.cardinal.data.LatLng
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import uniffi.cardinal_geocoder.newAirmailIndex
import java.io.File

class OfflineGeocodingService(private val context: Context) : GeocodingService, TileProcessor {
    private val geocoderDir = File(context.filesDir, "geocoder").apply { mkdirs() }
    private val airmailIndex = newAirmailIndex("en", geocoderDir.absolutePath)

    override suspend fun geocode(query: String, focusPoint: LatLng?): Flow<List<GeocodeResult>> =
        flow {
            try {
                val results = airmailIndex.searchPhrase(query)
                val geocodeResults = results.map { poi ->
                    val tagMap: HashMap<String, String> = HashMap(poi.tags.size)
                    for (tag in poi.tags) {
                        tagMap[tag.key] = tag.value
                    }
                    buildResult(tagMap, poi.lat, poi.lng)
                }
                emit(geocodeResults)
            } catch (e: Exception) {
                Log.e(TAG, "Geocode failed with exception", e)
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

    override suspend fun nearby(latitude: Double, longitude: Double): Flow<List<GeocodeResult>> =
        flow {
            emit(emptyList())
        }

    override suspend fun beginTileProcessing() {
        Log.d(TAG, "Beginning tile processing")
        airmailIndex.beginIngestion()
    }

    override suspend fun endTileProcessing() {
        Log.d(TAG, "Ending tile processing")
        airmailIndex.commitIngestion()
    }

    override suspend fun processTile(tileData: ByteArray, zoom: Int, x: Int, y: Int) {
        if (zoom != 14) {
            return
        }
        try {
            // Ingest the tile into the geocoder index
            airmailIndex.ingestTileWithCoordinates(tileData, x.toUInt(), y.toUInt(), zoom.toUByte())
        } catch (e: Exception) {
            // Log the error but don't throw as this shouldn't break the tile download process
            Log.e(TAG, "Error processing tile $zoom/$x/$y", e)
        }
    }

    fun buildResult(tags: Map<String, String>, latitude: Double, longitude: Double): GeocodeResult {

        // Get display name from name tag or create from address components
        val displayName = tags["name"] ?: buildAddressString(tags)

        // Populate address from available tags
        val address = buildAddress(tags)

        return GeocodeResult(
            displayName = displayName,
            latitude = latitude,
            longitude = longitude,
            address = address,
            properties = tags,
        )
    }

    private fun buildAddressString(tags: Map<String, String>): String {
        val houseNumber = tags["addr:housenumber"] ?: tags["addr_housenumber"]
        val road = tags["addr:street"] ?: tags["addr_street"]
        val city = tags["addr:city"] ?: tags["addr_city"]

        return when {
            houseNumber != null && road != null -> "$houseNumber $road"
            road != null -> road
            city != null -> city
            else -> context.getString(R.string.unnamed_location)
        }
    }

    private fun buildAddress(tags: Map<String, String>): Address {
        return Address(
            houseNumber = tags["addr:housenumber"] ?: tags["addr_housenumber"],
            road = tags["addr:street"] ?: tags["addr_street"],
            city = tags["addr:city"] ?: tags["addr_city"],
            state = tags["addr:state"] ?: tags["addr_state"],
            postcode = tags["addr:postcode"] ?: tags["addr_postcode"],
            country = tags["addr:country"] ?: tags["addr_country"]
        )
    }

    companion object {
        const val TAG = "OfflineGeocodingService"
    }
}
