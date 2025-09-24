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
import org.woheller69.AndroidAddressFormatter.AndroidAddressFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddressFormatter @Inject constructor() {
    private val formatter = AndroidAddressFormatter(true, true, false)

    fun format(address: Address): String? {
        try {
            val locale = Locale.getDefault()
            val fallbackCountryCode = locale.country.uppercase()
            return formatter.format(
                Gson().toJson(
                    mapOf(
                        "house_number" to address.houseNumber,
                        "road" to address.road,
                        "city" to address.city,
                        "state" to address.state,
                        "postcode" to address.postcode,
                        "country" to address.country,
                        "country_code" to address.countryCode,
                    ).filterValues { it != null }),
                fallbackCountryCode
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to format address", e)
            return null
        }
    }

    companion object {
        const val TAG = "AddressFormatter"
    }
}
