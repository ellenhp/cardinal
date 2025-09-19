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
