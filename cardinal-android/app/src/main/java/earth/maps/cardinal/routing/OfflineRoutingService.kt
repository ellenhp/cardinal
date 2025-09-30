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
