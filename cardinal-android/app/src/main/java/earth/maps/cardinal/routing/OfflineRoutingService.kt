package earth.maps.cardinal.routing

import ValhallaConfigBuilder
import android.content.Context
import android.util.Log
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.valhalla.valhalla.ValhallaActor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.io.File

class OfflineRoutingService(context: Context) : RoutingService {

    private val valhallaConfigPath =
        "${context.filesDir}/valhalla.json"
    private val valhallaActor = ValhallaActor(valhallaConfigPath)

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        val writer = File(valhallaConfigPath).writer(Charsets.UTF_8)
        writer.write(
            GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create().toJson(
                    ValhallaConfigBuilder().withTileDir("${context.filesDir}/valhalla_tiles")
                        .build()
                )
        )
        writer.close()
    }

    override suspend fun getRoute(
        request: String
    ): String {
        try {
            val valhallaResponse = coroutineScope.async {
                valhallaActor.route(
                    request
                )
            }.await()

            Log.d(TAG, valhallaResponse)

            return valhallaResponse
        } catch (e: Exception) {
            Log.e(TAG, "Failed to route", e)
            throw e // Re-throw because the clients catch these and show them to the user, real handy for a pre-prod app like this.
        }
    }

    companion object {
        const val TAG = "OfflineRoutingService"
    }
}
