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
