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

package earth.maps.cardinal.data

import android.util.Log
import com.google.gson.Gson
import earth.maps.cardinal.ui.generatePlaceId
import org.woheller69.AndroidAddressFormatter.AndroidAddressFormatter
import java.util.Locale

data class GeocodeResult(
    val latitude: Double,
    val longitude: Double,
    val displayName: String,
    val address: Address? = null
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
    fun format(): String? {
        try {
            val locale = Locale.getDefault()
            val fallbackCountryCode = locale.country.uppercase()
            return AndroidAddressFormatter(true, true, false).format(
                Gson().toJson(
                    mapOf(
                        "house_number" to houseNumber,
                        "road" to road,
                        "city" to city,
                        "state" to state,
                        "postcode" to postcode,
                        "country" to country,
                        "country_code" to countryCode,
                    ).filterValues { it != null }),
                fallbackCountryCode
            )
        } catch (e: Throwable) {
            // I'd really like to catch something more specific here but the library throws all sorts of different kinds of errors.
            Log.e(TAG, "Failed to format address", e)
            return null
        }
    }

    companion object {
        const val TAG = "Address"
    }
}

fun deduplicateSearchResults(results: List<GeocodeResult>): List<GeocodeResult> {
    return results.distinctBy { generatePlaceId(it) }
}
