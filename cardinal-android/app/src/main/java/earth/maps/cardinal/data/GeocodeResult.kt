package earth.maps.cardinal.data

import android.util.Log
import com.google.gson.Gson
import org.woheller69.AndroidAddressFormatter.AndroidAddressFormatter

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
            // Country code can be null in cases where a feature's centroid falls in e.g. the ocean, and the lib will throw.
            return countryCode?.let { countryCode ->
                AndroidAddressFormatter(true, true, false).format(
                    Gson().toJson(
                        mapOf(
                            "house_number" to houseNumber,
                            "road" to road,
                            "city" to city,
                            "state" to state,
                            "postcode" to postcode,
                            "country" to country
                        ).filterValues { it != null }),
                    countryCode
                )
            }
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
