package earth.maps.cardinal.ui.map

import android.location.Location
import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.dellisd.spatialk.geojson.Point
import io.github.dellisd.spatialk.geojson.Position
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.offset
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.Source
import org.maplibre.compose.sources.rememberGeoJsonSource


@Composable
fun LocationPuckLayers(idPrefix: String, locationSource: Source) {
    CircleLayer(
        id = "${idPrefix}-shadow",
        source = locationSource,
        radius = const(13.dp),
        color = const(Color.Black),
        blur = const(1f),
        translate = offset(0.dp, 1.dp),
    )
    CircleLayer(
        id = "${idPrefix}-circle",
        source = locationSource,
        radius = const(7.dp),
        color = const(MaterialTheme.colorScheme.primary),
        strokeColor = const(Color.White),
        strokeWidth = const(3.dp),
    )
}

@Composable
fun LocationPuck(location: Location) {
    Log.d("Location", "$location")

    val locationSource = rememberGeoJsonSource(
        data = GeoJsonData.Features(
            Point(
                Position(
                    location.longitude,
                    location.latitude
                )
            )
        )
    )
    LocationPuckLayers(idPrefix = "user-location", locationSource = locationSource)
}
