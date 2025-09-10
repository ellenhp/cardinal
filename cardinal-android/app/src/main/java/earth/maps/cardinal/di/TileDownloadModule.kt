package earth.maps.cardinal.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import earth.maps.cardinal.geocoding.TileProcessor
import earth.maps.cardinal.tileserver.PMTilesReader
import earth.maps.cardinal.tileserver.TileDownloadService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TileDownloadModule {
    private const val BASEMAP_TILE_URL =
        "https://cardinaldata.airmail.rs/planet-250825.pmtiles/planet-250825.pmtiles"

    @Provides
    @Singleton
    fun providePMTilesReader(): PMTilesReader {
        return PMTilesReader(BASEMAP_TILE_URL)
    }

    @Provides
    @Singleton
    fun provideTileDownloadService(
        @ApplicationContext context: Context,
        tileProcessor: TileProcessor,
        pmtilesReader: PMTilesReader
    ): TileDownloadService {
        return TileDownloadService(context, tileProcessor, pmtilesReader)
    }
}
