/*
 *     Cardinal Maps
 *     Copyright (C) 2025 Cardinal Maps Authors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package earth.maps.cardinal.data

import earth.maps.cardinal.ui.home.generatePlaceId

data class GeocodeResult(
    val latitude: Double,
    val longitude: Double,
    val displayName: String,
    val properties: Map<String, String>,
    val address: Address? = null,
)

data class Address(
    val houseNumber: String? = null,
    val road: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postcode: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
) {
    companion object {
        const val TAG = "Address"
    }
}

fun Address.format(formatter: AddressFormatter): String? {
    return formatter.format(this)
}

fun deduplicateSearchResults(results: List<GeocodeResult>): List<GeocodeResult> {
    return results.distinctBy { generatePlaceId(it) }
}
