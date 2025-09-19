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

import android.util.Log
import earth.maps.cardinal.data.AppPreferenceRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val TAG = "ValhallaRouting"

class ValhallaRoutingService(private val appPreferenceRepository: AppPreferenceRepository) :
    RoutingService {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging)
    }

    override suspend fun getRoute(
        request: String,
    ): String {
        try {
            val config = appPreferenceRepository.valhallaApiConfig.value
            val url = if (config.apiKey != null) {
                "${config.baseUrl}?api_key=${config.apiKey}"
            } else {
                config.baseUrl
            }

            val response = client.post {
                url(url)
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            return response.body()
        } catch (e: Exception) {
            Log.e(TAG, "Error during routing", e)
            throw e
        }
    }
}
