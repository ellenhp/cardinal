package earth.maps.cardinal.routing

import android.content.Context
import com.stadiamaps.ferrostar.core.AndroidTtsObserver
import dagger.hilt.android.qualifiers.ApplicationContext
import earth.maps.cardinal.data.RoutingMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FerrostarWrapperRepository @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
) {
    lateinit var walking: FerrostarWrapper
    lateinit var cycling: FerrostarWrapper
    lateinit var driving: FerrostarWrapper

    val androidTtsObserver = AndroidTtsObserver(context)

    fun setValhallaEndpoint(endpoint: String) {
        walking = FerrostarWrapper(context, RoutingMode.PEDESTRIAN, endpoint, androidTtsObserver)
        cycling = FerrostarWrapper(context, RoutingMode.BICYCLE, endpoint, androidTtsObserver)
        driving = FerrostarWrapper(context, RoutingMode.AUTO, endpoint, androidTtsObserver)
    }
}
