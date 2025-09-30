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
